package com.draftlegends.backend.controller

import com.draftlegends.backend.dto.*
import com.draftlegends.backend.service.PlayerService
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/api/players")
class PlayerController(private val playerService: PlayerService) {

    @GetMapping
    fun getAllPlayers(
        @RequestParam(required = false) position: String?,
        @RequestParam(required = false) sortBy: String?,
        @RequestParam(required = false) sortDir: String?
    ): ResponseEntity<List<PlayerDto>> {
        val players = playerService.getAllPlayers(position, sortBy, sortDir)
        return ResponseEntity.ok(players)
    }

    @GetMapping("/{id}")
    fun getPlayerById(@PathVariable id: Int): ResponseEntity<PlayerDto> {
        val player = playerService.getPlayerById(id)
            ?: return ResponseEntity.notFound().build()
        return ResponseEntity.ok(player)
    }

    @GetMapping("/position/{position}")
    fun getPlayersByPosition(@PathVariable position: String): ResponseEntity<List<PlayerDto>> {
        val players = playerService.getPlayersByPosition(position)
        return ResponseEntity.ok(players)
    }

    @PostMapping("/roster/gamelogs")
    fun getRosterGameLogs(@RequestBody request: RosterGameLogRequest): ResponseEntity<RosterGameLogResponse> {
        val response = playerService.getRosterGameLogs(request)
        return ResponseEntity.ok(response)
    }
}
