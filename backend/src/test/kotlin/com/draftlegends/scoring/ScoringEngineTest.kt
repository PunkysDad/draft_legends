package com.draftlegends.scoring

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows

class ScoringEngineTest {

    private val engine = ScoringEngine()

    private fun qbLog(
        passYards: Double? = null,
        passTds: Int? = null,
        interceptions: Int? = null,
        sacks: Int? = null
    ) = GameLog(
        gameLogId = 1, playerId = 1, position = "QB", season = 2024, week = 1,
        passAttempts = null, passCompletions = null, completionPct = null,
        yardsPerAttempt = null, passYards = passYards, passTds = passTds,
        interceptions = interceptions, passerRating = null, sacks = sacks,
        rushAttempts = null, rushYards = null, yardsPerCarry = null, rushLong = null,
        rushTds = null, receptions = null, recYards = null, yardsPerReception = null,
        recLong = null, recTds = null, wrReceptions = null, wrYards = null,
        wrTds = null, yardsPerWrReception = null
    )

    private fun rbLog(
        rushYards: Double? = null,
        rushTds: Int? = null,
        recYards: Double? = null,
        recTds: Int? = null,
        receptions: Int? = null
    ) = GameLog(
        gameLogId = 2, playerId = 2, position = "RB", season = 2024, week = 1,
        passAttempts = null, passCompletions = null, completionPct = null,
        yardsPerAttempt = null, passYards = null, passTds = null,
        interceptions = null, passerRating = null, sacks = null,
        rushAttempts = null, rushYards = rushYards, yardsPerCarry = null, rushLong = null,
        rushTds = rushTds, receptions = receptions, recYards = recYards,
        yardsPerReception = null, recLong = null, recTds = recTds,
        wrReceptions = null, wrYards = null, wrTds = null, yardsPerWrReception = null
    )

    private fun wrLog(
        wrYards: Double? = null,
        wrTds: Int? = null,
        wrReceptions: Int? = null
    ) = GameLog(
        gameLogId = 3, playerId = 3, position = "WR", season = 2024, week = 1,
        passAttempts = null, passCompletions = null, completionPct = null,
        yardsPerAttempt = null, passYards = null, passTds = null,
        interceptions = null, passerRating = null, sacks = null,
        rushAttempts = null, rushYards = null, yardsPerCarry = null, rushLong = null,
        rushTds = null, receptions = null, recYards = null, yardsPerReception = null,
        recLong = null, recTds = null, wrReceptions = wrReceptions, wrYards = wrYards,
        wrTds = wrTds, yardsPerWrReception = null
    )

    private fun assertBreakdownValid(result: ScoringResult) {
        assertTrue(result.breakdown.isNotEmpty())
        result.breakdown.forEach { assertTrue(it.statValue != 0.0) }
    }

    // QB tests

    @Test
    fun `QB normal game scores correctly`() {
        val result = engine.score(qbLog(passYards = 300.0, passTds = 2, interceptions = 1, sacks = 1))
        assertEquals(17.5, result.totalPoints)
        assertBreakdownValid(result)
    }

    @Test
    fun `QB shutout game scores zero with empty breakdown`() {
        val result = engine.score(qbLog(passYards = 0.0, passTds = 0, interceptions = 0, sacks = 0))
        assertEquals(0.0, result.totalPoints)
        assertTrue(result.breakdown.isEmpty())
    }

    @Test
    fun `QB bad game scores negative`() {
        val result = engine.score(qbLog(passYards = 85.0, passTds = 0, interceptions = 3, sacks = 2))
        assertEquals(-3.6, result.totalPoints)
        assertBreakdownValid(result)
    }

    // RB tests

    @Test
    fun `RB normal game scores correctly`() {
        val result = engine.score(rbLog(rushYards = 108.0, rushTds = 1, recYards = 54.0, recTds = 0, receptions = 6))
        assertEquals(25.2, result.totalPoints)
        assertBreakdownValid(result)
    }

    @Test
    fun `RB pure rushing game scores correctly`() {
        val result = engine.score(rbLog(rushYards = 150.0, rushTds = 2, recYards = 0.0, recTds = 0, receptions = 0))
        assertEquals(27.0, result.totalPoints)
        assertBreakdownValid(result)
    }

    // WR tests

    @Test
    fun `WR normal game scores correctly`() {
        val result = engine.score(wrLog(wrYards = 157.0, wrTds = 2, wrReceptions = 10))
        assertEquals(32.7, result.totalPoints)
        assertBreakdownValid(result)
    }

    @Test
    fun `WR quiet game scores correctly`() {
        val result = engine.score(wrLog(wrYards = 22.0, wrTds = 0, wrReceptions = 2))
        assertEquals(3.2, result.totalPoints)
        assertBreakdownValid(result)
    }

    // Unknown position test

    @Test
    fun `unknown position throws IllegalArgumentException`() {
        val log = qbLog().copy(position = "K")
        assertThrows<IllegalArgumentException> { engine.score(log) }
    }
}
