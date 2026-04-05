package com.draftlegends.backend.entity

import jakarta.persistence.*
import java.math.BigDecimal

@Entity
@Table(name = "league_weekly_matchups")
data class LeagueWeeklyMatchup(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "matchup_id")
    val matchupId: Int = 0,

    @Column(name = "league_id", nullable = false)
    val leagueId: Int = 0,

    @Column(name = "week_number", nullable = false)
    val weekNumber: Int = 0,

    @Column(name = "week_type", nullable = false)
    val weekType: String = "REGULAR",

    @Column(name = "home_team_id", nullable = false)
    val homeTeamId: Int = 0,

    @Column(name = "away_team_id", nullable = false)
    val awayTeamId: Int = 0,

    @Column(name = "home_score")
    var homeScore: BigDecimal? = null,

    @Column(name = "away_score")
    var awayScore: BigDecimal? = null,

    @Column(name = "winner_team_id")
    var winnerTeamId: Int? = null,

    @Column(name = "status", nullable = false)
    var status: String = "PENDING"
)
