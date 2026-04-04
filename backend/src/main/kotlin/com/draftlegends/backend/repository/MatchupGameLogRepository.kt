package com.draftlegends.backend.repository

import com.draftlegends.backend.entity.MatchupGameLog
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository

@Repository
interface MatchupGameLogRepository : JpaRepository<MatchupGameLog, Int> {
    fun findByMatchupId(matchupId: Int): List<MatchupGameLog>
    fun findByMatchupIdAndUserId(matchupId: Int, userId: Int): List<MatchupGameLog>
}
