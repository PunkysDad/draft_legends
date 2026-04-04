package com.draftlegends.backend.repository

import com.draftlegends.backend.entity.GameLog
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.stereotype.Repository

@Repository
interface GameLogRepository : JpaRepository<GameLog, Int> {
    fun findByPlayerId(playerId: Int): List<GameLog>

    @Query(
        value = "SELECT * FROM game_logs WHERE player_id = :playerId ORDER BY RANDOM() LIMIT 1",
        nativeQuery = true
    )
    fun findRandomByPlayerId(playerId: Int): GameLog?
}
