package com.draftlegends.backend.entity

import jakarta.persistence.*
import java.math.BigDecimal

@Entity
@Table(name = "matchup_game_logs")
data class MatchupGameLog(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    val id: Int = 0,

    @Column(name = "matchup_id")
    val matchupId: Int = 0,

    @Column(name = "user_id")
    val userId: Int = 0,

    @Column(name = "player_id")
    val playerId: Int = 0,

    @Column(name = "game_log_id")
    val gameLogId: Int = 0,

    @Column(name = "fantasy_points")
    val fantasyPoints: BigDecimal = BigDecimal.ZERO
)
