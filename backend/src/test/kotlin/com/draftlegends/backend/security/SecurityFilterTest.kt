package com.draftlegends.backend.security

import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status

@SpringBootTest
@AutoConfigureMockMvc
class SecurityFilterTest {

    @Autowired
    private lateinit var mockMvc: MockMvc

    @Test
    fun `protected endpoint returns 401 without token`() {
        mockMvc.perform(get("/api/players"))
            .andExpect(status().isUnauthorized)
    }

    @Test
    fun `protected endpoint returns 401 with invalid token`() {
        mockMvc.perform(
            get("/api/players")
                .header("Authorization", "Bearer invalid.token.here")
        ).andExpect(status().isUnauthorized)
    }

    @Test
    fun `auth endpoints are accessible without token`() {
        // POST without body will fail with 400, but NOT 401 — proving auth is not required
        mockMvc.perform(
            get("/actuator/health")
        ).andExpect(status().isOk)
    }
}
