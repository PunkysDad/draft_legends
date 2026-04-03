package com.draftlegends.backend.dto

import java.time.LocalDateTime

data class WalletResponse(
    val walletId: Int,
    val userId: Long,
    val balance: Int
)

data class TransactionResponse(
    val transactionId: Int,
    val amount: Int,
    val transactionType: String,
    val description: String?,
    val createdAt: LocalDateTime
)

data class SpendRequest(
    val amount: Int,
    val type: String,
    val description: String? = null
)

data class CreditRequest(
    val amount: Int,
    val type: String,
    val description: String? = null
)
