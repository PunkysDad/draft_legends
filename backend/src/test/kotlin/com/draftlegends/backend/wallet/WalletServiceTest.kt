package com.draftlegends.backend.wallet

import com.draftlegends.backend.entity.Wallet
import com.draftlegends.backend.repository.CoinTransactionRepository
import com.draftlegends.backend.repository.WalletRepository
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import java.time.LocalDate

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

    @Test
    fun `grantDailyLoginBonus credits 50 coins and sets lastLoginBonusDate to today`() {
        val wallet = walletService.grantDailyLoginBonus(testUserId)

        assertEquals(50, wallet.balance)
        assertEquals(LocalDate.now(), wallet.lastLoginBonusDate)

        val transactions = coinTransactionRepository.findByUserIdOrderByCreatedAtDesc(testUserId)
        assertEquals(1, transactions.size)
        assertEquals(50, transactions[0].amount)
        assertEquals("DAILY_LOGIN", transactions[0].transactionType)
        assertEquals("Daily login bonus", transactions[0].description)
    }

    @Test
    fun `grantDailyLoginBonus called twice in the same day only credits once`() {
        val first = walletService.grantDailyLoginBonus(testUserId)
        val second = walletService.grantDailyLoginBonus(testUserId)

        assertEquals(50, first.balance)
        assertEquals(50, second.balance)

        val transactions = coinTransactionRepository.findByUserIdOrderByCreatedAtDesc(testUserId)
        assertEquals(1, transactions.size)
    }

    @Test
    fun `grantDailyLoginBonus on a new day after a previous bonus grants again`() {
        val wallet = walletService.grantDailyLoginBonus(testUserId)
        assertEquals(50, wallet.balance)

        // Simulate a previous day's bonus by backdating lastLoginBonusDate
        wallet.lastLoginBonusDate = LocalDate.now().minusDays(1)
        walletRepository.save(wallet)

        val updated = walletService.grantDailyLoginBonus(testUserId)
        assertEquals(100, updated.balance)
        assertEquals(LocalDate.now(), updated.lastLoginBonusDate)

        val transactions = coinTransactionRepository.findByUserIdOrderByCreatedAtDesc(testUserId)
        assertEquals(2, transactions.size)
    }

    @Test
    fun `grantWinReward Quick Match credits 75 coins with WIN_QUICK_MATCH type`() {
        val wallet = walletService.grantWinReward(testUserId, "QUICK_MATCH")

        assertEquals(75, wallet.balance)

        val transactions = coinTransactionRepository.findByUserIdOrderByCreatedAtDesc(testUserId)
        assertEquals(1, transactions.size)
        assertEquals(75, transactions[0].amount)
        assertEquals("WIN_QUICK_MATCH", transactions[0].transactionType)
        assertEquals("Quick Match win reward", transactions[0].description)
    }

    @Test
    fun `grantWinReward League credits 100 coins with WIN_LEAGUE_MATCH type`() {
        val wallet = walletService.grantWinReward(testUserId, "LEAGUE")

        assertEquals(100, wallet.balance)

        val transactions = coinTransactionRepository.findByUserIdOrderByCreatedAtDesc(testUserId)
        assertEquals(1, transactions.size)
        assertEquals(100, transactions[0].amount)
        assertEquals("WIN_LEAGUE_MATCH", transactions[0].transactionType)
        assertEquals("League match win reward", transactions[0].description)
    }

    @Test
    fun `grantLossConsolation Quick Match credits 25 coins with LOSE_QUICK_MATCH type`() {
        val wallet = walletService.grantLossConsolation(testUserId, "QUICK_MATCH")

        assertEquals(25, wallet.balance)

        val transactions = coinTransactionRepository.findByUserIdOrderByCreatedAtDesc(testUserId)
        assertEquals(1, transactions.size)
        assertEquals(25, transactions[0].amount)
        assertEquals("LOSE_QUICK_MATCH", transactions[0].transactionType)
        assertEquals("Quick Match loss consolation", transactions[0].description)
    }

    @Test
    fun `grantLossConsolation League returns wallet unchanged`() {
        walletService.getOrCreateWallet(testUserId)

        val wallet = walletService.grantLossConsolation(testUserId, "LEAGUE")

        assertEquals(0, wallet.balance)

        val transactions = coinTransactionRepository.findByUserIdOrderByCreatedAtDesc(testUserId)
        assertEquals(0, transactions.size)
    }

    @Test
    fun `grantFirstMatchBonus credits 50 coins and sets lastFirstMatchBonusDate to today`() {
        val wallet = walletService.grantFirstMatchBonus(testUserId)

        assertEquals(50, wallet.balance)
        assertEquals(LocalDate.now(), wallet.lastFirstMatchBonusDate)

        val transactions = coinTransactionRepository.findByUserIdOrderByCreatedAtDesc(testUserId)
        assertEquals(1, transactions.size)
        assertEquals(50, transactions[0].amount)
        assertEquals("FIRST_MATCH_BONUS", transactions[0].transactionType)
        assertEquals("First match of the day bonus", transactions[0].description)
    }

    @Test
    fun `grantFirstMatchBonus called twice in the same day only credits once`() {
        val first = walletService.grantFirstMatchBonus(testUserId)
        val second = walletService.grantFirstMatchBonus(testUserId)

        assertEquals(50, first.balance)
        assertEquals(50, second.balance)

        val transactions = coinTransactionRepository.findByUserIdOrderByCreatedAtDesc(testUserId)
        assertEquals(1, transactions.size)
    }

    @Test
    fun `grantAdReward credits 60 coins with WATCH_AD type`() {
        val wallet = walletService.grantAdReward(testUserId)

        assertEquals(60, wallet.balance)
        assertEquals(1, wallet.dailyAdRewardCount)
        assertEquals(LocalDate.now(), wallet.lastAdRewardDate)

        val transactions = coinTransactionRepository.findByUserIdOrderByCreatedAtDesc(testUserId)
        assertEquals(1, transactions.size)
        assertEquals(60, transactions[0].amount)
        assertEquals("WATCH_AD", transactions[0].transactionType)
        assertEquals("Rewarded video ad", transactions[0].description)
    }

    @Test
    fun `grantAdReward can be called up to 5 times in one day`() {
        repeat(5) {
            walletService.grantAdReward(testUserId)
        }

        val wallet = walletRepository.findByUserId(testUserId).orElseThrow()
        assertEquals(300, wallet.balance)
        assertEquals(5, wallet.dailyAdRewardCount)

        val transactions = coinTransactionRepository.findByUserIdOrderByCreatedAtDesc(testUserId)
        assertEquals(5, transactions.size)
    }

    @Test
    fun `grantAdReward throws DailyAdLimitReachedException on the 6th call in the same day`() {
        repeat(5) {
            walletService.grantAdReward(testUserId)
        }

        assertThrows(DailyAdLimitReachedException::class.java) {
            walletService.grantAdReward(testUserId)
        }

        val wallet = walletRepository.findByUserId(testUserId).orElseThrow()
        assertEquals(300, wallet.balance)
        assertEquals(5, wallet.dailyAdRewardCount)
    }

    @Test
    fun `grantAdReward resets count on a new day and allows rewards again`() {
        repeat(5) {
            walletService.grantAdReward(testUserId)
        }

        // Simulate previous day
        val wallet = walletRepository.findByUserId(testUserId).orElseThrow()
        wallet.lastAdRewardDate = LocalDate.now().minusDays(1)
        walletRepository.save(wallet)

        val updated = walletService.grantAdReward(testUserId)
        assertEquals(360, updated.balance)
        assertEquals(1, updated.dailyAdRewardCount)
        assertEquals(LocalDate.now(), updated.lastAdRewardDate)
    }
}
