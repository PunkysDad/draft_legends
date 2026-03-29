package com.draftlegends.backend.auth

import com.draftlegends.backend.entity.User
import com.draftlegends.backend.repository.UserRepository
import com.google.firebase.auth.FirebaseAuth
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*
import java.time.LocalDateTime

data class GoogleAuthRequest(val idToken: String)
data class AppleAuthRequest(val identityToken: String)
data class AuthResponse(val token: String, val userId: Int, val displayName: String?, val coinBalance: Int)

@RestController
@RequestMapping("/api/auth")
class AuthController(
    private val userRepository: UserRepository,
    private val jwtUtil: JwtUtil,
    private val appleTokenVerifier: AppleTokenVerifier
) {

    @PostMapping("/google")
    fun googleAuth(@RequestBody request: GoogleAuthRequest): ResponseEntity<AuthResponse> {
        val decodedToken = FirebaseAuth.getInstance().verifyIdToken(request.idToken)
        val googleUid = decodedToken.uid
        val email = decodedToken.email
        val displayName = decodedToken.name ?: email?.substringBefore("@")

        val user = userRepository.findByGoogleUid(googleUid)?.let { existing ->
            existing.lastLoginAt = LocalDateTime.now()
            userRepository.save(existing)
        } ?: userRepository.save(
            User(
                googleUid = googleUid,
                email = email,
                displayName = displayName,
                lastLoginAt = LocalDateTime.now()
            )
        )

        val jwt = jwtUtil.issueToken(user.userId, user.email, user.displayName)
        return ResponseEntity.ok(AuthResponse(jwt, user.userId, user.displayName, user.coinBalance))
    }

    @PostMapping("/apple")
    fun appleAuth(@RequestBody request: AppleAuthRequest): ResponseEntity<AuthResponse> {
        val claims = appleTokenVerifier.verify(request.identityToken)
        val appleUid = claims.subject
        val email = claims["email"] as? String

        val user = userRepository.findByAppleUid(appleUid)?.let { existing ->
            existing.lastLoginAt = LocalDateTime.now()
            userRepository.save(existing)
        } ?: userRepository.save(
            User(
                appleUid = appleUid,
                email = email,
                displayName = email?.substringBefore("@"),
                lastLoginAt = LocalDateTime.now()
            )
        )

        val jwt = jwtUtil.issueToken(user.userId, user.email, user.displayName)
        return ResponseEntity.ok(AuthResponse(jwt, user.userId, user.displayName, user.coinBalance))
    }
}
