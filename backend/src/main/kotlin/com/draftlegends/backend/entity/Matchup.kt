package com.draftlegends.backend.entity

import jakarta.persistence.*
import java.math.BigDecimal
import java.time.LocalDateTime

@Entity
@Table(name = "matchups")
data class Matchup(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "matchup_id")
    val matchupId: Int = 0,

    @Column(name = "mode")
    val mode: String = "QUICK_MATCH",

    @Column(name = "status")
    var status: String = "PENDING",

    @Column(name = "user1_id")
    val user1Id: Int = 0,

    @Column(name = "user2_id")
    var user2Id: Int? = null,

    @Column(name = "is_vs_cpu")
    val isVsCpu: Boolean = false,

    @Column(name = "current_turn_user_id")
    var currentTurnUserId: Int? = null,

    @Column(name = "pick_deadline")
    var pickDeadline: LocalDateTime? = null,

    @Column(name = "user1_score")
    var user1Score: BigDecimal? = null,

    @Column(name = "user2_score")
    var user2Score: BigDecimal? = null,

    @Column(name = "winner_user_id")
    var winnerUserId: Int? = null,

    @Column(name = "created_at")
    val createdAt: LocalDateTime = LocalDateTime.now(),

    @Column(name = "completed_at")
    var completedAt: LocalDateTime? = null
)
