package com.draftlegends.backend.service

import com.draftlegends.backend.dto.*
import com.draftlegends.backend.mapper.toDto
import com.draftlegends.backend.repository.GameLogRepository
import com.draftlegends.backend.repository.PlayerRepository
import com.draftlegends.scoring.GameLog
import com.draftlegends.scoring.ScoringEngine
import org.springframework.stereotype.Service
import java.math.BigDecimal

@Service
class PlayerService(
    private val playerRepository: PlayerRepository,
    private val gameLogRepository: GameLogRepository,
    private val scoringEngine: ScoringEngine
) {
    fun getAllPlayers(position: String?, sortBy: String?, sortDir: String?): List<PlayerDto> {
        val players = when {
            position != null && sortBy == "salary" -> playerRepository.findByPositionOrderBySalaryDesc(position)
            position != null -> playerRepository.findByPosition(position)
            sortBy == "salary" -> playerRepository.findAllByOrderBySalaryDesc()
            sortBy == "volatility" -> playerRepository.findAllByOrderByVolatilityDesc()
            else -> playerRepository.findAll()
        }
        return if (sortDir == "asc") players.reversed().map { it.toDto() }
        else players.map { it.toDto() }
    }

    fun getPlayerById(playerId: Int): PlayerDto? =
        playerRepository.findById(playerId).orElse(null)?.toDto()

    fun getPlayersByPosition(position: String): List<PlayerDto> =
        playerRepository.findByPosition(position).map { it.toDto() }

    fun getRosterGameLogs(request: RosterGameLogRequest): RosterGameLogResponse {
        val results = request.playerIds.mapNotNull { playerId ->
            val player = playerRepository.findById(playerId).orElse(null) ?: return@mapNotNull null
            val gameLog = gameLogRepository.findRandomByPlayerId(playerId) ?: return@mapNotNull null

            val scoringInput = GameLog(
                gameLogId = gameLog.gameLogId,
                playerId = gameLog.playerId,
                position = gameLog.position,
                season = gameLog.season,
                week = gameLog.week,
                passAttempts = gameLog.passAttempts,
                passCompletions = gameLog.passCompletions,
                completionPct = gameLog.completionPct?.toDouble(),
                yardsPerAttempt = gameLog.yardsPerAttempt?.toDouble(),
                passYards = gameLog.passYards?.toDouble(),
                passTds = gameLog.passTds,
                interceptions = gameLog.interceptions,
                passerRating = gameLog.passerRating?.toDouble(),
                sacks = gameLog.sacks,
                rushAttempts = gameLog.rushAttempts,
                rushYards = gameLog.rushYards?.toDouble(),
                yardsPerCarry = gameLog.yardsPerCarry?.toDouble(),
                rushLong = gameLog.rushLong,
                rushTds = gameLog.rushTds,
                receptions = gameLog.receptions,
                recYards = gameLog.recYards?.toDouble(),
                yardsPerReception = gameLog.yardsPerReception?.toDouble(),
                recLong = gameLog.recLong,
                recTds = gameLog.recTds,
                wrReceptions = gameLog.wrReceptions,
                wrYards = gameLog.wrYards?.toDouble(),
                wrTds = gameLog.wrTds,
                yardsPerWrReception = gameLog.yardsPerWrReception?.toDouble()
            )

            val scoring = scoringEngine.score(scoringInput)

            PlayerGameLogResult(
                player = player.toDto(),
                gameLog = gameLog.toDto(),
                scoringResult = ScoringResultDto(
                    gameLogId = scoring.gameLogId,
                    playerId = scoring.playerId,
                    position = scoring.position,
                    totalPoints = scoring.totalPoints,
                    breakdown = scoring.breakdown.map {
                        ScoringBreakdownDto(it.label, it.statValue, it.points)
                    }
                )
            )
        }

        val total = results.fold(BigDecimal.ZERO) { acc, r ->
            acc + BigDecimal(r.scoringResult.totalPoints)
        }

        return RosterGameLogResponse(results = results, totalFantasyPoints = total)
    }
}
