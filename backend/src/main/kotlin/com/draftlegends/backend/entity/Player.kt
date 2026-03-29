package com.draftlegends.backend.entity

import jakarta.persistence.*

@Entity
@Table(name = "players")
data class Player(
    @Id
    @Column(name = "player_id")
    val playerId: Int = 0,

    @Column(name = "first_name")
    val firstName: String = "",

    @Column(name = "last_name")
    val lastName: String = "",

    @Column(name = "position")
    val position: String = "",

    @Column(name = "photo_url")
    val photoUrl: String? = null,

    @Column(name = "seasons_played")
    val seasonsPlayed: Int? = null,

    @Column(name = "total_touchdowns")
    val totalTouchdowns: Int? = null,

    @Column(name = "total_interceptions")
    val totalInterceptions: Int? = null,

    @Column(name = "salary")
    val salary: java.math.BigDecimal? = null,

    @Column(name = "volatility")
    val volatility: java.math.BigDecimal? = null
)
