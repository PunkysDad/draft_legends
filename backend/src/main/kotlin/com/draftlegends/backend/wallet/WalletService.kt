package com.draftlegends.backend.wallet

import com.draftlegends.backend.entity.CoinTransaction
import com.draftlegends.backend.entity.Wallet
import com.draftlegends.backend.repository.CoinTransactionRepository
import com.draftlegends.backend.repository.WalletRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDate
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

    @Transactional
    fun grantDailyLoginBonus(userId: Long): Wallet {
        val wallet = getOrCreateWallet(userId)
        val today = LocalDate.now()

        if (wallet.lastLoginBonusDate == today) {
            return wallet
        }

        val updated = credit(userId, 50, TransactionType.DAILY_LOGIN, "Daily login bonus")
        updated.lastLoginBonusDate = today
        return walletRepository.save(updated)
    }

    @Transactional
    fun grantWinReward(userId: Long, gameMode: String): Wallet {
        return when (gameMode) {
            "QUICK_MATCH" -> credit(userId, 75, TransactionType.WIN_QUICK_MATCH, "Quick Match win reward")
            "LEAGUE" -> credit(userId, 100, TransactionType.WIN_LEAGUE_MATCH, "League match win reward")
            else -> getOrCreateWallet(userId)
        }
    }

    @Transactional
    fun grantLossConsolation(userId: Long, gameMode: String): Wallet {
        return when (gameMode) {
            "QUICK_MATCH" -> credit(userId, 25, TransactionType.LOSE_QUICK_MATCH, "Quick Match loss consolation")
            else -> getOrCreateWallet(userId)
        }
    }

    @Transactional
    fun grantFirstMatchBonus(userId: Long): Wallet {
        val wallet = getOrCreateWallet(userId)
        val today = LocalDate.now()

        if (wallet.lastFirstMatchBonusDate == today) {
            return wallet
        }

        val updated = credit(userId, 50, TransactionType.FIRST_MATCH_BONUS, "First match of the day bonus")
        updated.lastFirstMatchBonusDate = today
        return walletRepository.save(updated)
    }
}
