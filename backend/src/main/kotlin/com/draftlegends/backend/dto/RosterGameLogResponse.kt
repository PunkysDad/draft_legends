package com.draftlegends.backend.dto

import java.math.BigDecimal

data class PlayerGameLogResult(
    val player: PlayerDto,
    val gameLog: GameLogDto,
    val scoringResult: ScoringResultDto
)

data class RosterGameLogResponse(
    val results: List<PlayerGameLogResult>,
    val totalFantasyPoints: BigDecimal
)
