package com.draftlegends.backend.repository

import com.draftlegends.backend.entity.Matchup
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.stereotype.Repository

@Repository
interface MatchupRepository : JpaRepository<Matchup, Int> {
    @Query("SELECT m FROM Matchup m WHERE m.user1Id = :userId OR m.user2Id = :userId ORDER BY m.createdAt DESC")
    fun findByUser(userId: Int): List<Matchup>

    fun findByStatusAndPickDeadlineBefore(status: String, deadline: java.time.LocalDateTime): List<Matchup>
}
