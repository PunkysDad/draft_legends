package com.draftlegends.backend.dto

data class ScoringBreakdownDto(
    val label: String,
    val statValue: Double,
    val points: Double
)

data class ScoringResultDto(
    val gameLogId: Int,
    val playerId: Int,
    val position: String,
    val totalPoints: Double,
    val breakdown: List<ScoringBreakdownDto>
)
