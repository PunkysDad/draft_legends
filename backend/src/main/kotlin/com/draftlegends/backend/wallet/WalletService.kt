package com.draftlegends.backend.wallet

import com.draftlegends.backend.entity.CoinTransaction
import com.draftlegends.backend.entity.Wallet
import com.draftlegends.backend.repository.CoinTransactionRepository
import com.draftlegends.backend.repository.WalletRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDateTime

@Service
class WalletService(
    private val walletRepository: WalletRepository,
    private val coinTransactionRepository: CoinTransactionRepository
) {

    fun getOrCreateWallet(userId: Long): Wallet {
        return walletRepository.findByUserId(userId).orElseGet {
            walletRepository.save(Wallet(userId = userId, balance = 0))
        }
    }

    fun getBalance(userId: Long): Int {
        return getOrCreateWallet(userId).balance
    }

    @Transactional
    fun credit(userId: Long, amount: Int, type: TransactionType, description: String? = null): Wallet {
        require(amount > 0) { "Credit amount must be positive" }

        val wallet = getOrCreateWallet(userId)
        wallet.balance += amount
        wallet.updatedAt = LocalDateTime.now()

        coinTransactionRepository.save(
            CoinTransaction(
                userId = userId,
                amount = amount,
                transactionType = type.name,
                description = description
            )
        )

        return walletRepository.save(wallet)
    }

    @Transactional
    fun spend(userId: Long, amount: Int, type: TransactionType, description: String? = null): Wallet {
        require(amount > 0) { "Spend amount must be positive" }

        val wallet = getOrCreateWallet(userId)
        if (wallet.balance < amount) {
            throw InsufficientFundsException("Insufficient funds: balance=${wallet.balance}, requested=$amount")
        }

        wallet.balance -= amount
        wallet.updatedAt = LocalDateTime.now()

        coinTransactionRepository.save(
            CoinTransaction(
                userId = userId,
                amount = -amount,
                transactionType = type.name,
                description = description
            )
        )

        return walletRepository.save(wallet)
    }

    fun getTransactionHistory(userId: Long): List<CoinTransaction> {
        return coinTransactionRepository.findByUserIdOrderByCreatedAtDesc(userId)
    }

    // TODO: Grant daily login bonus coins
    fun grantDailyLoginBonus(userId: Long) {
    }

    // TODO: Grant reward for winning a match
    fun grantWinReward(userId: Long, gameMode: String) {
    }

    // TODO: Grant consolation coins for a loss
    fun grantLossConsolation(userId: Long, gameMode: String) {
    }

    // TODO: Grant bonus for completing first match
    fun grantFirstMatchBonus(userId: Long) {
    }
}
