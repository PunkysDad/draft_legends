package com.draftlegends.backend.repository

import com.draftlegends.backend.entity.Wallet
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository
import java.util.Optional

@Repository
interface WalletRepository : JpaRepository<Wallet, Int> {
    fun findByUserId(userId: Long): Optional<Wallet>
}
