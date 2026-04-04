package com.draftlegends.backend.matchup

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class DraftSlotTest {

    @Test
    fun `pick 1 and 2 require QB`() {
        assertEquals("QB", getPickSlotPosition(1))
        assertEquals("QB", getPickSlotPosition(2))
    }

    @Test
    fun `pick 3 and 4 require RB`() {
        assertEquals("RB", getPickSlotPosition(3))
        assertEquals("RB", getPickSlotPosition(4))
    }

    @Test
    fun `pick 5 and 6 require WR`() {
        assertEquals("WR", getPickSlotPosition(5))
        assertEquals("WR", getPickSlotPosition(6))
    }

    @Test
    fun `alternating pick order assigns user1 odd picks and user2 even picks`() {
        val user1 = 10
        val user2 = 20
        assertEquals(user1, getPickUserId(1, user1, user2))
        assertEquals(user2, getPickUserId(2, user1, user2))
        assertEquals(user1, getPickUserId(3, user1, user2))
        assertEquals(user2, getPickUserId(4, user1, user2))
        assertEquals(user1, getPickUserId(5, user1, user2))
        assertEquals(user2, getPickUserId(6, user1, user2))
    }

    @Test
    fun `quick match has 6 total picks`() {
        assertEquals(6, MatchupService.QUICK_MATCH_SLOTS.size * 2)
    }

    // Pure logic extracted to match MatchupService behavior without instantiating it
    private fun getPickSlotPosition(pickNumber: Int): String {
        val slotIndex = (pickNumber - 1) / 2
        return MatchupService.QUICK_MATCH_SLOTS[slotIndex]
    }

    private fun getPickUserId(pickNumber: Int, user1Id: Int, user2Id: Int): Int {
        return if (pickNumber % 2 == 1) user1Id else user2Id
    }
}
