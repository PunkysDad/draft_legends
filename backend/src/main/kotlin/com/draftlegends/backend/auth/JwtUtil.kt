package com.draftlegends.backend.auth

import io.jsonwebtoken.Claims
import io.jsonwebtoken.Jwts
import io.jsonwebtoken.security.Keys
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component
import java.util.*
import javax.crypto.SecretKey

@Component
class JwtUtil(
    @Value("\${jwt.secret}") private val secret: String
) {
    companion object {
        private const val EXPIRATION_MS = 7L * 24 * 60 * 60 * 1000 // 7 days
    }

    private val key: SecretKey by lazy {
        Keys.hmacShaKeyFor(secret.toByteArray())
    }

    fun issueToken(userId: Int, email: String?, displayName: String?): String {
        val now = Date()
        return Jwts.builder()
            .subject(userId.toString())
            .claim("email", email)
            .claim("display_name", displayName)
            .issuedAt(now)
            .expiration(Date(now.time + EXPIRATION_MS))
            .signWith(key)
            .compact()
    }

    fun validateToken(token: String): Claims {
        return Jwts.parser()
            .verifyWith(key)
            .build()
            .parseSignedClaims(token)
            .payload
    }

    fun getUserIdFromToken(token: String): Int {
        return validateToken(token).subject.toInt()
    }
}
