package com.draftlegends.backend.dto

import java.math.BigDecimal
import java.time.LocalDate

data class GameLogDto(
    val gameLogId: Int,
    val playerId: Int,
    val season: Int?,
    val week: Int?,
    val gameDate: LocalDate?,
    val position: String,
    // QB
    val passAttempts: Int?,
    val passCompletions: Int?,
    val completionPct: BigDecimal?,
    val yardsPerAttempt: BigDecimal?,
    val passYards: BigDecimal?,
    val passTds: Int?,
    val interceptions: Int?,
    val passerRating: BigDecimal?,
    val sacks: Int?,
    // RB
    val rushAttempts: Int?,
    val rushYards: BigDecimal?,
    val yardsPerCarry: BigDecimal?,
    val rushLong: Int?,
    val rushTds: Int?,
    val receptions: Int?,
    val recYards: BigDecimal?,
    val yardsPerReception: BigDecimal?,
    val recLong: Int?,
    val recTds: Int?,
    // WR
    val wrReceptions: Int?,
    val wrYards: BigDecimal?,
    val wrTds: Int?,
    val yardsPerWrReception: BigDecimal?,
    val fantasyPoints: BigDecimal?
)
