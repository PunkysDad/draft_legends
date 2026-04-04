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
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.temporal.TemporalAdjusters

@Component
@Profile("migration & !mlb-migration")
class NewPlayerMigration(
    private val jdbcTemplate: JdbcTemplate
) : ApplicationRunner {

    companion object {
        private const val PLAYERS_CSV = "../data/new_players/players.csv"
        private const val GAME_LOGS_CSV = "../data/new_players/game_logs.csv"
    }

    override fun run(args: ApplicationArguments) {
        println("=== Starting New Player Migration ===")

        val playerStats = migratePlayers()
        val gameLogStats = migrateGameLogs()

        println()
        println("=== Migration Summary ===")
        println("  Players inserted: ${playerStats.inserted}")
        println("  Players updated:  ${playerStats.updated}")
        println("  Game logs inserted: ${gameLogStats.inserted}")
        println("  Game logs skipped:  ${gameLogStats.skipped}")
        println("=== Migration complete ===")
    }

    private fun migratePlayers(): MigrationStats {
        val stats = MigrationStats()
        val rows = readCsv(PLAYERS_CSV)

        for (row in rows) {
            val firstName = row["first_name"]!!
            val lastName = row["last_name"]!!
            val position = row["position"]!!
            val salary = BigDecimal(row["salary"]!!)
            val volatility = BigDecimal(row["volatility"]!!)
            val seasonsPlayed = row["seasons_played"]!!.toInt()
            val totalTouchdowns = row["total_touchdowns"]!!.toInt()

            val existingId = jdbcTemplate.queryForList(
                "SELECT player_id FROM players WHERE first_name = ? AND last_name = ?",
                Int::class.java,
                firstName, lastName
            )

            if (existingId.isNotEmpty()) {
                jdbcTemplate.update(
                    """
                    UPDATE players
                    SET salary = ?, volatility = ?, seasons_played = ?, total_touchdowns = ?
                    WHERE first_name = ? AND last_name = ?
                    """.trimIndent(),
                    salary, volatility, seasonsPlayed, totalTouchdowns,
                    firstName, lastName
                )
                stats.updated++
                println("  Updated player: $firstName $lastName (ID=${existingId[0]})")
            } else {
                jdbcTemplate.update(
                    """
                    INSERT INTO players (first_name, last_name, position, photo_url,
                                         seasons_played, total_touchdowns, total_interceptions,
                                         salary, volatility)
                    VALUES (?, ?, ?, ?, ?, ?, NULL, ?, ?)
                    """.trimIndent(),
                    firstName, lastName, position, "",
                    seasonsPlayed, totalTouchdowns,
                    salary, volatility
                )
                stats.inserted++
                println("  Inserted player: $firstName $lastName ($position)")
            }
        }

        return stats
    }

    private fun migrateGameLogs(): MigrationStats {
        val stats = MigrationStats()
        val rows = readCsv(GAME_LOGS_CSV)

        // Cache player_id lookups
        val playerIdCache = mutableMapOf<String, Int>()

        for (row in rows) {
            val firstName = row["first_name"]!!
            val lastName = row["last_name"]!!
            val position = row["position"]!!
            val season = row["season"]!!.toInt()
            val week = row["week"]!!.toInt()
            val fantasyPoints = BigDecimal(row["fantasy_points"]!!)

            val cacheKey = "$firstName $lastName"
            val playerId = playerIdCache.getOrPut(cacheKey) {
                jdbcTemplate.queryForObject(
                    "SELECT player_id FROM players WHERE first_name = ? AND last_name = ?",
                    Int::class.java,
                    firstName, lastName
                )!!
            }

            val sourceDocId = "${playerId}_nfl_${season}_${week}"
            val gameDate = nflWeekToDate(season, week)

            when (position) {
                "RB" -> {
                    val rushAttempts = row["rush_attempts"]!!.toInt()
                    val rushYards = BigDecimal(row["rush_yards"]!!)
                    val rushTds = row["rush_tds"]!!.toInt()
                    val receptions = row["receptions"]!!.toInt()
                    val recYards = BigDecimal(row["rec_yards"]!!)
                    val recTds = row["rec_tds"]!!.toInt()

                    val affected = jdbcTemplate.update(
                        """
                        INSERT INTO game_logs (player_id, source_doc_id, season, week, game_date, position,
                                               rush_attempts, rush_yards, rush_tds,
                                               receptions, rec_yards, rec_tds,
                                               fantasy_points)
                        VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                        ON CONFLICT ON CONSTRAINT uq_game_log DO NOTHING
                        """.trimIndent(),
                        playerId, sourceDocId, season, week, Date.valueOf(gameDate), position,
                        rushAttempts, rushYards, rushTds,
                        receptions, recYards, recTds,
                        fantasyPoints
                    )

                    if (affected > 0) stats.inserted++ else stats.skipped++
                }
                "WR" -> {
                    val wrReceptions = row["receptions"]!!.toInt()
                    val wrYards = BigDecimal(row["rec_yards"]!!)
                    val wrTds = row["rec_tds"]!!.toInt()

                    val affected = jdbcTemplate.update(
                        """
                        INSERT INTO game_logs (player_id, source_doc_id, season, week, game_date, position,
                                               wr_receptions, wr_yards, wr_tds,
                                               fantasy_points)
                        VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                        ON CONFLICT ON CONSTRAINT uq_game_log DO NOTHING
                        """.trimIndent(),
                        playerId, sourceDocId, season, week, Date.valueOf(gameDate), position,
                        wrReceptions, wrYards, wrTds,
                        fantasyPoints
                    )

                    if (affected > 0) stats.inserted++ else stats.skipped++
                }
            }
        }

        return stats
    }

    /**
     * Approximate the Monday of a given NFL season week.
     * NFL Week 1 typically starts the first full week of September.
     * We use the first Monday of September + (week - 1) * 7 days.
     */
    private fun nflWeekToDate(season: Int, week: Int): LocalDate {
        val septFirst = LocalDate.of(season, 9, 1)
        val firstMonday = septFirst.with(TemporalAdjusters.firstInMonth(DayOfWeek.MONDAY))
        return firstMonday.plusWeeks((week - 1).toLong())
    }

    private fun readCsv(path: String): List<Map<String, String>> {
        val rows = mutableListOf<Map<String, String>>()
        BufferedReader(FileReader(path)).use { reader ->
            val headers = reader.readLine()?.split(",") ?: return rows
            reader.forEachLine { line ->
                val values = line.split(",")
                val row = headers.zip(values).toMap()
                rows.add(row)
            }
        }
        return rows
    }

    private data class MigrationStats(var inserted: Int = 0, var updated: Int = 0, var skipped: Int = 0)
}
