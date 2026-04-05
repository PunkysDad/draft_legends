package com.draftlegends.backend.service

import com.draftlegends.backend.dto.*
import com.draftlegends.backend.entity.*
import com.draftlegends.backend.repository.*
import com.draftlegends.backend.wallet.TransactionType
import com.draftlegends.backend.wallet.WalletService
import com.draftlegends.scoring.ScoringEngine
import org.springframework.http.HttpStatus
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import org.springframework.web.server.ResponseStatusException
import java.math.BigDecimal
import java.math.RoundingMode
import java.time.Instant
import java.time.LocalDateTime

@Service
class LeagueService(
    private val leagueRepository: LeagueRepository,
    private val leagueTeamRepository: LeagueTeamRepository,
    private val leagueDraftPickRepository: LeagueDraftPickRepository,
    private val leagueWeeklyMatchupRepository: LeagueWeeklyMatchupRepository,
    private val leagueMatchupGameLogRepository: LeagueMatchupGameLogRepository,
    private val playerRepository: PlayerRepository,
    private val gameLogRepository: GameLogRepository,
    private val userRepository: UserRepository,
    private val walletService: WalletService,
    private val scoringEngine: ScoringEngine
) {
    companion object {
        const val DEFAULT_MAX_TEAMS = 10
        const val ROSTER_QB_SLOTS = 1
        const val ROSTER_RB_SLOTS = 2
        const val ROSTER_WR_SLOTS = 2
        const val ROSTER_SIZE = ROSTER_QB_SLOTS + ROSTER_RB_SLOTS + ROSTER_WR_SLOTS // 5
        const val REGULAR_SEASON_WEEKS = 4
        const val ENTRY_FEE = 400
        const val DRAFT_PICK_SECONDS = 45
        // Quick Match has 3 slots, league has 5. Scale proportionally.
        // Quick Match doesn't enforce a cap, so we use a baseline of 60.00 for 3 slots.
        val SALARY_CAP: BigDecimal = BigDecimal("100.00") // 60/3*5 = 100
    }

    private val positionSlotOrder = listOf("QB1") +
            (1..ROSTER_RB_SLOTS).map { "RB$it" } +
            (1..ROSTER_WR_SLOTS).map { "WR$it" }

    // ---------------------------------------------------------------
    // Create League
    // ---------------------------------------------------------------
    @Transactional
    fun createLeague(userId: Int, name: String): LeagueResponse {
        val user = userRepository.findById(userId)
            .orElseThrow { ResponseStatusException(HttpStatus.NOT_FOUND, "User not found") }

        walletService.spend(userId.toLong(), ENTRY_FEE, TransactionType.LEAGUE_ENTRY, "League entry fee: $name")

        val league = leagueRepository.save(
            League(
                name = name,
                commissionerUserId = userId,
                maxTeams = DEFAULT_MAX_TEAMS,
                salaryCap = SALARY_CAP,
                entryFeeCoins = ENTRY_FEE,
                draftPickSeconds = DRAFT_PICK_SECONDS
            )
        )

        val teamName = "${user.displayName ?: "Player"}'s Team"
        leagueTeamRepository.save(
            LeagueTeam(leagueId = league.leagueId, userId = userId, teamName = teamName)
        )

        return toLeagueResponse(league)
    }

    // ---------------------------------------------------------------
    // Join League
    // ---------------------------------------------------------------
    @Transactional
    fun joinLeague(userId: Int, leagueId: Int): LeagueTeamResponse {
        val league = getLeagueEntity(leagueId)

        if (league.status != "FORMING") {
            throw ResponseStatusException(HttpStatus.BAD_REQUEST, "League is not accepting new teams")
        }
        if (leagueTeamRepository.findByLeagueIdAndUserId(leagueId, userId).isPresent) {
            throw ResponseStatusException(HttpStatus.BAD_REQUEST, "Already joined this league")
        }
        val teamCount = leagueTeamRepository.countByLeagueId(leagueId)
        if (teamCount >= league.maxTeams) {
            throw ResponseStatusException(HttpStatus.BAD_REQUEST, "League is full")
        }

        walletService.spend(userId.toLong(), league.entryFeeCoins, TransactionType.LEAGUE_ENTRY, "League entry fee: ${league.name}")

        val user = userRepository.findById(userId)
            .orElseThrow { ResponseStatusException(HttpStatus.NOT_FOUND, "User not found") }
        val teamName = "${user.displayName ?: "Player"}'s Team"
        val team = leagueTeamRepository.save(
            LeagueTeam(leagueId = leagueId, userId = userId, teamName = teamName)
        )

        val newCount = leagueTeamRepository.countByLeagueId(leagueId)
        if (newCount >= league.maxTeams) {
            startDraft(leagueId)
        }

        return toTeamResponse(team)
    }

    // ---------------------------------------------------------------
    // Start Draft (internal)
    // ---------------------------------------------------------------
    @Transactional
    fun startDraft(leagueId: Int) {
        val league = getLeagueEntity(leagueId)
        val teams = leagueTeamRepository.findAllByLeagueId(leagueId).shuffled()

        teams.forEachIndexed { index, team ->
            team.draftPosition = index + 1
            leagueTeamRepository.save(team)
        }

        league.status = "DRAFTING"
        league.draftStartedAt = Instant.now()
        leagueRepository.save(league)
    }

    // ---------------------------------------------------------------
    // Get Draft State
    // ---------------------------------------------------------------
    fun getDraftState(leagueId: Int, userId: Int): DraftStateResponse {
        val league = getLeagueEntity(leagueId)
        val teams = leagueTeamRepository.findAllByLeagueId(leagueId)
        val picks = leagueDraftPickRepository.findAllByLeagueIdOrderByOverallPick(leagueId)
        val totalPicks = teams.size * ROSTER_SIZE
        val currentPickNumber = picks.size + 1

        val currentTeamId = if (currentPickNumber <= totalPicks) {
            getTeamForPick(currentPickNumber, teams)?.teamId
        } else null

        val draftedPlayerIds = picks.map { it.playerId }.toSet()
        val allPlayers = playerRepository.findAllByOrderBySalaryDesc()
        val available = allPlayers.filter { it.playerId !in draftedPlayerIds }
        val availableByPosition = available.groupBy { it.position }
            .mapValues { (_, players) ->
                players.map { AvailablePlayerDto(it.playerId, it.firstName, it.lastName, it.position, it.salary) }
            }

        val salaryRemaining = teams.associate { team ->
            val teamPicks = picks.filter { it.teamId == team.teamId }
            val spent = teamPicks.sumOf { pick ->
                allPlayers.find { it.playerId == pick.playerId }?.salary ?: BigDecimal.ZERO
            }
            team.teamId to league.salaryCap.subtract(spent)
        }

        val pickDtos = picks.map { pick ->
            val player = allPlayers.find { it.playerId == pick.playerId }
            LeagueDraftPickDto(
                pickId = pick.pickId, teamId = pick.teamId, playerId = pick.playerId,
                playerFirstName = player?.firstName ?: "", playerLastName = player?.lastName ?: "",
                position = player?.position ?: "", round = pick.round,
                overallPick = pick.overallPick, positionSlot = pick.positionSlot
            )
        }

        return DraftStateResponse(
            currentPickNumber = currentPickNumber,
            currentTeamId = currentTeamId,
            picksMade = pickDtos,
            availablePlayersByPosition = availableByPosition,
            salaryRemainingPerTeam = salaryRemaining
        )
    }

    // ---------------------------------------------------------------
    // Make Draft Pick
    // ---------------------------------------------------------------
    @Transactional
    fun makeDraftPick(userId: Int, leagueId: Int, playerId: Int): DraftPickResponse {
        val league = getLeagueEntity(leagueId)
        if (league.status != "DRAFTING") {
            throw ResponseStatusException(HttpStatus.BAD_REQUEST, "League is not in drafting state")
        }

        val teams = leagueTeamRepository.findAllByLeagueId(leagueId)
        val picks = leagueDraftPickRepository.findAllByLeagueIdOrderByOverallPick(leagueId)
        val totalPicks = teams.size * ROSTER_SIZE
        val currentPickNumber = picks.size + 1

        if (currentPickNumber > totalPicks) {
            throw ResponseStatusException(HttpStatus.BAD_REQUEST, "Draft is already complete")
        }

        val currentTeam = getTeamForPick(currentPickNumber, teams)
            ?: throw ResponseStatusException(HttpStatus.BAD_REQUEST, "Cannot determine current team")

        if (currentTeam.userId != userId) {
            throw ResponseStatusException(HttpStatus.FORBIDDEN, "It is not your turn")
        }

        return executePickInternal(league, currentTeam, playerId, currentPickNumber, picks, teams)
    }

    @Transactional
    fun autoPickForTeam(leagueId: Int, teamId: Int) {
        val league = getLeagueEntity(leagueId)
        if (league.status != "DRAFTING") return

        val teams = leagueTeamRepository.findAllByLeagueId(leagueId)
        val picks = leagueDraftPickRepository.findAllByLeagueIdOrderByOverallPick(leagueId)
        val totalPicks = teams.size * ROSTER_SIZE
        val currentPickNumber = picks.size + 1
        if (currentPickNumber > totalPicks) return

        val currentTeam = getTeamForPick(currentPickNumber, teams) ?: return
        if (currentTeam.teamId != teamId) return

        val nextSlot = getNextSlotForTeam(currentTeam.teamId, leagueId)
            ?: return
        val neededPosition = nextSlot.substring(0, 2)

        val draftedPlayerIds = picks.map { it.playerId }.toSet()
        val teamPicks = picks.filter { it.teamId == currentTeam.teamId }
        val teamSalarySpent = teamPicks.sumOf { pick ->
            playerRepository.findById(pick.playerId).map { it.salary ?: BigDecimal.ZERO }.orElse(BigDecimal.ZERO)
        }
        val remaining = league.salaryCap.subtract(teamSalarySpent)

        val bestPlayer = playerRepository.findByPositionOrderBySalaryDesc(neededPosition)
            .firstOrNull { it.playerId !in draftedPlayerIds && (it.salary ?: BigDecimal.ZERO) <= remaining }
            ?: playerRepository.findByPositionOrderBySalaryDesc(neededPosition)
                .firstOrNull { it.playerId !in draftedPlayerIds }
            ?: return

        executePickInternal(league, currentTeam, bestPlayer.playerId, currentPickNumber, picks, teams)
    }

    private fun executePickInternal(
        league: League, team: LeagueTeam, playerId: Int,
        currentPickNumber: Int, picks: List<LeagueDraftPick>, teams: List<LeagueTeam>
    ): DraftPickResponse {
        if (leagueDraftPickRepository.existsByLeagueIdAndPlayerId(league.leagueId, playerId)) {
            throw ResponseStatusException(HttpStatus.BAD_REQUEST, "Player already drafted")
        }

        val player = playerRepository.findById(playerId)
            .orElseThrow { ResponseStatusException(HttpStatus.BAD_REQUEST, "Player not found") }

        val nextSlot = getNextSlotForTeam(team.teamId, league.leagueId)
            ?: throw ResponseStatusException(HttpStatus.BAD_REQUEST, "Team roster is full")
        val neededPosition = nextSlot.substring(0, 2)

        if (player.position != neededPosition) {
            throw ResponseStatusException(HttpStatus.BAD_REQUEST, "Must draft a $neededPosition (got ${player.position})")
        }

        val teamPicks = picks.filter { it.teamId == team.teamId }
        val teamSalarySpent = teamPicks.sumOf { pick ->
            playerRepository.findById(pick.playerId).map { it.salary ?: BigDecimal.ZERO }.orElse(BigDecimal.ZERO)
        }
        val remaining = league.salaryCap.subtract(teamSalarySpent)
        if ((player.salary ?: BigDecimal.ZERO) > remaining) {
            throw ResponseStatusException(HttpStatus.BAD_REQUEST, "Salary cap exceeded")
        }

        val round = ((currentPickNumber - 1) / teams.size) + 1
        val savedPick = leagueDraftPickRepository.save(
            LeagueDraftPick(
                leagueId = league.leagueId, teamId = team.teamId, playerId = playerId,
                round = round, overallPick = currentPickNumber, positionSlot = nextSlot,
                pickedAt = LocalDateTime.now()
            )
        )

        val totalPicks = teams.size * ROSTER_SIZE
        if (currentPickNumber >= totalPicks) {
            completeDraft(league.leagueId)
        }

        val draftState = getDraftState(league.leagueId, team.userId)
        val pickDto = LeagueDraftPickDto(
            pickId = savedPick.pickId, teamId = savedPick.teamId, playerId = savedPick.playerId,
            playerFirstName = player.firstName, playerLastName = player.lastName,
            position = player.position, round = savedPick.round,
            overallPick = savedPick.overallPick, positionSlot = savedPick.positionSlot
        )
        return DraftPickResponse(pick = pickDto, draftState = draftState)
    }

    // ---------------------------------------------------------------
    // Complete Draft
    // ---------------------------------------------------------------
    @Transactional
    fun completeDraft(leagueId: Int) {
        val league = getLeagueEntity(leagueId)
        league.status = "ACTIVE"
        league.currentWeek = 1
        leagueRepository.save(league)

        generateRegularSeasonSchedule(leagueId)
        resolveWeek(leagueId, 1)
    }

    // ---------------------------------------------------------------
    // Generate Schedule (round-robin)
    // ---------------------------------------------------------------
    private fun generateRegularSeasonSchedule(leagueId: Int) {
        val teams = leagueTeamRepository.findAllByLeagueIdOrderByWinsDescPointsForDesc(leagueId)
        val teamIds = teams.map { it.teamId }
        val n = teamIds.size

        // Standard round-robin: fix first team, rotate rest
        val rotatingIds = teamIds.toMutableList()
        val allRounds = mutableListOf<List<Pair<Int, Int>>>()

        for (round in 0 until n - 1) {
            val matchups = mutableListOf<Pair<Int, Int>>()
            for (i in 0 until n / 2) {
                val home = rotatingIds[i]
                val away = rotatingIds[n - 1 - i]
                matchups.add(home to away)
            }
            allRounds.add(matchups)
            // Rotate: keep first element fixed, rotate rest
            val last = rotatingIds.removeAt(rotatingIds.size - 1)
            rotatingIds.add(1, last)
        }

        for (week in 1..REGULAR_SEASON_WEEKS) {
            val roundIndex = (week - 1) % allRounds.size
            for ((home, away) in allRounds[roundIndex]) {
                leagueWeeklyMatchupRepository.save(
                    LeagueWeeklyMatchup(
                        leagueId = leagueId, weekNumber = week, weekType = "REGULAR",
                        homeTeamId = home, awayTeamId = away
                    )
                )
            }
        }
    }

    // ---------------------------------------------------------------
    // Resolve Week
    // ---------------------------------------------------------------
    @Transactional
    fun resolveWeek(leagueId: Int, weekNumber: Int) {
        val matchups = leagueWeeklyMatchupRepository.findAllByLeagueIdAndWeekNumber(leagueId, weekNumber)
            .filter { it.status == "PENDING" }

        for (matchup in matchups) {
            val homeScore = resolveTeamScore(matchup.matchupId, matchup.homeTeamId, leagueId)
            val awayScore = resolveTeamScore(matchup.matchupId, matchup.awayTeamId, leagueId)

            matchup.homeScore = homeScore
            matchup.awayScore = awayScore
            matchup.winnerTeamId = when {
                homeScore > awayScore -> matchup.homeTeamId
                awayScore > homeScore -> matchup.awayTeamId
                else -> matchup.homeTeamId // tie goes to home
            }
            matchup.status = "COMPLETE"
            leagueWeeklyMatchupRepository.save(matchup)

            // Update standings
            val homeTeam = leagueTeamRepository.findById(matchup.homeTeamId).orElseThrow()
            val awayTeam = leagueTeamRepository.findById(matchup.awayTeamId).orElseThrow()

            homeTeam.pointsFor = homeTeam.pointsFor.add(homeScore)
            homeTeam.pointsAgainst = homeTeam.pointsAgainst.add(awayScore)
            awayTeam.pointsFor = awayTeam.pointsFor.add(awayScore)
            awayTeam.pointsAgainst = awayTeam.pointsAgainst.add(homeScore)

            if (matchup.winnerTeamId == matchup.homeTeamId) {
                homeTeam.wins++
                awayTeam.losses++
            } else {
                awayTeam.wins++
                homeTeam.losses++
            }

            leagueTeamRepository.save(homeTeam)
            leagueTeamRepository.save(awayTeam)
        }

        advanceLeague(leagueId)
    }

    private fun resolveTeamScore(matchupId: Int, teamId: Int, leagueId: Int): BigDecimal {
        val picks = leagueDraftPickRepository.findAllByLeagueIdAndTeamId(leagueId, teamId)
        var total = BigDecimal.ZERO

        for (pick in picks) {
            val gameLog = gameLogRepository.findRandomByPlayerId(pick.playerId) ?: continue
            val scoringInput = mapToScoringGameLog(gameLog)
            val result = scoringEngine.score(scoringInput)
            val points = BigDecimal(result.totalPoints).setScale(2, RoundingMode.HALF_UP)

            leagueMatchupGameLogRepository.save(
                LeagueMatchupGameLog(
                    matchupId = matchupId, teamId = teamId,
                    playerId = pick.playerId, gameLogId = gameLog.gameLogId,
                    fantasyPoints = points
                )
            )

            total = total.add(points)
        }

        return total.setScale(2, RoundingMode.HALF_UP)
    }

    // ---------------------------------------------------------------
    // Advance League
    // ---------------------------------------------------------------
    @Transactional
    fun advanceLeague(leagueId: Int) {
        val league = getLeagueEntity(leagueId)

        if (league.status == "ACTIVE") {
            if (league.currentWeek < league.regularSeasonWeeks) {
                league.currentWeek++
                leagueRepository.save(league)
                resolveWeek(leagueId, league.currentWeek)
            } else {
                startPlayoffs(leagueId)
            }
        } else if (league.status == "PLAYOFFS") {
            val currentWeek = league.currentWeek
            val pendingCount = leagueWeeklyMatchupRepository.countByLeagueIdAndWeekNumberAndStatus(leagueId, currentWeek, "PENDING")
            if (pendingCount == 0) {
                val matchups = leagueWeeklyMatchupRepository.findAllByLeagueIdAndWeekNumber(leagueId, currentWeek)
                if (matchups.any { it.weekType == "FINAL" }) {
                    league.status = "COMPLETE"
                    leagueRepository.save(league)
                } else {
                    // Semifinals done, create final
                    val winners = matchups.mapNotNull { it.winnerTeamId }
                    if (winners.size == 2) {
                        league.currentWeek = currentWeek + 1
                        leagueRepository.save(league)
                        leagueWeeklyMatchupRepository.save(
                            LeagueWeeklyMatchup(
                                leagueId = leagueId, weekNumber = league.currentWeek,
                                weekType = "FINAL", homeTeamId = winners[0], awayTeamId = winners[1]
                            )
                        )
                        resolveWeek(leagueId, league.currentWeek)
                    }
                }
            }
        }
    }

    // ---------------------------------------------------------------
    // Start Playoffs
    // ---------------------------------------------------------------
    @Transactional
    fun startPlayoffs(leagueId: Int) {
        val league = getLeagueEntity(leagueId)
        val standings = leagueTeamRepository.findAllByLeagueIdOrderByWinsDescPointsForDesc(leagueId)
        val top4 = standings.take(4)

        league.status = "PLAYOFFS"
        league.currentWeek = league.regularSeasonWeeks + 1 // Week 5
        leagueRepository.save(league)

        // Seed 1 vs Seed 4, Seed 2 vs Seed 3
        leagueWeeklyMatchupRepository.save(
            LeagueWeeklyMatchup(
                leagueId = leagueId, weekNumber = league.currentWeek,
                weekType = "SEMIFINAL", homeTeamId = top4[0].teamId, awayTeamId = top4[3].teamId
            )
        )
        leagueWeeklyMatchupRepository.save(
            LeagueWeeklyMatchup(
                leagueId = leagueId, weekNumber = league.currentWeek,
                weekType = "SEMIFINAL", homeTeamId = top4[1].teamId, awayTeamId = top4[2].teamId
            )
        )

        resolveWeek(leagueId, league.currentWeek)
    }

    // ---------------------------------------------------------------
    // Get League
    // ---------------------------------------------------------------
    fun getLeague(leagueId: Int): LeagueDetailResponse {
        val league = getLeagueEntity(leagueId)
        val teams = leagueTeamRepository.findAllByLeagueIdOrderByWinsDescPointsForDesc(leagueId)
        return LeagueDetailResponse(
            league = toLeagueResponse(league),
            standings = teams.map { toTeamResponse(it) }
        )
    }

    // ---------------------------------------------------------------
    // Get Week Matchups
    // ---------------------------------------------------------------
    fun getWeekMatchups(leagueId: Int, weekNumber: Int): List<WeekMatchupResponse> {
        val matchups = leagueWeeklyMatchupRepository.findAllByLeagueIdAndWeekNumber(leagueId, weekNumber)
        return matchups.map { matchup ->
            val homeGameLogs = leagueMatchupGameLogRepository.findAllByMatchupId(matchup.matchupId)
                .filter { it.teamId == matchup.homeTeamId }
            val awayGameLogs = leagueMatchupGameLogRepository.findAllByMatchupId(matchup.matchupId)
                .filter { it.teamId == matchup.awayTeamId }

            val allTeams = leagueTeamRepository.findAllByLeagueId(leagueId)
            val homeTeam = allTeams.find { it.teamId == matchup.homeTeamId }!!
            val awayTeam = allTeams.find { it.teamId == matchup.awayTeamId }!!

            WeekMatchupResponse(
                matchupId = matchup.matchupId,
                weekNumber = matchup.weekNumber,
                weekType = matchup.weekType,
                status = matchup.status,
                homeTeam = buildTeamDto(homeTeam, matchup.homeScore, homeGameLogs),
                awayTeam = buildTeamDto(awayTeam, matchup.awayScore, awayGameLogs),
                winnerTeamId = matchup.winnerTeamId
            )
        }
    }

    private fun buildTeamDto(team: LeagueTeam, score: BigDecimal?, gameLogs: List<LeagueMatchupGameLog>): WeekMatchupTeamDto {
        return WeekMatchupTeamDto(
            teamId = team.teamId,
            teamName = team.teamName,
            score = score,
            gameLogs = gameLogs.map { gl ->
                val player = playerRepository.findById(gl.playerId).orElse(null)
                MatchupGameLogDto(
                    playerId = gl.playerId,
                    playerFirstName = player?.firstName ?: "",
                    playerLastName = player?.lastName ?: "",
                    position = player?.position ?: "",
                    fantasyPoints = gl.fantasyPoints
                )
            }
        )
    }

    // ---------------------------------------------------------------
    // Helpers
    // ---------------------------------------------------------------
    private fun getLeagueEntity(leagueId: Int): League {
        return leagueRepository.findById(leagueId)
            .orElseThrow { ResponseStatusException(HttpStatus.NOT_FOUND, "League not found") }
    }

    private fun getTeamForPick(pickNumber: Int, teams: List<LeagueTeam>): LeagueTeam? {
        val sorted = teams.sortedBy { it.draftPosition ?: Int.MAX_VALUE }
        val n = sorted.size
        val zeroIndex = pickNumber - 1
        val round = zeroIndex / n
        val posInRound = zeroIndex % n
        // Snake: odd rounds reverse
        val teamIndex = if (round % 2 == 0) posInRound else (n - 1 - posInRound)
        return sorted.getOrNull(teamIndex)
    }

    private fun getNextSlotForTeam(teamId: Int, leagueId: Int): String? {
        val teamPicks = leagueDraftPickRepository.findAllByLeagueIdAndTeamId(leagueId, teamId)
        val filledSlots = teamPicks.map { it.positionSlot }.toSet()
        return positionSlotOrder.firstOrNull { it !in filledSlots }
    }

    private fun toLeagueResponse(league: League): LeagueResponse {
        val teamCount = leagueTeamRepository.countByLeagueId(league.leagueId)
        return LeagueResponse(
            leagueId = league.leagueId, name = league.name, status = league.status,
            currentWeek = league.currentWeek, teamCount = teamCount,
            maxTeams = league.maxTeams, salaryCap = league.salaryCap,
            entryFeeCoins = league.entryFeeCoins
        )
    }

    private fun toTeamResponse(team: LeagueTeam): LeagueTeamResponse {
        return LeagueTeamResponse(
            teamId = team.teamId, teamName = team.teamName, userId = team.userId,
            wins = team.wins, losses = team.losses, pointsFor = team.pointsFor,
            pointsAgainst = team.pointsAgainst, draftPosition = team.draftPosition
        )
    }

    private fun mapToScoringGameLog(gl: GameLog): com.draftlegends.scoring.GameLog {
        return com.draftlegends.scoring.GameLog(
            gameLogId = gl.gameLogId, playerId = gl.playerId, position = gl.position,
            season = gl.season, week = gl.week,
            passAttempts = gl.passAttempts, passCompletions = gl.passCompletions,
            completionPct = gl.completionPct?.toDouble(), yardsPerAttempt = gl.yardsPerAttempt?.toDouble(),
            passYards = gl.passYards?.toDouble(), passTds = gl.passTds,
            interceptions = gl.interceptions, passerRating = gl.passerRating?.toDouble(),
            sacks = gl.sacks, rushAttempts = gl.rushAttempts,
            rushYards = gl.rushYards?.toDouble(), yardsPerCarry = gl.yardsPerCarry?.toDouble(),
            rushLong = gl.rushLong, rushTds = gl.rushTds,
            receptions = gl.receptions, recYards = gl.recYards?.toDouble(),
            yardsPerReception = gl.yardsPerReception?.toDouble(), recLong = gl.recLong,
            recTds = gl.recTds, wrReceptions = gl.wrReceptions,
            wrYards = gl.wrYards?.toDouble(), wrTds = gl.wrTds,
            yardsPerWrReception = gl.yardsPerWrReception?.toDouble()
        )
    }
}
