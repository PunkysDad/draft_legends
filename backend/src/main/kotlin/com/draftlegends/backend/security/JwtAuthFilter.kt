package com.draftlegends.backend.security

import com.draftlegends.backend.auth.JwtUtil
import jakarta.servlet.FilterChain
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.stereotype.Component
import org.springframework.web.filter.OncePerRequestFilter

@Component
class JwtAuthFilter(
    private val jwtUtil: JwtUtil
) : OncePerRequestFilter() {

    override fun doFilterInternal(
        request: HttpServletRequest,
        response: HttpServletResponse,
        filterChain: FilterChain
    ) {
        val header = request.getHeader("Authorization")

        if (header != null && header.startsWith("Bearer ")) {
            val token = header.substring(7)
            try {
                val claims = jwtUtil.validateToken(token)
                val userId = claims.subject.toInt()
                val auth = UsernamePasswordAuthenticationToken(userId, null, emptyList())
                SecurityContextHolder.getContext().authentication = auth
            } catch (_: Exception) {
                // Invalid token — continue without authentication
            }
        }

        filterChain.doFilter(request, response)
    }
}
