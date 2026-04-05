package com.draftlegends.backend.service

import com.draftlegends.backend.repository.LeagueDraftPickRepository
import com.draftlegends.backend.repository.LeagueRepository
import com.draftlegends.backend.repository.LeagueTeamRepository
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component
import java.time.Duration
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneId

@Component
class LeagueAutoPickScheduler(
    private val leagueRepository: LeagueRepository,
    private val leagueTeamRepository: LeagueTeamRepository,
    private val leagueDraftPickRepository: LeagueDraftPickRepository,
    private val leagueService: LeagueService
) {
    @Scheduled(fixedDelay = 5000)
    fun checkExpiredDraftPicks() {
        val draftingLeagues = leagueRepository.findByStatus("DRAFTING")
        for (league in draftingLeagues) {
            val picks = leagueDraftPickRepository.findAllByLeagueIdOrderByOverallPick(league.leagueId)
            val teams = leagueTeamRepository.findAllByLeagueId(league.leagueId)
            val totalPicks = teams.size * LeagueService.ROSTER_SIZE
            val currentPickNumber = picks.size + 1
            if (currentPickNumber > totalPicks) continue

            val referenceInstant = if (picks.isNotEmpty()) {
                val lastPickedAt = picks.last().pickedAt
                lastPickedAt?.atZone(ZoneId.systemDefault())?.toInstant() ?: Instant.now()
            } else {
                league.draftStartedAt ?: Instant.now()
            }

            val elapsed = Duration.between(referenceInstant, Instant.now()).seconds
            if (elapsed >= league.draftPickSeconds) {
                val sorted = teams.sortedBy { it.draftPosition ?: Int.MAX_VALUE }
                val n = sorted.size
                val zeroIndex = currentPickNumber - 1
                val round = zeroIndex / n
                val posInRound = zeroIndex % n
                val teamIndex = if (round % 2 == 0) posInRound else (n - 1 - posInRound)
                val currentTeam = sorted.getOrNull(teamIndex) ?: continue

                leagueService.autoPickForTeam(league.leagueId, currentTeam.teamId)
            }
        }
    }
}
