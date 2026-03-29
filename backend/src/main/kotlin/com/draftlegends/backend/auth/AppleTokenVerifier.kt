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
class AppleTokenVerifier(
    @Value("\${apple.bundleId}") private val bundleId: String
) {
    companion object {
        private const val APPLE_KEYS_URL = "https://appleid.apple.com/auth/keys"
        private const val APPLE_ISSUER = "https://appleid.apple.com"
    }

    private val objectMapper = ObjectMapper()

    fun verify(identityToken: String): Claims {
        val header = parseHeader(identityToken)
        val kid = header["kid"] as? String
            ?: throw IllegalArgumentException("Apple token missing kid header")

        val publicKey = fetchApplePublicKey(kid)

        return Jwts.parser()
            .requireIssuer(APPLE_ISSUER)
            .requireAudience(bundleId)
            .verifyWith(publicKey)
            .build()
            .parseSignedClaims(identityToken)
            .payload
    }

    private fun parseHeader(token: String): Map<*, *> {
        val headerPart = token.split(".").firstOrNull()
            ?: throw IllegalArgumentException("Invalid token format")
        val decoded = Base64.getUrlDecoder().decode(headerPart)
        return objectMapper.readValue(decoded, Map::class.java)
    }

    private fun fetchApplePublicKey(kid: String): PublicKey {
        val json = URI(APPLE_KEYS_URL).toURL().readText()
        val keysResponse = objectMapper.readValue(json, Map::class.java)

        @Suppress("UNCHECKED_CAST")
        val keys = keysResponse["keys"] as? List<Map<String, Any>>
            ?: throw IllegalStateException("Failed to parse Apple public keys")

        val keyData = keys.find { it["kid"] == kid }
            ?: throw IllegalArgumentException("Apple public key not found for kid: $kid")

        val n = Base64.getUrlDecoder().decode(keyData["n"] as String)
        val e = Base64.getUrlDecoder().decode(keyData["e"] as String)

        val spec = RSAPublicKeySpec(BigInteger(1, n), BigInteger(1, e))
        return KeyFactory.getInstance("RSA").generatePublic(spec)
    }
}
