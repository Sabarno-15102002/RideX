
package com.ridex.pricing.config;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = TestSecurityController.class)
@Import({
        SecurityConfig.class,
        JwtAuthenticationFilter.class
})
@TestPropertySource(properties = {
        "jwt.secret=01234567890123456789012345678901"
})
class SecurityConfigTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void shouldRejectUnauthenticatedProtectedEndpoint() throws Exception {
        mockMvc.perform(get("/test/protected"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void shouldPermitInternalEndpoint() throws Exception {
        mockMvc.perform(get("/api/v1/internal/test"))
                .andExpect(status().isOk());
    }

    @Test
    void shouldPermitActuatorEndpoint() throws Exception {
        mockMvc.perform(get("/actuator/test"))
                .andExpect(status().isOk());
    }
}
