package com.draftlegends.backend.controller

import com.draftlegends.backend.dto.*
import com.draftlegends.backend.service.LeagueService
import org.springframework.http.ResponseEntity
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/api/leagues")
class LeagueController(private val leagueService: LeagueService) {

    private fun currentUserId(): Int =
        SecurityContextHolder.getContext().authentication.principal as Int

    @PostMapping
    fun createLeague(@RequestBody request: CreateLeagueRequest): ResponseEntity<LeagueResponse> {
        return ResponseEntity.ok(leagueService.createLeague(currentUserId(), request.name))
    }

    @PostMapping("/{id}/join")
    fun joinLeague(@PathVariable id: Int): ResponseEntity<LeagueTeamResponse> {
        return ResponseEntity.ok(leagueService.joinLeague(currentUserId(), id))
    }

    @GetMapping("/{id}")
    fun getLeague(@PathVariable id: Int): ResponseEntity<LeagueDetailResponse> {
        return ResponseEntity.ok(leagueService.getLeague(id))
    }

    @GetMapping("/{id}/draft")
    fun getDraftState(@PathVariable id: Int): ResponseEntity<DraftStateResponse> {
        return ResponseEntity.ok(leagueService.getDraftState(id, currentUserId()))
    }

    @PostMapping("/{id}/draft/pick")
    fun makeDraftPick(
        @PathVariable id: Int,
        @RequestBody request: LeaguePickRequest
    ): ResponseEntity<DraftPickResponse> {
        return ResponseEntity.ok(leagueService.makeDraftPick(currentUserId(), id, request.playerId))
    }

    @GetMapping("/{id}/standings")
    fun getStandings(@PathVariable id: Int): ResponseEntity<List<LeagueTeamResponse>> {
        val detail = leagueService.getLeague(id)
        return ResponseEntity.ok(detail.standings)
    }

    @GetMapping("/{id}/weeks/{week}")
    fun getWeekMatchups(
        @PathVariable id: Int,
        @PathVariable week: Int
    ): ResponseEntity<List<WeekMatchupResponse>> {
        return ResponseEntity.ok(leagueService.getWeekMatchups(id, week))
    }
}
