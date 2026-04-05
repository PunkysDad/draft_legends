package com.draftlegends.backend.entity

import jakarta.persistence.*
import java.math.BigDecimal
import java.time.Instant
import java.time.LocalDateTime

@Entity
@Table(name = "leagues")
data class League(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "league_id")
    val leagueId: Int = 0,

    @Column(name = "name", nullable = false)
    val name: String = "",

    @Column(name = "commissioner_user_id")
    val commissionerUserId: Int = 0,

    @Column(name = "max_teams", nullable = false)
    val maxTeams: Int = 10,

    @Column(name = "roster_qb_slots", nullable = false)
    val rosterQbSlots: Int = 1,

    @Column(name = "roster_rb_slots", nullable = false)
    val rosterRbSlots: Int = 2,

    @Column(name = "roster_wr_slots", nullable = false)
    val rosterWrSlots: Int = 2,

    @Column(name = "salary_cap", nullable = false)
    val salaryCap: BigDecimal = BigDecimal.ZERO,

    @Column(name = "regular_season_weeks", nullable = false)
    val regularSeasonWeeks: Int = 4,

    @Column(name = "entry_fee_coins", nullable = false)
    val entryFeeCoins: Int = 400,

    @Column(name = "draft_pick_seconds", nullable = false)
    val draftPickSeconds: Int = 45,

    @Column(name = "status", nullable = false)
    var status: String = "FORMING",

    @Column(name = "current_week", nullable = false)
    var currentWeek: Int = 0,

    @Column(name = "created_at", nullable = false)
    val createdAt: LocalDateTime = LocalDateTime.now(),

    @Column(name = "draft_started_at")
    var draftStartedAt: Instant? = null
)
