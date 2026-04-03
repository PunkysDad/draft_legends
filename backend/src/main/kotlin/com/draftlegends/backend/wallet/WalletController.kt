package com.draftlegends.backend.wallet

import com.draftlegends.backend.dto.CreditRequest
import com.draftlegends.backend.dto.SpendRequest
import com.draftlegends.backend.dto.TransactionResponse
import com.draftlegends.backend.dto.WalletResponse
import org.springframework.http.ResponseEntity
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/api/wallet")
class WalletController(private val walletService: WalletService) {

    private fun currentUserId(): Long =
        (SecurityContextHolder.getContext().authentication.principal as Int).toLong()

    @GetMapping
    fun getWallet(): ResponseEntity<WalletResponse> {
        val wallet = walletService.getOrCreateWallet(currentUserId())
        return ResponseEntity.ok(
            WalletResponse(
                walletId = wallet.walletId,
                userId = wallet.userId,
                balance = wallet.balance
            )
        )
    }

    @GetMapping("/transactions")
    fun getTransactions(): ResponseEntity<List<TransactionResponse>> {
        val transactions = walletService.getTransactionHistory(currentUserId())
        return ResponseEntity.ok(
            transactions.map { tx ->
                TransactionResponse(
                    transactionId = tx.transactionId,
                    amount = tx.amount,
                    transactionType = tx.transactionType,
                    description = tx.description,
                    createdAt = tx.createdAt
                )
            }
        )
    }

    @PostMapping("/spend")
    fun spend(@RequestBody request: SpendRequest): ResponseEntity<WalletResponse> {
        val type = TransactionType.valueOf(request.type)
        val wallet = walletService.spend(currentUserId(), request.amount, type, request.description)
        return ResponseEntity.ok(
            WalletResponse(
                walletId = wallet.walletId,
                userId = wallet.userId,
                balance = wallet.balance
            )
        )
    }

    @PostMapping("/credit")
    fun credit(@RequestBody request: CreditRequest): ResponseEntity<WalletResponse> {
        val type = TransactionType.valueOf(request.type)
        val wallet = walletService.credit(currentUserId(), request.amount, type, request.description)
        return ResponseEntity.ok(
            WalletResponse(
                walletId = wallet.walletId,
                userId = wallet.userId,
                balance = wallet.balance
            )
        )
    }
}
