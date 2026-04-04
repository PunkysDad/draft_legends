package com.draftlegends.migration

import org.springframework.boot.ApplicationArguments
import org.springframework.boot.ApplicationRunner
import org.springframework.context.annotation.Profile
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.stereotype.Component
import java.io.BufferedReader
import java.io.FileReader
import java.math.BigDecimal
import java.sql.Date
import java.time.LocalDate

@Component
@Profile("mlb-migration")
class MlbPlayerMigration(
    private val jdbcTemplate: JdbcTemplate
) : ApplicationRunner {

    companion object {
        private const val PLAYERS_CSV = "../data/mlb_players/mlb_players.csv"
        private const val GAME_LOGS_CSV = "../data/mlb_players/mlb_game_logs.csv"
    }

    override fun run(args: ApplicationArguments) {
        println("=== Starting MLB Player Migration ===")

        val playerIdMap = mutableMapOf<String, Int>()
        val playerStats = migratePlayers(playerIdMap)
        val gameLogStats = migrateGameLogs(playerIdMap)

        println()
        println("=== Migration Summary ===")
        println("  Players inserted: ${playerStats.inserted}")
        println("  Players updated:  ${playerStats.updated}")
        println("  Game logs inserted: ${gameLogStats.inserted}")
        println("  Game logs skipped:  ${gameLogStats.skipped}")
        println("=== MLB Migration complete ===")
    }

    private fun migratePlayers(playerIdMap: MutableMap<String, Int>): MigrationStats {
        val stats = MigrationStats()
        val rows = readCsv(PLAYERS_CSV)

        for (row in rows) {
            val firstName = row["first_name"]!!
            val lastName = row["last_name"]!!
            val playerType = row["player_type"]!!
            val avgFantasyPoints = BigDecimal(row["avg_fantasy_points"]!!)
            val volatility = BigDecimal(row["volatility"]!!)
            val salary = BigDecimal(row["salary"]!!)
            val seasonsPlayed = row["seasons_played"]!!.toInt()
            val totalHomeRuns = row["total_home_runs"]!!.toInt()
            val totalStrikeouts = row["total_strikeouts"]!!.toInt()
            val keyMlbam = row["key_mlbam"]!!.toInt()

            val mapKey = "$firstName $lastName"

            val existingIds = jdbcTemplate.queryForList(
                "SELECT player_id FROM mlb_players WHERE first_name = ? AND last_name = ?",
                Int::class.java,
                firstName, lastName
            )

            if (existingIds.isNotEmpty()) {
                val playerId = existingIds[0]
                jdbcTemplate.update(
                    """
                    UPDATE mlb_players
                    SET salary = ?, volatility = ?, avg_fantasy_points = ?,
                        seasons_played = ?, total_home_runs = ?, total_strikeouts = ?
                    WHERE player_id = ?
                    """.trimIndent(),
                    salary, volatility, avgFantasyPoints,
                    seasonsPlayed, totalHomeRuns, totalStrikeouts,
                    playerId
                )
                playerIdMap[mapKey] = playerId
                stats.updated++
                println("  Updated player: $firstName $lastName (ID=$playerId)")
            } else {
                jdbcTemplate.update(
                    """
                    INSERT INTO mlb_players (first_name, last_name, player_type,
                                             avg_fantasy_points, volatility, salary,
                                             seasons_played, total_home_runs, total_strikeouts,
                                             key_mlbam)
                    VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                    """.trimIndent(),
                    firstName, lastName, playerType,
                    avgFantasyPoints, volatility, salary,
                    seasonsPlayed, totalHomeRuns, totalStrikeouts,
                    keyMlbam
                )

                val playerId = jdbcTemplate.queryForObject(
                    "SELECT player_id FROM mlb_players WHERE first_name = ? AND last_name = ?",
                    Int::class.java,
                    firstName, lastName
                )!!
                playerIdMap[mapKey] = playerId
                stats.inserted++
                println("  Inserted player: $firstName $lastName ($playerType, ID=$playerId)")
            }
        }

        return stats
    }

    private fun migrateGameLogs(playerIdMap: Map<String, Int>): MigrationStats {
        val stats = MigrationStats()
        val rows = readCsv(GAME_LOGS_CSV)

        for (row in rows) {
            val firstName = row["first_name"]!!
            val lastName = row["last_name"]!!
            val playerType = row["player_type"]!!
            val season = row["season"]!!.toInt()
            val gameDateStr = row["game_date"]!!
            val gameDate = LocalDate.parse(gameDateStr)
            val opponent = row["opponent"]!!
            val isHome = row["is_home"]!! == "True"
            val fantasyPoints = BigDecimal(row["fantasy_points"]!!)

            val mapKey = "$firstName $lastName"
            val playerId = playerIdMap[mapKey]
            if (playerId == null) {
                stats.skipped++
                continue
            }

            val sourceDocId = "${playerId}_mlb_${season}_${gameDateStr}"

            val affected = if (playerType == "HITTER") {
                jdbcTemplate.update(
                    """
                    INSERT INTO mlb_game_logs (player_id, source_doc_id, season, game_date,
                                               opponent, is_home,
                                               at_bats, hits, singles, doubles, triples,
                                               home_runs, rbi, runs, stolen_bases, walks,
                                               strikeouts_batting, fantasy_points)
                    VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                    ON CONFLICT ON CONSTRAINT uq_mlb_game_log DO NOTHING
                    """.trimIndent(),
                    playerId, sourceDocId, season, Date.valueOf(gameDate),
                    opponent, isHome,
                    row["at_bats"]!!.toInt(),
                    row["hits"]!!.toInt(),
                    row["singles"]!!.toInt(),
                    row["doubles"]!!.toInt(),
                    row["triples"]!!.toInt(),
                    row["home_runs"]!!.toInt(),
                    row["rbi"]!!.toInt(),
                    row["runs"]!!.toInt(),
                    row["stolen_bases"]!!.toInt(),
                    row["walks"]!!.toInt(),
                    row["strikeouts_batting"]!!.toInt(),
                    fantasyPoints
                )
            } else {
                jdbcTemplate.update(
                    """
                    INSERT INTO mlb_game_logs (player_id, source_doc_id, season, game_date,
                                               opponent, is_home,
                                               innings_pitched, pitcher_strikeouts, walks_allowed,
                                               earned_runs, hits_allowed, wins, losses, saves,
                                               fantasy_points)
                    VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                    ON CONFLICT ON CONSTRAINT uq_mlb_game_log DO NOTHING
                    """.trimIndent(),
                    playerId, sourceDocId, season, Date.valueOf(gameDate),
                    opponent, isHome,
                    BigDecimal(row["innings_pitched"]!!),
                    row["pitcher_strikeouts"]!!.toInt(),
                    row["walks_allowed"]!!.toInt(),
                    row["earned_runs"]!!.toInt(),
                    row["hits_allowed"]!!.toInt(),
                    row["wins"]!!.toInt(),
                    row["losses"]!!.toInt(),
                    row["saves"]!!.toInt(),
                    fantasyPoints
                )
            }

            if (affected > 0) stats.inserted++ else stats.skipped++
        }

        return stats
    }

    private fun readCsv(path: String): List<Map<String, String>> {
        val rows = mutableListOf<Map<String, String>>()
        BufferedReader(FileReader(path)).use { reader ->
            val headers = reader.readLine()?.split(",") ?: return rows
            reader.forEachLine { line ->
                val values = parseCsvLine(line)
                if (values.size == headers.size) {
                    rows.add(headers.zip(values).toMap())
                }
            }
        }
        return rows
    }

    /**
     * Parse a CSV line handling quoted fields (for opponent names with commas).
     */
    private fun parseCsvLine(line: String): List<String> {
        val fields = mutableListOf<String>()
        var current = StringBuilder()
        var inQuotes = false

        for (ch in line) {
            when {
                ch == '"' -> inQuotes = !inQuotes
                ch == ',' && !inQuotes -> {
                    fields.add(current.toString())
                    current = StringBuilder()
                }
                else -> current.append(ch)
            }
        }
        fields.add(current.toString())

        return fields
    }

    private data class MigrationStats(var inserted: Int = 0, var updated: Int = 0, var skipped: Int = 0)
}
