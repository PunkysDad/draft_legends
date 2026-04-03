package com.draftlegends.backend.repository

import com.draftlegends.backend.entity.CoinTransaction
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository

@Repository
interface CoinTransactionRepository : JpaRepository<CoinTransaction, Int> {
    fun findByUserIdOrderByCreatedAtDesc(userId: Long): List<CoinTransaction>
}
