package com.draftlegends.backend.entity

import jakarta.persistence.*
import java.time.LocalDateTime

@Entity
@Table(name = "users")
data class User(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "user_id")
    val userId: Int = 0,

    @Column(name = "google_uid")
    val googleUid: String? = null,

    @Column(name = "apple_uid")
    val appleUid: String? = null,

    @Column(name = "display_name")
    val displayName: String? = null,

    @Column(name = "email")
    val email: String? = null,

    @Column(name = "coin_balance")
    val coinBalance: Int = 500,

    @Column(name = "created_at")
    val createdAt: LocalDateTime = LocalDateTime.now(),

    @Column(name = "last_login_at")
    var lastLoginAt: LocalDateTime? = null
)
