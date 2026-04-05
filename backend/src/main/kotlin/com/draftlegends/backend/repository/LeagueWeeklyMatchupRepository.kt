package com.draftlegends.backend.repository

import com.draftlegends.backend.entity.LeagueWeeklyMatchup
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository

@Repository
interface LeagueWeeklyMatchupRepository : JpaRepository<LeagueWeeklyMatchup, Int> {
    fun findAllByLeagueIdAndWeekNumber(leagueId: Int, weekNumber: Int): List<LeagueWeeklyMatchup>
    fun countByLeagueIdAndWeekNumberAndStatus(leagueId: Int, weekNumber: Int, status: String): Int
}
