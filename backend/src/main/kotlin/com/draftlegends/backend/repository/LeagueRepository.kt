package com.draftlegends.backend.repository

import com.draftlegends.backend.entity.League
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository

@Repository
interface LeagueRepository : JpaRepository<League, Int> {
    fun findByStatus(status: String): List<League>
}
