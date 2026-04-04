package com.draftlegends.backend.entity

import jakarta.persistence.*
import java.time.LocalDateTime

@Entity
@Table(name = "draft_picks")
data class DraftPick(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "pick_id")
    val pickId: Int = 0,

    @Column(name = "matchup_id")
    val matchupId: Int = 0,

    @Column(name = "user_id")
    val userId: Int = 0,

    @Column(name = "player_id")
    val playerId: Int = 0,

    @Column(name = "pick_number")
    val pickNumber: Int = 0,

    @Column(name = "is_cpu_pick")
    val isCpuPick: Boolean = false,

    @Column(name = "picked_at")
    val pickedAt: LocalDateTime = LocalDateTime.now()
)
