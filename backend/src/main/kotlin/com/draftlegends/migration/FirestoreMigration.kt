package com.draftlegends.migration

import com.google.auth.oauth2.GoogleCredentials
import com.google.cloud.firestore.Firestore
import com.google.cloud.firestore.QueryDocumentSnapshot
import com.google.firebase.FirebaseApp
import com.google.firebase.FirebaseOptions
import com.google.firebase.cloud.FirestoreClient
import org.springframework.boot.CommandLineRunner
import org.springframework.context.annotation.Profile
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.stereotype.Component
import java.io.FileInputStream
import java.math.BigDecimal
import java.math.RoundingMode
import java.sql.Date
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import kotlin.math.sqrt

@SpringBootApplication(scanBasePackages = ["com.draftlegends.migration"])
class FirestoreMigrationApplication

fun main(args: Array<String>) {
    runApplication<FirestoreMigrationApplication>(*args) {
        setAdditionalProfiles("migration")
    }
}

@Component
@Profile("firestore-migration")
class FirestoreMigration(
    private val jdbcTemplate: JdbcTemplate
) : CommandLineRunner {

    override fun run(vararg args: String) {
        println("=== Starting Firestore to PostgreSQL migration ===")

        val firestore = initializeFirestore()

        val players = firestore.collection("legends").get().get().documents
        println("Found ${players.size} players in Firestore")

        for (playerDoc in players) {
            migratePlayer(playerDoc)
            migratePlayerSeasons(playerDoc)
            migrateGameLogs(firestore, playerDoc)
        }

        println("=== Computing salary and volatility for all players ===")
        computeSalaryAndVolatility()

        println("=== Migration complete ===")
    }

    private fun initializeFirestore(): Firestore {
        val credentialsPath = System.getenv("FIREBASE_CREDENTIALS_PATH")
            ?: throw IllegalStateException("FIREBASE_CREDENTIALS_PATH environment variable is not set")

        if (FirebaseApp.getApps().isEmpty()) {
            val options = FirebaseOptions.builder()
                .setCredentials(GoogleCredentials.fromStream(FileInputStream(credentialsPath)))
                .build()
            FirebaseApp.initializeApp(options)
        }

        return FirestoreClient.getFirestore()
    }

    private fun migratePlayer(doc: QueryDocumentSnapshot) {
        val playerId = doc.getLong("player_id")?.toInt()
            ?: throw IllegalStateException("Player document ${doc.id} missing player_id")
        val firstName = doc.getString("first_name") ?: ""
        val lastName = doc.getString("last_name") ?: ""
        val position = doc.getString("position") ?: ""
        val photoUrl = doc.getString("photo")
        val seasonsPlayed = doc.getLong("seasonsPlayed")?.toInt()
        val totalTouchdowns = when (position) {
            "WR", "RB" -> doc.getLong("totalTds")?.toInt()
            else -> doc.getLong("totalTouchdowns")?.toInt()
        }
        val totalInterceptions = if (position == "QB") doc.getLong("totalInterceptions")?.toInt() else null

        jdbcTemplate.update(
            """
            INSERT INTO players (player_id, first_name, last_name, position, photo_url,
                                 seasons_played, total_touchdowns, total_interceptions, salary, volatility)
            VALUES (?, ?, ?, ?, ?, ?, ?, ?, 0.0, 0.0)
            ON CONFLICT (player_id) DO UPDATE SET
                first_name = EXCLUDED.first_name,
                last_name = EXCLUDED.last_name,
                position = EXCLUDED.position,
                photo_url = EXCLUDED.photo_url,
                seasons_played = EXCLUDED.seasons_played,
                total_touchdowns = EXCLUDED.total_touchdowns,
                total_interceptions = EXCLUDED.total_interceptions
            """.trimIndent(),
            playerId, firstName, lastName, position, photoUrl,
            seasonsPlayed, totalTouchdowns, totalInterceptions
        )

        println("  Migrated player: $firstName $lastName (ID=$playerId, $position)")
    }

    @Suppress("UNCHECKED_CAST")
    private fun migratePlayerSeasons(doc: QueryDocumentSnapshot) {
        val playerId = doc.getLong("player_id")?.toInt() ?: return
        val seasons = doc.get("seasons") as? List<Map<String, Any>> ?: return

        for (entry in seasons) {
            val season = (entry["season"] as? Number)?.toInt() ?: continue
            val gamesPlayed = (entry["games"] as? Number)?.toInt()

            jdbcTemplate.update(
                """
                INSERT INTO player_seasons (player_id, season, games_played)
                VALUES (?, ?, ?)
                ON CONFLICT ON CONSTRAINT uq_player_season DO UPDATE SET
                    games_played = EXCLUDED.games_played
                """.trimIndent(),
                playerId, season, gamesPlayed
            )
        }
    }

    private fun migrateGameLogs(firestore: Firestore, playerDoc: QueryDocumentSnapshot) {
        val playerId = playerDoc.getLong("player_id")?.toInt() ?: return
        val position = playerDoc.getString("position") ?: return

        val playerSeasonDoc = firestore.collection("legendseason")
            .document(playerId.toString())
        val seasonCollections = playerSeasonDoc.listCollections()

        var totalGameLogs = 0

        for (seasonCollection in seasonCollections) {
            val season = seasonCollection.id.toIntOrNull()
            val gameDocs = seasonCollection.get().get().documents

            for (gameDoc in gameDocs) {
                when (position) {
                    "QB" -> migrateQbGameLog(gameDoc, playerId, position, season)
                    "RB" -> migrateRbGameLog(gameDoc, playerId, position, season)
                    "WR" -> migrateWrGameLog(gameDoc, playerId, position, season)
                    else -> println("      Unknown position $position for player $playerId, skipping game log")
                }
                totalGameLogs++
            }
        }

        if (totalGameLogs == 0) {
            println("    No game logs found for player $playerId")
        } else {
            println("    Migrated $totalGameLogs game logs for player $playerId ($position)")
        }
    }

    private fun migrateQbGameLog(doc: QueryDocumentSnapshot, playerId: Int, position: String, season: Int?) {
        val week = doc.getLong("week")?.toInt()
        val gameDateStr = doc.getString("game_date")
        val gameDate = parseQbDate(gameDateStr, season)

        val passAttempts = doc.getLong("att")?.toInt()
        val passCompletions = doc.getLong("comp")?.toInt()
        val completionPct = doc.getDouble("pct")?.toBigDecimal()
        val yardsPerAttempt = doc.getDouble("average")?.toBigDecimal()
        val passYards = doc.getDouble("yds")?.toBigDecimal()
            ?: doc.getLong("yds")?.toBigDecimal()
        val passTds = doc.getLong("tds")?.toInt()
        val interceptions = doc.getLong("ints")?.toInt()
        val passerRating = doc.getDouble("rating")?.toBigDecimal()
        val sacks = doc.getLong("sacks")?.toInt()

        val fantasyPoints = computeQbFantasy(passYards, passTds, interceptions, sacks)

        jdbcTemplate.update(
            """
            INSERT INTO game_logs (player_id, source_doc_id, season, week, game_date, position,
                                   pass_attempts, pass_completions, completion_pct, yards_per_attempt,
                                   pass_yards, pass_tds, interceptions, passer_rating, sacks, fantasy_points)
            VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
            ON CONFLICT ON CONSTRAINT uq_game_log DO UPDATE SET
                pass_attempts = EXCLUDED.pass_attempts,
                pass_completions = EXCLUDED.pass_completions,
                completion_pct = EXCLUDED.completion_pct,
                yards_per_attempt = EXCLUDED.yards_per_attempt,
                pass_yards = EXCLUDED.pass_yards,
                pass_tds = EXCLUDED.pass_tds,
                interceptions = EXCLUDED.interceptions,
                passer_rating = EXCLUDED.passer_rating,
                sacks = EXCLUDED.sacks,
                fantasy_points = EXCLUDED.fantasy_points
            """.trimIndent(),
            playerId, "${playerId}_${season}_${doc.id}", season, week, gameDate, position,
            passAttempts, passCompletions, completionPct, yardsPerAttempt,
            passYards, passTds, interceptions, passerRating, sacks, fantasyPoints
        )
    }

    private fun migrateRbGameLog(doc: QueryDocumentSnapshot, playerId: Int, position: String, season: Int?) {
        val week = doc.getLong("week")?.toInt()
        val gameDateStr = doc.getString("game_date")
        val gameDate = parseRbDate(gameDateStr, season)

        val rushAttempts = doc.getLong("attempts")?.toInt()
        val rushYards = doc.getLong("rushing_yards")?.toBigDecimal()
        val yardsPerCarry = doc.getDouble("rushing_average")?.toBigDecimal()
        val rushLong = doc.getLong("rushing_long")?.toInt()
        val rushTds = doc.getLong("rushing_touchdowns")?.toInt()
        val receptions = doc.getLong("receptions")?.toInt()
        val recYards = doc.getLong("rec_yards")?.toBigDecimal()
        val yardsPerReception = doc.getDouble("rec_average")?.toBigDecimal()
        val recLong = doc.getLong("rec_long")?.toInt()
        val recTds = doc.getLong("rec_touchdowns")?.toInt()

        val fantasyPoints = computeRbFantasy(rushYards, rushTds, recYards, recTds, receptions)

        jdbcTemplate.update(
            """
            INSERT INTO game_logs (player_id, source_doc_id, season, week, game_date, position,
                                   rush_attempts, rush_yards, yards_per_carry, rush_long, rush_tds,
                                   receptions, rec_yards, yards_per_reception, rec_long, rec_tds,
                                   fantasy_points)
            VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
            ON CONFLICT ON CONSTRAINT uq_game_log DO UPDATE SET
                rush_attempts = EXCLUDED.rush_attempts,
                rush_yards = EXCLUDED.rush_yards,
                yards_per_carry = EXCLUDED.yards_per_carry,
                rush_long = EXCLUDED.rush_long,
                rush_tds = EXCLUDED.rush_tds,
                receptions = EXCLUDED.receptions,
                rec_yards = EXCLUDED.rec_yards,
                yards_per_reception = EXCLUDED.yards_per_reception,
                rec_long = EXCLUDED.rec_long,
                rec_tds = EXCLUDED.rec_tds,
                fantasy_points = EXCLUDED.fantasy_points
            """.trimIndent(),
            playerId, "${playerId}_${season}_${doc.id}", season, week, gameDate, position,
            rushAttempts, rushYards, yardsPerCarry, rushLong, rushTds,
            receptions, recYards, yardsPerReception, recLong, recTds,
            fantasyPoints
        )
    }

    @Suppress("UNCHECKED_CAST")
    private fun migrateWrGameLog(doc: QueryDocumentSnapshot, playerId: Int, position: String, season: Int?) {
        val game = doc.get("game") as? Map<String, Any> ?: return

        val dateStr = game["date"] as? String
        val gameDate = if (dateStr != null) Date.valueOf(LocalDate.parse(dateStr, DateTimeFormatter.ISO_LOCAL_DATE)) else null
        val resolvedSeason = season ?: if (dateStr != null) LocalDate.parse(dateStr, DateTimeFormatter.ISO_LOCAL_DATE).year else null
        val week = (game["week"] as? Number)?.toInt()

        val wrReceptions = (game["receptions"] as? Number)?.toInt()
        val wrYards = (game["weekYards"] as? Number)?.toDouble()?.toBigDecimal()
        val wrTds = (game["weekTds"] as? Number)?.toInt()
        val yardsPerWrReception = (game["weekAvg"] as? Number)?.toDouble()?.toBigDecimal()

        val fantasyPoints = computeWrFantasy(wrYards, wrTds, wrReceptions)

        jdbcTemplate.update(
            """
            INSERT INTO game_logs (player_id, source_doc_id, season, week, game_date, position,
                                   wr_receptions, wr_yards, wr_tds, yards_per_wr_reception,
                                   fantasy_points)
            VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
            ON CONFLICT ON CONSTRAINT uq_game_log DO UPDATE SET
                wr_receptions = EXCLUDED.wr_receptions,
                wr_yards = EXCLUDED.wr_yards,
                wr_tds = EXCLUDED.wr_tds,
                yards_per_wr_reception = EXCLUDED.yards_per_wr_reception,
                fantasy_points = EXCLUDED.fantasy_points
            """.trimIndent(),
            playerId, "${playerId}_${season}_${doc.id}", resolvedSeason, week, gameDate, position,
            wrReceptions, wrYards, wrTds, yardsPerWrReception,
            fantasyPoints
        )
    }

    private fun parseQbDate(dateStr: String?, season: Int?): Date? {
        if (dateStr == null || season == null) return null
        return try {
            val parts = dateStr.split("/")
            if (parts.size == 2) {
                val month = parts[0].toInt()
                val day = parts[1].toInt()
                Date.valueOf(LocalDate.of(season, month, day))
            } else null
        } catch (e: Exception) {
            null
        }
    }

    private fun parseRbDate(dateStr: String?, season: Int?): Date? {
        if (dateStr == null || season == null) return null
        return try {
            val parts = dateStr.split("/")
            if (parts.size == 2) {
                val month = parts[0].toInt()
                val day = parts[1].toInt()
                Date.valueOf(LocalDate.of(season, month, day))
            } else null
        } catch (e: Exception) {
            null
        }
    }

    private fun computeQbFantasy(
        passYards: BigDecimal?, passTds: Int?, interceptions: Int?, sacks: Int?
    ): BigDecimal {
        val yards = passYards?.toDouble() ?: 0.0
        val tds = passTds ?: 0
        val ints = interceptions ?: 0
        val sk = sacks ?: 0
        val points = (yards / 25.0) + (tds * 4) + (ints * -2) + (sk * -0.5)
        return BigDecimal(points).setScale(2, RoundingMode.HALF_UP)
    }

    private fun computeRbFantasy(
        rushYards: BigDecimal?, rushTds: Int?, recYards: BigDecimal?, recTds: Int?, receptions: Int?
    ): BigDecimal {
        val ry = rushYards?.toDouble() ?: 0.0
        val rt = rushTds ?: 0
        val recy = recYards?.toDouble() ?: 0.0
        val rect = recTds ?: 0
        val rec = receptions ?: 0
        val points = (ry / 10.0) + (rt * 6) + (recy / 10.0) + (rect * 6) + (rec * 0.5)
        return BigDecimal(points).setScale(2, RoundingMode.HALF_UP)
    }

    private fun computeWrFantasy(
        wrYards: BigDecimal?, wrTds: Int?, wrReceptions: Int?
    ): BigDecimal {
        val yards = wrYards?.toDouble() ?: 0.0
        val tds = wrTds ?: 0
        val rec = wrReceptions ?: 0
        val points = (yards / 10.0) + (tds * 6) + (rec * 0.5)
        return BigDecimal(points).setScale(2, RoundingMode.HALF_UP)
    }

    private fun computeSalaryAndVolatility() {
        val playerIds = jdbcTemplate.queryForList(
            "SELECT DISTINCT player_id FROM game_logs", Int::class.java
        )

        for (playerId in playerIds) {
            val fantasyPoints = jdbcTemplate.queryForList(
                "SELECT fantasy_points FROM game_logs WHERE player_id = ?",
                BigDecimal::class.java,
                playerId
            )

            if (fantasyPoints.isEmpty()) continue

            val values = fantasyPoints.map { it.toDouble() }
            val mean = values.average()
            val variance = values.map { (it - mean) * (it - mean) }.average()
            val stdDev = sqrt(variance)

            val salary = BigDecimal(mean).setScale(2, RoundingMode.HALF_UP)
            val volatility = BigDecimal(stdDev).setScale(2, RoundingMode.HALF_UP)

            jdbcTemplate.update(
                "UPDATE players SET salary = ?, volatility = ? WHERE player_id = ?",
                salary, volatility, playerId
            )

            println("  Player $playerId: salary=$salary, volatility=$volatility")
        }
    }
}
