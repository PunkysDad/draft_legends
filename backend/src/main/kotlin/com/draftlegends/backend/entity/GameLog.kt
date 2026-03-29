package com.draftlegends.backend.entity

import jakarta.persistence.*
import java.math.BigDecimal
import java.time.LocalDate

@Entity
@Table(name = "game_logs")
data class GameLog(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "game_log_id")
    val gameLogId: Int = 0,

    @Column(name = "player_id")
    val playerId: Int = 0,

    @Column(name = "source_doc_id")
    val sourceDocId: String? = null,

    @Column(name = "season")
    val season: Int? = null,

    @Column(name = "week")
    val week: Int? = null,

    @Column(name = "game_date")
    val gameDate: LocalDate? = null,

    @Column(name = "position")
    val position: String = "",

    // QB fields
    @Column(name = "pass_attempts") val passAttempts: Int? = null,
    @Column(name = "pass_completions") val passCompletions: Int? = null,
    @Column(name = "completion_pct") val completionPct: BigDecimal? = null,
    @Column(name = "yards_per_attempt") val yardsPerAttempt: BigDecimal? = null,
    @Column(name = "pass_yards") val passYards: BigDecimal? = null,
    @Column(name = "pass_tds") val passTds: Int? = null,
    @Column(name = "interceptions") val interceptions: Int? = null,
    @Column(name = "passer_rating") val passerRating: BigDecimal? = null,
    @Column(name = "sacks") val sacks: Int? = null,

    // RB fields
    @Column(name = "rush_attempts") val rushAttempts: Int? = null,
    @Column(name = "rush_yards") val rushYards: BigDecimal? = null,
    @Column(name = "yards_per_carry") val yardsPerCarry: BigDecimal? = null,
    @Column(name = "rush_long") val rushLong: Int? = null,
    @Column(name = "rush_tds") val rushTds: Int? = null,
    @Column(name = "receptions") val receptions: Int? = null,
    @Column(name = "rec_yards") val recYards: BigDecimal? = null,
    @Column(name = "yards_per_reception") val yardsPerReception: BigDecimal? = null,
    @Column(name = "rec_long") val recLong: Int? = null,
    @Column(name = "rec_tds") val recTds: Int? = null,

    // WR fields
    @Column(name = "wr_receptions") val wrReceptions: Int? = null,
    @Column(name = "wr_yards") val wrYards: BigDecimal? = null,
    @Column(name = "wr_tds") val wrTds: Int? = null,
    @Column(name = "yards_per_wr_reception") val yardsPerWrReception: BigDecimal? = null,

    @Column(name = "fantasy_points")
    val fantasyPoints: BigDecimal? = null
)
