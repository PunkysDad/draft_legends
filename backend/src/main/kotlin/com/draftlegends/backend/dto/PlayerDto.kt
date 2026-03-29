package com.draftlegends.backend.dto

import java.math.BigDecimal

data class PlayerDto(
    val playerId: Int,
    val firstName: String,
    val lastName: String,
    val position: String,
    val photoUrl: String?,
    val seasonsPlayed: Int?,
    val totalTouchdowns: Int?,
    val totalInterceptions: Int?,
    val salary: BigDecimal?,
    val volatility: BigDecimal?
)
