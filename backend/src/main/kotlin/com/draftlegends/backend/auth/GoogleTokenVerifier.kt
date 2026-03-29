package com.draftlegends.backend.auth

import com.fasterxml.jackson.databind.ObjectMapper
import io.jsonwebtoken.Claims
import io.jsonwebtoken.Jwts
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component
import java.math.BigInteger
import java.net.URI
import java.security.KeyFactory
import java.security.PublicKey
import java.security.spec.RSAPublicKeySpec
import java.util.*

@Component
class GoogleTokenVerifier(
    @Value("\${google.clientId}") private val clientId: String
) {
    companion object {
        private const val GOOGLE_KEYS_URL = "https://www.googleapis.com/oauth2/v3/certs"
        private val GOOGLE_ISSUERS = setOf("https://accounts.google.com", "accounts.google.com")
    }

    private val objectMapper = ObjectMapper()

    fun verify(idToken: String): Claims {
        val header = parseHeader(idToken)
        val kid = header["kid"] as? String
            ?: throw IllegalArgumentException("Google token missing kid header")

        val publicKey = fetchGooglePublicKey(kid)

        val claims = Jwts.parser()
            .requireAudience(clientId)
            .verifyWith(publicKey)
            .build()
            .parseSignedClaims(idToken)
            .payload

        val issuer = claims.issuer
        if (issuer !in GOOGLE_ISSUERS) {
            throw IllegalArgumentException("Invalid Google token issuer: $issuer")
        }

        return claims
    }

    private fun parseHeader(token: String): Map<*, *> {
        val headerPart = token.split(".").firstOrNull()
            ?: throw IllegalArgumentException("Invalid token format")
        val decoded = Base64.getUrlDecoder().decode(headerPart)
        return objectMapper.readValue(decoded, Map::class.java)
    }

    private fun fetchGooglePublicKey(kid: String): PublicKey {
        val json = URI(GOOGLE_KEYS_URL).toURL().readText()
        val keysResponse = objectMapper.readValue(json, Map::class.java)

        @Suppress("UNCHECKED_CAST")
        val keys = keysResponse["keys"] as? List<Map<String, Any>>
            ?: throw IllegalStateException("Failed to parse Google public keys")

        val keyData = keys.find { it["kid"] == kid }
            ?: throw IllegalArgumentException("Google public key not found for kid: $kid")

        val n = Base64.getUrlDecoder().decode(keyData["n"] as String)
        val e = Base64.getUrlDecoder().decode(keyData["e"] as String)

        val spec = RSAPublicKeySpec(BigInteger(1, n), BigInteger(1, e))
        return KeyFactory.getInstance("RSA").generatePublic(spec)
    }
}
