package com.draftlegends.backend.dto

import java.math.BigDecimal

data class CreateLeagueRequest(val name: String)

data class LeaguePickRequest(val playerId: Int)

data class LeagueResponse(
    val leagueId: Int,
    val name: String,
    val status: String,
    val currentWeek: Int,
    val teamCount: Int,
    val maxTeams: Int,
    val salaryCap: BigDecimal,
    val entryFeeCoins: Int
)

data class LeagueTeamResponse(
    val teamId: Int,
    val teamName: String,
    val userId: Int,
    val wins: Int,
    val losses: Int,
    val pointsFor: BigDecimal,
    val pointsAgainst: BigDecimal,
    val draftPosition: Int?
)

data class LeagueDetailResponse(
    val league: LeagueResponse,
    val standings: List<LeagueTeamResponse>
)

data class LeagueDraftPickDto(
    val pickId: Int,
    val teamId: Int,
    val playerId: Int,
    val playerFirstName: String,
    val playerLastName: String,
    val position: String,
    val round: Int,
    val overallPick: Int,
    val positionSlot: String
)

data class AvailablePlayerDto(
    val playerId: Int,
    val firstName: String,
    val lastName: String,
    val position: String,
    val salary: BigDecimal?
)

data class DraftStateResponse(
    val currentPickNumber: Int,
    val currentTeamId: Int?,
    val picksMade: List<LeagueDraftPickDto>,
    val availablePlayersByPosition: Map<String, List<AvailablePlayerDto>>,
    val salaryRemainingPerTeam: Map<Int, BigDecimal>
)

data class DraftPickResponse(
    val pick: LeagueDraftPickDto,
    val draftState: DraftStateResponse
)

data class MatchupGameLogDto(
    val playerId: Int,
    val playerFirstName: String,
    val playerLastName: String,
    val position: String,
    val fantasyPoints: BigDecimal
)

data class WeekMatchupTeamDto(
    val teamId: Int,
    val teamName: String,
    val score: BigDecimal?,
    val gameLogs: List<MatchupGameLogDto>
)

data class WeekMatchupResponse(
    val matchupId: Int,
    val weekNumber: Int,
    val weekType: String,
    val status: String,
    val homeTeam: WeekMatchupTeamDto,
    val awayTeam: WeekMatchupTeamDto,
    val winnerTeamId: Int?
)
