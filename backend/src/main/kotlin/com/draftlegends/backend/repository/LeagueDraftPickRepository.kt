package com.draftlegends.backend.repository

import com.draftlegends.backend.entity.LeagueDraftPick
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository

@Repository
interface LeagueDraftPickRepository : JpaRepository<LeagueDraftPick, Int> {
    fun findAllByLeagueIdOrderByOverallPick(leagueId: Int): List<LeagueDraftPick>
    fun existsByLeagueIdAndPlayerId(leagueId: Int, playerId: Int): Boolean
    fun countByLeagueId(leagueId: Int): Int
    fun findAllByLeagueIdAndTeamId(leagueId: Int, teamId: Int): List<LeagueDraftPick>
}
