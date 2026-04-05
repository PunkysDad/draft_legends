package com.draftlegends.backend.entity

import jakarta.persistence.*
import java.math.BigDecimal
import java.time.LocalDateTime

@Entity
@Table(name = "league_teams")
data class LeagueTeam(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "team_id")
    val teamId: Int = 0,

    @Column(name = "league_id", nullable = false)
    val leagueId: Int = 0,

    @Column(name = "user_id", nullable = false)
    val userId: Int = 0,

    @Column(name = "team_name", nullable = false)
    val teamName: String = "",

    @Column(name = "wins", nullable = false)
    var wins: Int = 0,

    @Column(name = "losses", nullable = false)
    var losses: Int = 0,

    @Column(name = "points_for", nullable = false)
    var pointsFor: BigDecimal = BigDecimal.ZERO,

    @Column(name = "points_against", nullable = false)
    var pointsAgainst: BigDecimal = BigDecimal.ZERO,

    @Column(name = "draft_position")
    var draftPosition: Int? = null,

    @Column(name = "created_at", nullable = false)
    val createdAt: LocalDateTime = LocalDateTime.now()
)
