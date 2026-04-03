package com.draftlegends.backend.wallet

import com.draftlegends.backend.entity.Wallet
import com.draftlegends.backend.repository.CoinTransactionRepository
import com.draftlegends.backend.repository.WalletRepository
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest

@SpringBootTest
class WalletServiceTest {

    @Autowired private lateinit var walletService: WalletService
    @Autowired private lateinit var walletRepository: WalletRepository
    @Autowired private lateinit var coinTransactionRepository: CoinTransactionRepository

    private val testUserId = 1001L

    @BeforeEach
    fun setup() {
        coinTransactionRepository.deleteAll()
        walletRepository.deleteAll()
    }

    @Test
    fun `getOrCreateWallet creates a new wallet for a new user`() {
        val wallet = walletService.getOrCreateWallet(testUserId)
        assertEquals(testUserId, wallet.userId)
        assertEquals(0, wallet.balance)
        assertTrue(wallet.walletId > 0)
    }

    @Test
    fun `getOrCreateWallet returns existing wallet for a known user`() {
        val created = walletService.getOrCreateWallet(testUserId)
        val fetched = walletService.getOrCreateWallet(testUserId)
        assertEquals(created.walletId, fetched.walletId)
        assertEquals(created.userId, fetched.userId)
    }

    @Test
    fun `credit increases balance and inserts a transaction row`() {
        walletService.getOrCreateWallet(testUserId)

        val wallet = walletService.credit(testUserId, 100, TransactionType.WELCOME_BONUS, "Welcome!")
        assertEquals(100, wallet.balance)

        val transactions = coinTransactionRepository.findByUserIdOrderByCreatedAtDesc(testUserId)
        assertEquals(1, transactions.size)
        assertEquals(100, transactions[0].amount)
        assertEquals("WELCOME_BONUS", transactions[0].transactionType)
    }

    @Test
    fun `spend decreases balance and inserts a negative transaction row`() {
        walletService.getOrCreateWallet(testUserId)
        walletService.credit(testUserId, 200, TransactionType.WELCOME_BONUS)

        val wallet = walletService.spend(testUserId, 50, TransactionType.SPEND, "Bought powerup")
        assertEquals(150, wallet.balance)

        val transactions = coinTransactionRepository.findByUserIdOrderByCreatedAtDesc(testUserId)
        val spendTx = transactions.first { it.transactionType == "SPEND" }
        assertEquals(-50, spendTx.amount)
    }

    @Test
    fun `spend throws InsufficientFundsException when balance is insufficient`() {
        walletService.getOrCreateWallet(testUserId)
        walletService.credit(testUserId, 10, TransactionType.WELCOME_BONUS)

        assertThrows(InsufficientFundsException::class.java) {
            walletService.spend(testUserId, 50, TransactionType.SPEND)
        }
    }

    @Test
    fun `credit then spend in sequence results in correct final balance`() {
        walletService.getOrCreateWallet(testUserId)

        walletService.credit(testUserId, 500, TransactionType.WELCOME_BONUS)
        walletService.credit(testUserId, 100, TransactionType.DAILY_LOGIN)
        walletService.spend(testUserId, 200, TransactionType.LEAGUE_ENTRY)
        val wallet = walletService.spend(testUserId, 150, TransactionType.SPEND)

        assertEquals(250, wallet.balance)
    }

    @Test
    fun `getTransactionHistory returns rows in descending created_at order`() {
        walletService.getOrCreateWallet(testUserId)

        walletService.credit(testUserId, 100, TransactionType.WELCOME_BONUS, "First")
        walletService.credit(testUserId, 50, TransactionType.DAILY_LOGIN, "Second")
        walletService.spend(testUserId, 25, TransactionType.SPEND, "Third")

        val history = walletService.getTransactionHistory(testUserId)
        assertEquals(3, history.size)
        // Most recent first
        assertEquals("SPEND", history[0].transactionType)
        assertEquals("DAILY_LOGIN", history[1].transactionType)
        assertEquals("WELCOME_BONUS", history[2].transactionType)
    }
}
