package com.draftlegends.backend

import com.draftlegends.backend.dto.CreateMatchupRequest
import com.draftlegends.backend.dto.PickRequest
import com.draftlegends.backend.matchup.MatchupService
import com.draftlegends.backend.repository.CoinTransactionRepository
import com.draftlegends.backend.repository.WalletRepository
import com.draftlegends.backend.wallet.WalletService
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.transaction.annotation.Transactional

@SpringBootTest
@Transactional
class MatchupWalletIntegrationTest {

    @Autowired private lateinit var matchupService: MatchupService
    @Autowired private lateinit var walletService: WalletService
    @Autowired private lateinit var walletRepository: WalletRepository
    @Autowired private lateinit var coinTransactionRepository: CoinTransactionRepository
    @Autowired private lateinit var jdbcTemplate: JdbcTemplate

    private val testUserId = 1

    @BeforeEach
    fun setup() {
        // Clean test data (order matters for foreign keys)
        jdbcTemplate.execute("DELETE FROM coin_transactions")
        jdbcTemplate.execute("DELETE FROM wallets")
        jdbcTemplate.execute("DELETE FROM matchup_game_logs")
        jdbcTemplate.execute("DELETE FROM draft_picks")
        jdbcTemplate.execute("DELETE FROM matchups")
        jdbcTemplate.execute("DELETE FROM game_logs")
        jdbcTemplate.execute("DELETE FROM players")
        jdbcTemplate.execute("DELETE FROM users")

        // Insert CPU user
        jdbcTemplate.execute(
            "MERGE INTO users (user_id, display_name, email, coin_balance) KEY (user_id) VALUES (-1, 'CPU', 'cpu@test.com', 0)"
        )

        // Insert test user
        jdbcTemplate.execute(
            "INSERT INTO users (user_id, display_name, email, coin_balance) VALUES ($testUserId, 'TestUser', 'test@test.com', 500)"
        )

        // Insert players (2 per position for CPU to have choices)
        jdbcTemplate.execute("INSERT INTO players (player_id, first_name, last_name, position, salary, volatility) VALUES (1, 'Tom', 'Brady', 'QB', 20.00, 5.00)")
        jdbcTemplate.execute("INSERT INTO players (player_id, first_name, last_name, position, salary, volatility) VALUES (2, 'Pat', 'Mahomes', 'QB', 18.00, 4.00)")
        jdbcTemplate.execute("INSERT INTO players (player_id, first_name, last_name, position, salary, volatility) VALUES (3, 'Barry', 'Sanders', 'RB', 22.00, 6.00)")
        jdbcTemplate.execute("INSERT INTO players (player_id, first_name, last_name, position, salary, volatility) VALUES (4, 'Walter', 'Payton', 'RB', 19.00, 5.00)")
        jdbcTemplate.execute("INSERT INTO players (player_id, first_name, last_name, position, salary, volatility) VALUES (5, 'Jerry', 'Rice', 'WR', 25.00, 3.00)")
        jdbcTemplate.execute("INSERT INTO players (player_id, first_name, last_name, position, salary, volatility) VALUES (6, 'Randy', 'Moss', 'WR', 21.00, 7.00)")

        // Insert one game log per player so resolution can work
        jdbcTemplate.execute("INSERT INTO game_logs (game_log_id, player_id, position, pass_yards, pass_tds, interceptions, sacks, fantasy_points) VALUES (101, 1, 'QB', 300, 2, 1, 1, 17.50)")
        jdbcTemplate.execute("INSERT INTO game_logs (game_log_id, player_id, position, pass_yards, pass_tds, interceptions, sacks, fantasy_points) VALUES (102, 2, 'QB', 250, 1, 0, 2, 13.00)")
        jdbcTemplate.execute("INSERT INTO game_logs (game_log_id, player_id, position, rush_yards, rush_tds, rec_yards, rec_tds, receptions, fantasy_points) VALUES (103, 3, 'RB', 150, 2, 30, 0, 3, 30.00)")
        jdbcTemplate.execute("INSERT INTO game_logs (game_log_id, player_id, position, rush_yards, rush_tds, rec_yards, rec_tds, receptions, fantasy_points) VALUES (104, 4, 'RB', 100, 1, 20, 0, 2, 19.00)")
        jdbcTemplate.execute("INSERT INTO game_logs (game_log_id, player_id, position, wr_yards, wr_tds, wr_receptions, fantasy_points) VALUES (105, 5, 'WR', 120, 1, 8, 22.00)")
        jdbcTemplate.execute("INSERT INTO game_logs (game_log_id, player_id, position, wr_yards, wr_tds, wr_receptions, fantasy_points) VALUES (106, 6, 'WR', 80, 0, 5, 10.50)")

        // Create wallet for test user with balance 0
        walletService.getOrCreateWallet(testUserId.toLong())
    }

    @Test
    fun `fullMatchupRewardFlow — wallet rewards fire when matchup resolves`() {
        // Create QUICK_MATCH vs CPU
        val response = matchupService.createMatchup(testUserId, CreateMatchupRequest(mode = "QUICK_MATCH", vsMode = "CPU"))
        val matchupId = response.matchupId

        // Make human picks until matchup completes (CPU picks happen automatically)
        var current = matchupService.getMatchup(matchupId)
        var iterations = 0

        while (current.status == "DRAFTING" && iterations < 6) {
            if (current.currentTurnUserId != testUserId) break

            val draftedIds = current.rosters.flatMap { it.picks }.map { it.playerId }.toSet()
            val nextPickNumber = draftedIds.size + 1
            val position = matchupService.getPickSlotPosition(nextPickNumber)

            val playerId = when (position) {
                "QB" -> listOf(1, 2).first { it !in draftedIds }
                "RB" -> listOf(3, 4).first { it !in draftedIds }
                "WR" -> listOf(5, 6).first { it !in draftedIds }
                else -> throw IllegalStateException("Unexpected position: $position")
            }

            current = matchupService.makePick(testUserId, matchupId, PickRequest(playerId))
            iterations++
        }

        // Verify matchup completed
        val finalState = matchupService.getMatchup(matchupId)
        assertEquals("COMPLETE", finalState.status)

        // Verify wallet rewards
        val wallet = walletRepository.findByUserId(testUserId.toLong()).orElseThrow()
        assertTrue(wallet.balance > 0, "Wallet balance should be > 0 after matchup rewards")

        val transactions = coinTransactionRepository.findByUserIdOrderByCreatedAtDesc(testUserId.toLong())
        assertTrue(transactions.size >= 2, "Should have at least 2 transactions (FIRST_MATCH_BONUS + win/loss)")

        val types = transactions.map { it.transactionType }.toSet()
        assertTrue("FIRST_MATCH_BONUS" in types, "Should include FIRST_MATCH_BONUS transaction")
        assertTrue(
            "WIN_QUICK_MATCH" in types || "LOSE_QUICK_MATCH" in types,
            "Should include WIN_QUICK_MATCH or LOSE_QUICK_MATCH transaction"
        )
    }
}
