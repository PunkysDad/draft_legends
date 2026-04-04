package com.draftlegends.backend.mapper

import com.draftlegends.backend.dto.GameLogDto
import com.draftlegends.backend.dto.PlayerDto
import com.draftlegends.backend.entity.GameLog
import com.draftlegends.backend.entity.Player

fun Player.toDto() = PlayerDto(
    playerId = playerId,
    firstName = firstName,
    lastName = lastName,
    position = position,
    photoUrl = photoUrl,
    seasonsPlayed = seasonsPlayed,
    totalTouchdowns = totalTouchdowns,
    totalInterceptions = totalInterceptions,
    salary = salary,
    volatility = volatility
)

fun GameLog.toDto() = GameLogDto(
    gameLogId = gameLogId,
    playerId = playerId,
    season = season,
    week = week,
    gameDate = gameDate,
    position = position,
    passAttempts = passAttempts,
    passCompletions = passCompletions,
    completionPct = completionPct,
    yardsPerAttempt = yardsPerAttempt,
    passYards = passYards,
    passTds = passTds,
    interceptions = interceptions,
    passerRating = passerRating,
    sacks = sacks,
    rushAttempts = rushAttempts,
    rushYards = rushYards,
    yardsPerCarry = yardsPerCarry,
    rushLong = rushLong,
    rushTds = rushTds,
    receptions = receptions,
    recYards = recYards,
    yardsPerReception = yardsPerReception,
    recLong = recLong,
    recTds = recTds,
    wrReceptions = wrReceptions,
    wrYards = wrYards,
    wrTds = wrTds,
    yardsPerWrReception = yardsPerWrReception,
    fantasyPoints = fantasyPoints
)
