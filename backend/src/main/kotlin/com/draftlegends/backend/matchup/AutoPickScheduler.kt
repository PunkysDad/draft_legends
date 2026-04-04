package com.draftlegends.backend.matchup

import org.springframework.scheduling.annotation.EnableScheduling
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component

@Component
@EnableScheduling
class AutoPickScheduler(
    private val matchupService: MatchupService
) {
    @Scheduled(fixedRate = 10000)
    fun checkExpiredTurns() {
        matchupService.handleExpiredTurns()
    }
}
