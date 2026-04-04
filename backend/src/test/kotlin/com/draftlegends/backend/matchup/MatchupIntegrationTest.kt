package com.draftlegends.backend.matchup

import com.draftlegends.backend.auth.JwtUtil
import com.fasterxml.jackson.databind.ObjectMapper
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.http.MediaType
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.*

@SpringBootTest
@AutoConfigureMockMvc
class MatchupIntegrationTest {

    @Autowired private lateinit var mockMvc: MockMvc
    @Autowired private lateinit var jwtUtil: JwtUtil
    @Autowired private lateinit var jdbcTemplate: JdbcTemplate
    @Autowired private lateinit var objectMapper: ObjectMapper

    private lateinit var userToken: String
    private val testUserId = 100

    @BeforeEach
    fun setup() {
        // Clean test data (order matters for foreign keys)
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

        userToken = jwtUtil.issueToken(testUserId, "test@test.com", "TestUser")
    }

    @Test
    fun `full vs-CPU matchup completes with scores`() {
        // Create CPU matchup
        val createBody = """{"mode":"QUICK_MATCH","vsMode":"CPU"}"""
        val createResult = mockMvc.perform(
            post("/api/matchups")
                .header("Authorization", "Bearer $userToken")
                .contentType(MediaType.APPLICATION_JSON)
                .content(createBody)
        ).andExpect(status().isOk)
            .andReturn()

        val matchup = objectMapper.readTree(createResult.response.contentAsString)
        val matchupId = matchup["matchupId"].asInt()
        var currentStatus = matchup["status"].asText()

        // If CPU went first, it already picked. Keep picking until complete.
        var iterations = 0
        while (currentStatus == "DRAFTING" && iterations < 6) {
            val currentTurn = matchup.path("currentTurnUserId")
            // Check whose turn — if it's our turn, pick
            val getResult = mockMvc.perform(
                get("/api/matchups/$matchupId")
                    .header("Authorization", "Bearer $userToken")
            ).andExpect(status().isOk).andReturn()

            val state = objectMapper.readTree(getResult.response.contentAsString)
            currentStatus = state["status"].asText()
            if (currentStatus != "DRAFTING") break

            val turnUserId = state["currentTurnUserId"].asInt()
            if (turnUserId != testUserId) break // CPU turn, should auto-resolve

            // Determine which position we need
            val picks = state["rosters"].flatMap { it["picks"].toList() }
            val nextPickNumber = picks.size + 1
            val position = when ((nextPickNumber - 1) / 2) {
                0 -> "QB"; 1 -> "RB"; else -> "WR"
            }

            // Find an available player at that position
            val draftedIds = picks.map { it["playerId"].asInt() }.toSet()
            val playerId = when (position) {
                "QB" -> listOf(1, 2).first { it !in draftedIds }
                "RB" -> listOf(3, 4).first { it !in draftedIds }
                "WR" -> listOf(5, 6).first { it !in draftedIds }
                else -> throw IllegalStateException()
            }

            val pickBody = """{"playerId":$playerId}"""
            val pickResult = mockMvc.perform(
                post("/api/matchups/$matchupId/pick")
                    .header("Authorization", "Bearer $userToken")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(pickBody)
            ).andExpect(status().isOk).andReturn()

            val pickState = objectMapper.readTree(pickResult.response.contentAsString)
            currentStatus = pickState["status"].asText()
            iterations++
        }

        // Verify final state is COMPLETE with scores
        val finalResult = mockMvc.perform(
            get("/api/matchups/$matchupId")
                .header("Authorization", "Bearer $userToken")
        ).andExpect(status().isOk)
            .andExpect(jsonPath("$.status").value("COMPLETE"))
            .andExpect(jsonPath("$.rosters[0].totalScore").isNotEmpty)
            .andExpect(jsonPath("$.rosters[1].totalScore").isNotEmpty)
            .andReturn()
    }

    @Test
    fun `picking out of turn returns 403`() {
        // Create a human matchup where we are user1
        val createBody = """{"mode":"QUICK_MATCH","vsMode":"HUMAN"}"""
        val createResult = mockMvc.perform(
            post("/api/matchups")
                .header("Authorization", "Bearer $userToken")
                .contentType(MediaType.APPLICATION_JSON)
                .content(createBody)
        ).andExpect(status().isOk).andReturn()

        val matchup = objectMapper.readTree(createResult.response.contentAsString)
        val matchupId = matchup["matchupId"].asInt()

        // Matchup is PENDING, no one's turn yet — trying to pick should fail
        // First join as a second user
        val user2Id = 200
        jdbcTemplate.execute("INSERT INTO users (user_id, display_name, email, coin_balance) VALUES ($user2Id, 'User2', 'u2@test.com', 500)")
        val user2Token = jwtUtil.issueToken(user2Id, "u2@test.com", "User2")

        mockMvc.perform(
            post("/api/matchups/$matchupId/join")
                .header("Authorization", "Bearer $user2Token")
        ).andExpect(status().isOk)

        // Now it's user1's turn (pick 1). User2 tries to pick → 403
        mockMvc.perform(
            post("/api/matchups/$matchupId/pick")
                .header("Authorization", "Bearer $user2Token")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""{"playerId":1}""")
        ).andExpect(status().isForbidden)
    }

    @Test
    fun `picking wrong position returns 400`() {
        val createBody = """{"mode":"QUICK_MATCH","vsMode":"CPU"}"""
        val createResult = mockMvc.perform(
            post("/api/matchups")
                .header("Authorization", "Bearer $userToken")
                .contentType(MediaType.APPLICATION_JSON)
                .content(createBody)
        ).andExpect(status().isOk).andReturn()

        val matchup = objectMapper.readTree(createResult.response.contentAsString)
        val matchupId = matchup["matchupId"].asInt()
        val status = matchup["status"].asText()

        if (status != "DRAFTING") return // CPU finished everything

        val turnUserId = matchup["currentTurnUserId"].asInt()
        if (turnUserId != testUserId) return // CPU's turn

        // Pick 1 or 2 requires QB — try picking an RB (player 3)
        mockMvc.perform(
            post("/api/matchups/$matchupId/pick")
                .header("Authorization", "Bearer $userToken")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""{"playerId":3}""")
        ).andExpect(status().isBadRequest)
    }

    @Test
    fun `get matchups returns user matchups`() {
        val createBody = """{"mode":"QUICK_MATCH","vsMode":"CPU"}"""
        mockMvc.perform(
            post("/api/matchups")
                .header("Authorization", "Bearer $userToken")
                .contentType(MediaType.APPLICATION_JSON)
                .content(createBody)
        ).andExpect(status().isOk)

        mockMvc.perform(
            get("/api/matchups")
                .header("Authorization", "Bearer $userToken")
        ).andExpect(status().isOk)
            .andExpect(jsonPath("$.length()").value(1))
    }
}
