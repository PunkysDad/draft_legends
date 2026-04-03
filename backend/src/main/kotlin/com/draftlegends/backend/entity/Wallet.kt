package com.draftlegends.backend.entity

import jakarta.persistence.*
import java.time.LocalDateTime

@Entity
@Table(name = "wallets")
data class Wallet(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "wallet_id")
    val walletId: Int = 0,

    @Column(name = "user_id", unique = true, nullable = false)
    val userId: Long = 0,

    @Column(name = "balance", nullable = false)
    var balance: Int = 0,

    @Column(name = "created_at", nullable = false)
    val createdAt: LocalDateTime = LocalDateTime.now(),

    @Column(name = "updated_at", nullable = false)
    var updatedAt: LocalDateTime = LocalDateTime.now()
)
