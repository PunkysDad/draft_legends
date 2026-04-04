package com.draftlegends.backend.matchup

import com.draftlegends.backend.dto.CreateMatchupRequest
import com.draftlegends.backend.dto.MatchupResponse
import com.draftlegends.backend.dto.PickRequest
import org.springframework.http.ResponseEntity
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/api/matchups")
class MatchupController(private val matchupService: MatchupService) {

    private fun currentUserId(): Int =
        SecurityContextHolder.getContext().authentication.principal as Int

    @PostMapping
    fun createMatchup(@RequestBody request: CreateMatchupRequest): ResponseEntity<MatchupResponse> {
        val response = matchupService.createMatchup(currentUserId(), request)
        return ResponseEntity.ok(response)
    }

    @PostMapping("/{id}/join")
    fun joinMatchup(@PathVariable id: Int): ResponseEntity<MatchupResponse> {
        val response = matchupService.joinMatchup(currentUserId(), id)
        return ResponseEntity.ok(response)
    }

    @PostMapping("/{id}/pick")
    fun makePick(@PathVariable id: Int, @RequestBody request: PickRequest): ResponseEntity<MatchupResponse> {
        val response = matchupService.makePick(currentUserId(), id, request)
        return ResponseEntity.ok(response)
    }

    @GetMapping("/{id}")
    fun getMatchup(@PathVariable id: Int): ResponseEntity<MatchupResponse> {
        val response = matchupService.getMatchup(id)
        return ResponseEntity.ok(response)
    }

    @GetMapping
    fun getUserMatchups(): ResponseEntity<List<MatchupResponse>> {
        val response = matchupService.getUserMatchups(currentUserId())
        return ResponseEntity.ok(response)
    }
}
