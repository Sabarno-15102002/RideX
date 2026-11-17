package com.ridex.matching.config;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Date;
import java.util.UUID;

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

        private static final String SECRET = "01234567890123456789012345678901";

        @Autowired
        private MockMvc mockMvc;

        @Test
        void actuatorEndpoint_shouldBeAccessibleWithoutAuthentication()
                        throws Exception {

                mockMvc.perform(get("/actuator/test"))
                                .andDo(org.springframework.test.web.servlet.result.MockMvcResultHandlers.print())
                                .andExpect(status().isOk());
        }

        @Test
        void protectedEndpoint_shouldRejectUnauthenticatedRequest()
                        throws Exception {

                mockMvc.perform(get("/protected"))
                                .andExpect(status().isForbidden());
        }

        @Test
        void protectedEndpoint_shouldRejectInvalidJwt()
                        throws Exception {

                mockMvc.perform(
                                get("/protected")
                                                .header(
                                                                "Authorization",
                                                                "Bearer invalid-token"))
                                .andExpect(status().isForbidden());
        }

        @Test
        void protectedEndpoint_shouldAllowAuthenticatedUser()
                        throws Exception {

                String token = generateToken();

                mockMvc.perform(
                                get("/protected")
                                                .header("Authorization", "Bearer " + token))
                                .andDo(org.springframework.test.web.servlet.result.MockMvcResultHandlers.print())
                                .andExpect(status().isOk());
        }

        private String generateToken() {
                SecretKey key = Keys.hmacShaKeyFor(
                                SECRET.getBytes(StandardCharsets.UTF_8));

                return Jwts.builder()
                                .subject(UUID.randomUUID().toString())
                                .claim("role", "RIDER")
                                .issuedAt(Date.from(Instant.now().minusSeconds(60)))
                                .expiration(Date.from(Instant.now().plusSeconds(3600)))
                                .signWith(key)
                                .compact();
        }
}
