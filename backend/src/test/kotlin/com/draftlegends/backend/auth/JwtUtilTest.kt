package com.draftlegends.backend.auth

import io.jsonwebtoken.ExpiredJwtException
import io.jsonwebtoken.Jwts
import io.jsonwebtoken.security.Keys
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import java.util.*

class JwtUtilTest {

    private val secret = "test-secret-key-that-is-at-least-32-characters-long"
    private val jwtUtil = JwtUtil(secret)

    @Test
    fun `issueToken creates a valid JWT with correct claims`() {
        val token = jwtUtil.issueToken(42, "player@example.com", "TestUser")
        val claims = jwtUtil.validateToken(token)

        assertEquals("42", claims.subject)
        assertEquals("player@example.com", claims["email"])
        assertEquals("TestUser", claims["display_name"])
        assertNotNull(claims.issuedAt)
        assertNotNull(claims.expiration)
        assertTrue(claims.expiration.after(Date()))
    }

    @Test
    fun `getUserIdFromToken returns correct userId`() {
        val token = jwtUtil.issueToken(99, "test@test.com", "Name")
        assertEquals(99, jwtUtil.getUserIdFromToken(token))
    }

    @Test
    fun `validateToken rejects a tampered token`() {
        val token = jwtUtil.issueToken(1, "a@b.com", "A")
        val tampered = token.dropLast(3) + "xyz"
        assertThrows<Exception> { jwtUtil.validateToken(tampered) }
    }

    @Test
    fun `validateToken rejects an expired token`() {
        val key = Keys.hmacShaKeyFor(secret.toByteArray())
        val expired = Jwts.builder()
            .subject("1")
            .issuedAt(Date(System.currentTimeMillis() - 20000))
            .expiration(Date(System.currentTimeMillis() - 10000))
            .signWith(key)
            .compact()

        assertThrows<ExpiredJwtException> { jwtUtil.validateToken(expired) }
    }

    @Test
    fun `validateToken rejects a token signed with wrong key`() {
        val wrongKey = Keys.hmacShaKeyFor("wrong-secret-key-that-is-at-least-32-chars!!".toByteArray())
        val token = Jwts.builder()
            .subject("1")
            .issuedAt(Date())
            .expiration(Date(System.currentTimeMillis() + 60000))
            .signWith(wrongKey)
            .compact()

        assertThrows<Exception> { jwtUtil.validateToken(token) }
    }

    @Test
    fun `issueToken handles null email and displayName`() {
        val token = jwtUtil.issueToken(10, null, null)
        val claims = jwtUtil.validateToken(token)
        assertEquals("10", claims.subject)
        assertNull(claims["email"])
        assertNull(claims["display_name"])
    }
}
