package com.draftlegends.backend.repository

import com.draftlegends.backend.entity.LeagueTeam
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository
import java.util.Optional

@Repository
interface LeagueTeamRepository : JpaRepository<LeagueTeam, Int> {
    fun findByLeagueIdAndUserId(leagueId: Int, userId: Int): Optional<LeagueTeam>
    fun findAllByLeagueIdOrderByWinsDescPointsForDesc(leagueId: Int): List<LeagueTeam>
    fun findAllByLeagueId(leagueId: Int): List<LeagueTeam>
    fun countByLeagueId(leagueId: Int): Int
}
