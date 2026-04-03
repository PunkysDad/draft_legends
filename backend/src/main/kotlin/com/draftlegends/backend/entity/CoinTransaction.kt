package com.draftlegends.backend.entity

import jakarta.persistence.*
import java.time.LocalDateTime

@Entity
@Table(name = "coin_transactions")
data class CoinTransaction(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "transaction_id")
    val transactionId: Int = 0,

    @Column(name = "user_id", nullable = false)
    val userId: Long = 0,

    @Column(name = "amount", nullable = false)
    val amount: Int = 0,

    @Column(name = "transaction_type", nullable = false)
    val transactionType: String = "",

    @Column(name = "description")
    val description: String? = null,

    @Column(name = "created_at", nullable = false)
    val createdAt: LocalDateTime = LocalDateTime.now()
)
