package com.draftlegends.backend.dto

import java.math.BigDecimal
import java.time.LocalDateTime

data class CreateMatchupRequest(
    val mode: String,
    val vsMode: String // "CPU" or "HUMAN"
)

data class PickRequest(
    val playerId: Int
)

data class DraftPickDto(
    val pickId: Int,
    val userId: Int,
    val playerId: Int,
    val playerFirstName: String,
    val playerLastName: String,
    val position: String,
    val pickNumber: Int,
    val isCpuPick: Boolean,
    val pickedAt: LocalDateTime
)

data class GameLogPullDto(
    val playerId: Int,
    val playerFirstName: String,
    val playerLastName: String,
    val position: String,
    val gameLogId: Int,
    val season: Int?,
    val week: Int?,
    val fantasyPoints: BigDecimal,
    val breakdown: List<ScoringBreakdownDto>
)

data class RosterDto(
    val userId: Int,
    val displayName: String?,
    val picks: List<DraftPickDto>,
    val gameLogPulls: List<GameLogPullDto>,
    val totalScore: BigDecimal?
)

data class MatchupResponse(
    val matchupId: Int,
    val mode: String,
    val status: String,
    val isVsCpu: Boolean,
    val currentTurnUserId: Int?,
    val pickDeadline: LocalDateTime?,
    val rosters: List<RosterDto>,
    val winnerUserId: Int?,
    val createdAt: LocalDateTime,
    val completedAt: LocalDateTime?
)
