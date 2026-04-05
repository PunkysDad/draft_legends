package com.draftlegends.backend.repository

import com.draftlegends.backend.entity.LeagueMatchupGameLog
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository

@Repository
interface LeagueMatchupGameLogRepository : JpaRepository<LeagueMatchupGameLog, Int> {
    fun findAllByMatchupId(matchupId: Int): List<LeagueMatchupGameLog>
}
