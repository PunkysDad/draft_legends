package com.draftlegends.backend.matchup

import com.draftlegends.backend.dto.*
import com.draftlegends.backend.entity.*
import com.draftlegends.backend.repository.*
import com.draftlegends.scoring.ScoringEngine
import org.springframework.http.HttpStatus
import org.springframework.stereotype.Service
import org.springframework.web.server.ResponseStatusException
import java.math.BigDecimal
import java.math.RoundingMode
import java.time.LocalDateTime

@Service
class MatchupService(
    private val matchupRepository: MatchupRepository,
    private val draftPickRepository: DraftPickRepository,
    private val matchupGameLogRepository: MatchupGameLogRepository,
    private val playerRepository: PlayerRepository,
    private val gameLogRepository: GameLogRepository,
    private val userRepository: UserRepository,
    private val scoringEngine: ScoringEngine
) {
    companion object {
        const val CPU_USER_ID = -1
        private const val PICK_TIMEOUT_SECONDS = 45L
        val QUICK_MATCH_SLOTS = listOf("QB", "RB", "WR")
    }

    fun getPickSlotPosition(pickNumber: Int): String {
        val slotIndex = (pickNumber - 1) / 2
        return QUICK_MATCH_SLOTS[slotIndex]
    }

    fun getPickUserId(pickNumber: Int, user1Id: Int, user2Id: Int): Int {
        return if (pickNumber % 2 == 1) user1Id else user2Id
    }

    fun totalPicksForMode(mode: String): Int {
        return when (mode) {
            "QUICK_MATCH" -> QUICK_MATCH_SLOTS.size * 2
            else -> throw ResponseStatusException(HttpStatus.BAD_REQUEST, "Unknown mode: $mode")
        }
    }

    fun createMatchup(userId: Int, request: CreateMatchupRequest): MatchupResponse {
        if (request.mode != "QUICK_MATCH") {
            throw ResponseStatusException(HttpStatus.BAD_REQUEST, "Unsupported mode: ${request.mode}")
        }

        val isVsCpu = request.vsMode == "CPU"
        val user1GoesFirst = Math.random() < 0.5
        val user1Id: Int
        val user2Id: Int?

        if (isVsCpu) {
            user1Id = if (user1GoesFirst) userId else CPU_USER_ID
            user2Id = if (user1GoesFirst) CPU_USER_ID else userId
        } else {
            user1Id = userId
            user2Id = null
        }

        val matchup = Matchup(
            mode = request.mode,
            status = if (isVsCpu) "DRAFTING" else "PENDING",
            user1Id = user1Id,
            user2Id = user2Id,
            isVsCpu = isVsCpu,
            currentTurnUserId = if (isVsCpu) (if (user1GoesFirst) user1Id else user2Id) else null,
            pickDeadline = if (isVsCpu) LocalDateTime.now().plusSeconds(PICK_TIMEOUT_SECONDS) else null
        )

        val saved = matchupRepository.save(matchup)

        if (isVsCpu && saved.currentTurnUserId == CPU_USER_ID) {
            executeCpuPicksChain(saved)
        }

        return toResponse(matchupRepository.findById(saved.matchupId).orElseThrow())
    }

    fun joinMatchup(userId: Int, matchupId: Int): MatchupResponse {
        val matchup = matchupRepository.findById(matchupId)
            .orElseThrow { ResponseStatusException(HttpStatus.NOT_FOUND, "Matchup not found") }

        if (matchup.status != "PENDING") {
            throw ResponseStatusException(HttpStatus.BAD_REQUEST, "Matchup is not pending")
        }
        if (matchup.user1Id == userId) {
            throw ResponseStatusException(HttpStatus.BAD_REQUEST, "Cannot join your own matchup")
        }

        matchup.user2Id = userId
        matchup.status = "DRAFTING"
        matchup.currentTurnUserId = matchup.user1Id
        matchup.pickDeadline = LocalDateTime.now().plusSeconds(PICK_TIMEOUT_SECONDS)
        matchupRepository.save(matchup)

        return toResponse(matchup)
    }

    fun makePick(userId: Int, matchupId: Int, request: PickRequest): MatchupResponse {
        val matchup = matchupRepository.findById(matchupId)
            .orElseThrow { ResponseStatusException(HttpStatus.NOT_FOUND, "Matchup not found") }

        if (matchup.status != "DRAFTING") {
            throw ResponseStatusException(HttpStatus.BAD_REQUEST, "Matchup is not in drafting state")
        }
        if (matchup.currentTurnUserId != userId) {
            throw ResponseStatusException(HttpStatus.FORBIDDEN, "It is not your turn")
        }

        val picks = draftPickRepository.findByMatchupIdOrderByPickNumberAsc(matchupId)
        val nextPickNumber = picks.size + 1
        val requiredPosition = getPickSlotPosition(nextPickNumber)

        val player = playerRepository.findById(request.playerId)
            .orElseThrow { ResponseStatusException(HttpStatus.BAD_REQUEST, "Player not found") }

        if (player.position != requiredPosition) {
            throw ResponseStatusException(HttpStatus.BAD_REQUEST, "Must draft a $requiredPosition (got ${player.position})")
        }

        val draftedPlayerIds = picks.map { it.playerId }.toSet()
        if (request.playerId in draftedPlayerIds) {
            throw ResponseStatusException(HttpStatus.BAD_REQUEST, "Player already drafted in this matchup")
        }

        recordPick(matchup, userId, request.playerId, nextPickNumber, isCpuPick = false)
        advanceTurn(matchup, nextPickNumber)

        return toResponse(matchupRepository.findById(matchupId).orElseThrow())
    }

    fun getMatchup(matchupId: Int): MatchupResponse {
        val matchup = matchupRepository.findById(matchupId)
            .orElseThrow { ResponseStatusException(HttpStatus.NOT_FOUND, "Matchup not found") }
        return toResponse(matchup)
    }

    fun getUserMatchups(userId: Int): List<MatchupResponse> {
        return matchupRepository.findByUser(userId).map { toResponse(it) }
    }

    fun handleExpiredTurns() {
        val expired = matchupRepository.findByStatusAndPickDeadlineBefore("DRAFTING", LocalDateTime.now())
        for (matchup in expired) {
            autoPickForExpiredTurn(matchup)
        }
    }

    private fun autoPickForExpiredTurn(matchup: Matchup) {
        val picks = draftPickRepository.findByMatchupIdOrderByPickNumberAsc(matchup.matchupId)
        val nextPickNumber = picks.size + 1
        val totalPicks = totalPicksForMode(matchup.mode)
        if (nextPickNumber > totalPicks) return

        val userId = matchup.currentTurnUserId ?: return
        val requiredPosition = getPickSlotPosition(nextPickNumber)
        val draftedPlayerIds = picks.map { it.playerId }.toSet()

        val bestPlayer = findBestAvailablePlayer(requiredPosition, draftedPlayerIds) ?: return

        recordPick(matchup, userId, bestPlayer.playerId, nextPickNumber, isCpuPick = false)
        advanceTurn(matchup, nextPickNumber)
    }

    fun executeCpuPicksChain(matchup: Matchup) {
        var current = matchupRepository.findById(matchup.matchupId).orElseThrow()
        while (current.status == "DRAFTING" && current.currentTurnUserId == CPU_USER_ID) {
            val picks = draftPickRepository.findByMatchupIdOrderByPickNumberAsc(current.matchupId)
            val nextPickNumber = picks.size + 1
            val totalPicks = totalPicksForMode(current.mode)
            if (nextPickNumber > totalPicks) break

            val requiredPosition = getPickSlotPosition(nextPickNumber)
            val draftedPlayerIds = picks.map { it.playerId }.toSet()
            val bestPlayer = findBestAvailablePlayer(requiredPosition, draftedPlayerIds) ?: break

            recordPick(current, CPU_USER_ID, bestPlayer.playerId, nextPickNumber, isCpuPick = true)
            advanceTurn(current, nextPickNumber)
            current = matchupRepository.findById(current.matchupId).orElseThrow()
        }
    }

    fun findBestAvailablePlayer(position: String, excludePlayerIds: Set<Int>): Player? {
        val candidates = playerRepository.findByPositionOrderBySalaryDesc(position)
        return candidates.firstOrNull { it.playerId !in excludePlayerIds }
    }

    private fun recordPick(matchup: Matchup, userId: Int, playerId: Int, pickNumber: Int, isCpuPick: Boolean) {
        draftPickRepository.save(
            DraftPick(
                matchupId = matchup.matchupId,
                userId = userId,
                playerId = playerId,
                pickNumber = pickNumber,
                isCpuPick = isCpuPick
            )
        )
    }

    private fun advanceTurn(matchup: Matchup, completedPickNumber: Int) {
        val totalPicks = totalPicksForMode(matchup.mode)
        if (completedPickNumber >= totalPicks) {
            matchup.status = "LOCKED"
            matchup.currentTurnUserId = null
            matchup.pickDeadline = null
            matchupRepository.save(matchup)
            resolveGameLogs(matchup)
            return
        }

        val nextPickNumber = completedPickNumber + 1
        val nextUserId = getPickUserId(nextPickNumber, matchup.user1Id, matchup.user2Id!!)
        matchup.currentTurnUserId = nextUserId
        matchup.pickDeadline = LocalDateTime.now().plusSeconds(PICK_TIMEOUT_SECONDS)
        matchupRepository.save(matchup)

        if (nextUserId == CPU_USER_ID) {
            executeCpuPicksChain(matchup)
        }
    }

    private fun resolveGameLogs(matchup: Matchup) {
        val picks = draftPickRepository.findByMatchupIdOrderByPickNumberAsc(matchup.matchupId)

        var user1Total = BigDecimal.ZERO
        var user2Total = BigDecimal.ZERO

        for (pick in picks) {
            val randomGameLog = gameLogRepository.findRandomByPlayerId(pick.playerId)
                ?: continue

            val scoringInput = mapToScoringGameLog(randomGameLog)
            val result = scoringEngine.score(scoringInput)
            val points = BigDecimal(result.totalPoints).setScale(2, RoundingMode.HALF_UP)

            matchupGameLogRepository.save(
                MatchupGameLog(
                    matchupId = matchup.matchupId,
                    userId = pick.userId,
                    playerId = pick.playerId,
                    gameLogId = randomGameLog.gameLogId,
                    fantasyPoints = points
                )
            )

            if (pick.userId == matchup.user1Id) {
                user1Total = user1Total.add(points)
            } else {
                user2Total = user2Total.add(points)
            }
        }

        matchup.user1Score = user1Total.setScale(2, RoundingMode.HALF_UP)
        matchup.user2Score = user2Total.setScale(2, RoundingMode.HALF_UP)
        matchup.winnerUserId = when {
            user1Total > user2Total -> matchup.user1Id
            user2Total > user1Total -> matchup.user2Id
            else -> null // tie
        }
        matchup.status = "COMPLETE"
        matchup.completedAt = LocalDateTime.now()
        matchupRepository.save(matchup)
    }

    private fun mapToScoringGameLog(gl: GameLog): com.draftlegends.scoring.GameLog {
        return com.draftlegends.scoring.GameLog(
            gameLogId = gl.gameLogId,
            playerId = gl.playerId,
            position = gl.position,
            season = gl.season,
            week = gl.week,
            passAttempts = gl.passAttempts,
            passCompletions = gl.passCompletions,
            completionPct = gl.completionPct?.toDouble(),
            yardsPerAttempt = gl.yardsPerAttempt?.toDouble(),
            passYards = gl.passYards?.toDouble(),
            passTds = gl.passTds,
            interceptions = gl.interceptions,
            passerRating = gl.passerRating?.toDouble(),
            sacks = gl.sacks,
            rushAttempts = gl.rushAttempts,
            rushYards = gl.rushYards?.toDouble(),
            yardsPerCarry = gl.yardsPerCarry?.toDouble(),
            rushLong = gl.rushLong,
            rushTds = gl.rushTds,
            receptions = gl.receptions,
            recYards = gl.recYards?.toDouble(),
            yardsPerReception = gl.yardsPerReception?.toDouble(),
            recLong = gl.recLong,
            recTds = gl.recTds,
            wrReceptions = gl.wrReceptions,
            wrYards = gl.wrYards?.toDouble(),
            wrTds = gl.wrTds,
            yardsPerWrReception = gl.yardsPerWrReception?.toDouble()
        )
    }

    fun toResponse(matchup: Matchup): MatchupResponse {
        val picks = draftPickRepository.findByMatchupIdOrderByPickNumberAsc(matchup.matchupId)
        val gameLogs = matchupGameLogRepository.findByMatchupId(matchup.matchupId)
        val playerIds = (picks.map { it.playerId } + gameLogs.map { it.playerId }).toSet()
        val playersById = if (playerIds.isNotEmpty()) {
            playerRepository.findAllById(playerIds).associateBy { it.playerId }
        } else emptyMap()

        val userIds = listOfNotNull(matchup.user1Id, matchup.user2Id)
        val usersById = if (userIds.isNotEmpty()) {
            userRepository.findAllById(userIds).associateBy { it.userId }
        } else emptyMap()

        val gameLogsById = if (gameLogs.isNotEmpty()) {
            val glIds = gameLogs.map { it.gameLogId }
            gameLogRepository.findAllById(glIds).associateBy { it.gameLogId }
        } else emptyMap()

        val rosters = userIds.map { uid ->
            val userPicks = picks.filter { it.userId == uid }
            val userGameLogs = gameLogs.filter { it.userId == uid }

            RosterDto(
                userId = uid,
                displayName = usersById[uid]?.displayName,
                picks = userPicks.map { pick ->
                    val p = playersById[pick.playerId]
                    DraftPickDto(
                        pickId = pick.pickId,
                        userId = pick.userId,
                        playerId = pick.playerId,
                        playerFirstName = p?.firstName ?: "",
                        playerLastName = p?.lastName ?: "",
                        position = p?.position ?: "",
                        pickNumber = pick.pickNumber,
                        isCpuPick = pick.isCpuPick,
                        pickedAt = pick.pickedAt
                    )
                },
                gameLogPulls = if (matchup.status == "COMPLETE") {
                    userGameLogs.map { mgl ->
                        val p = playersById[mgl.playerId]
                        val gl = gameLogsById[mgl.gameLogId]
                        val breakdown = if (gl != null) {
                            val result = scoringEngine.score(mapToScoringGameLog(gl))
                            result.breakdown.map { ScoringBreakdownDto(it.label, it.statValue, it.points) }
                        } else emptyList()

                        GameLogPullDto(
                            playerId = mgl.playerId,
                            playerFirstName = p?.firstName ?: "",
                            playerLastName = p?.lastName ?: "",
                            position = p?.position ?: "",
                            gameLogId = mgl.gameLogId,
                            season = gl?.season,
                            week = gl?.week,
                            fantasyPoints = mgl.fantasyPoints,
                            breakdown = breakdown
                        )
                    }
                } else emptyList(),
                totalScore = if (uid == matchup.user1Id) matchup.user1Score else matchup.user2Score
            )
        }

        return MatchupResponse(
            matchupId = matchup.matchupId,
            mode = matchup.mode,
            status = matchup.status,
            isVsCpu = matchup.isVsCpu,
            currentTurnUserId = matchup.currentTurnUserId,
            pickDeadline = matchup.pickDeadline,
            rosters = rosters,
            winnerUserId = matchup.winnerUserId,
            createdAt = matchup.createdAt,
            completedAt = matchup.completedAt
        )
    }
}
