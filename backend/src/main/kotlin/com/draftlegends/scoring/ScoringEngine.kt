package com.draftlegends.scoring

import org.springframework.stereotype.Service
import kotlin.math.round

data class GameLog(
    val gameLogId: Int,
    val playerId: Int,
    val position: String,
    val season: Int?,
    val week: Int?,
    // QB fields
    val passAttempts: Int?,
    val passCompletions: Int?,
    val completionPct: Double?,
    val yardsPerAttempt: Double?,
    val passYards: Double?,
    val passTds: Int?,
    val interceptions: Int?,
    val passerRating: Double?,
    val sacks: Int?,
    // RB fields
    val rushAttempts: Int?,
    val rushYards: Double?,
    val yardsPerCarry: Double?,
    val rushLong: Int?,
    val rushTds: Int?,
    val receptions: Int?,
    val recYards: Double?,
    val yardsPerReception: Double?,
    val recLong: Int?,
    val recTds: Int?,
    // WR fields
    val wrReceptions: Int?,
    val wrYards: Double?,
    val wrTds: Int?,
    val yardsPerWrReception: Double?
)

data class ScoringBreakdown(
    val label: String,
    val statValue: Double,
    val points: Double
)

data class ScoringResult(
    val gameLogId: Int,
    val playerId: Int,
    val position: String,
    val totalPoints: Double,
    val breakdown: List<ScoringBreakdown>
)

@Service
class ScoringEngine {

    fun score(gameLog: GameLog): ScoringResult {
        val breakdown = when (gameLog.position) {
            "QB" -> scoreQb(gameLog)
            "RB" -> scoreRb(gameLog)
            "WR" -> scoreWr(gameLog)
            else -> throw IllegalArgumentException("Unsupported position: ${gameLog.position}")
        }

        val totalPoints = roundTo2((breakdown.sumOf { it.points }))

        return ScoringResult(
            gameLogId = gameLog.gameLogId,
            playerId = gameLog.playerId,
            position = gameLog.position,
            totalPoints = totalPoints,
            breakdown = breakdown
        )
    }

    private fun scoreQb(gameLog: GameLog): List<ScoringBreakdown> {
        return listOfNotNull(
            breakdown("Passing Yards", gameLog.passYards ?: 0.0, (gameLog.passYards ?: 0.0) / 25.0),
            breakdown("Passing TDs", (gameLog.passTds ?: 0).toDouble(), (gameLog.passTds ?: 0) * 4.0),
            breakdown("Interceptions", (gameLog.interceptions ?: 0).toDouble(), (gameLog.interceptions ?: 0) * -2.0),
            breakdown("Sacks", (gameLog.sacks ?: 0).toDouble(), (gameLog.sacks ?: 0) * -0.5)
        )
    }

    private fun scoreRb(gameLog: GameLog): List<ScoringBreakdown> {
        return listOfNotNull(
            breakdown("Rushing Yards", gameLog.rushYards ?: 0.0, (gameLog.rushYards ?: 0.0) / 10.0),
            breakdown("Rushing TDs", (gameLog.rushTds ?: 0).toDouble(), (gameLog.rushTds ?: 0) * 6.0),
            breakdown("Receiving Yards", gameLog.recYards ?: 0.0, (gameLog.recYards ?: 0.0) / 10.0),
            breakdown("Receiving TDs", (gameLog.recTds ?: 0).toDouble(), (gameLog.recTds ?: 0) * 6.0),
            breakdown("Receptions", (gameLog.receptions ?: 0).toDouble(), (gameLog.receptions ?: 0) * 0.5)
        )
    }

    private fun scoreWr(gameLog: GameLog): List<ScoringBreakdown> {
        return listOfNotNull(
            breakdown("Receiving Yards", gameLog.wrYards ?: 0.0, (gameLog.wrYards ?: 0.0) / 10.0),
            breakdown("Receiving TDs", (gameLog.wrTds ?: 0).toDouble(), (gameLog.wrTds ?: 0) * 6.0),
            breakdown("Receptions", (gameLog.wrReceptions ?: 0).toDouble(), (gameLog.wrReceptions ?: 0) * 0.5)
        )
    }

    private fun breakdown(label: String, statValue: Double, points: Double): ScoringBreakdown? {
        if (statValue == 0.0) return null
        return ScoringBreakdown(label, statValue, points)
    }

    private fun roundTo2(value: Double): Double {
        return round(value * 100.0) / 100.0
    }
}
