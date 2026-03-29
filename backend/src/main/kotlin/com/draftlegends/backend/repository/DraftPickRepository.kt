package com.draftlegends.backend.repository

import com.draftlegends.backend.entity.DraftPick
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository

@Repository
interface DraftPickRepository : JpaRepository<DraftPick, Int> {
    fun findByMatchupIdOrderByPickNumberAsc(matchupId: Int): List<DraftPick>
    fun findByMatchupIdAndUserId(matchupId: Int, userId: Int): List<DraftPick>
}
