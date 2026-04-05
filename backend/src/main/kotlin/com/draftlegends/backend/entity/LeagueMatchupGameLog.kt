package com.draftlegends.backend.entity

import jakarta.persistence.*
import java.math.BigDecimal

@Entity
@Table(name = "league_matchup_game_logs")
data class LeagueMatchupGameLog(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    val id: Int = 0,

    @Column(name = "matchup_id", nullable = false)
    val matchupId: Int = 0,

    @Column(name = "team_id", nullable = false)
    val teamId: Int = 0,

    @Column(name = "player_id", nullable = false)
    val playerId: Int = 0,

    @Column(name = "game_log_id", nullable = false)
    val gameLogId: Int = 0,

    @Column(name = "fantasy_points", nullable = false)
    val fantasyPoints: BigDecimal = BigDecimal.ZERO
)
