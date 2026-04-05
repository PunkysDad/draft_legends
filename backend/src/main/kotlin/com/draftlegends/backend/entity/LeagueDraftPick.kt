package com.draftlegends.backend.entity

import jakarta.persistence.*
import java.time.LocalDateTime

@Entity
@Table(name = "league_draft_picks")
data class LeagueDraftPick(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "pick_id")
    val pickId: Int = 0,

    @Column(name = "league_id", nullable = false)
    val leagueId: Int = 0,

    @Column(name = "team_id", nullable = false)
    val teamId: Int = 0,

    @Column(name = "player_id", nullable = false)
    val playerId: Int = 0,

    @Column(name = "round", nullable = false)
    val round: Int = 0,

    @Column(name = "overall_pick", nullable = false)
    val overallPick: Int = 0,

    @Column(name = "position_slot", nullable = false)
    val positionSlot: String = "",

    @Column(name = "picked_at")
    val pickedAt: LocalDateTime? = null
)
