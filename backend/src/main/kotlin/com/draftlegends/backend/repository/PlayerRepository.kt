package com.draftlegends.backend.repository

import com.draftlegends.backend.entity.Player
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository

@Repository
interface PlayerRepository : JpaRepository<Player, Int> {
    fun findByPosition(position: String): List<Player>
    fun findByPositionOrderBySalaryDesc(position: String): List<Player>
    fun findAllByOrderBySalaryDesc(): List<Player>
    fun findAllByOrderByVolatilityDesc(): List<Player>
}
