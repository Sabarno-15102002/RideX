
package com.ridex.auth.config;

import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;
import com.ridex.auth.controller.AuthController;
import org.springframework.context.annotation.FilterType;
import org.springframework.context.annotation.ComponentScan;
import com.ridex.auth.service.AuthService;

import io.jsonwebtoken.Claims;

@WebMvcTest(
    controllers = SecurityConfigTest.TestController.class,
    excludeFilters = @ComponentScan.Filter(
        type = FilterType.ASSIGNABLE_TYPE,
        classes = AuthController.class
    )
)
@Import({
        SecurityConfig.class,
        JwtAuthenticationFilter.class,
        SecurityConfigTest.TestController.class
})
class SecurityConfigTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private JwtProvider jwtProvider;

    @MockitoBean
    private AuthService authService;

    private UUID userId;

    @BeforeEach
    void setUp() {
        userId = UUID.randomUUID();
        SecurityContextHolder.clearContext();
    }

    @Test
    void registerEndpoint_shouldBePublic() throws Exception {

        mockMvc.perform(post("/api/v1/auth/register"))
                .andExpect(status().isOk());
    }

    @Test
    void loginEndpoint_shouldBePublic() throws Exception {

        mockMvc.perform(post("/api/v1/auth/login"))
                .andExpect(status().isOk());
    }

    @Test
    void refreshEndpoint_shouldBePublic() throws Exception {

        mockMvc.perform(post("/api/v1/auth/refresh"))
                .andExpect(status().isOk());
    }

    @Test
    void logoutEndpoint_shouldBePublic() throws Exception {

        mockMvc.perform(post("/api/v1/auth/logout"))
                .andExpect(status().isOk());
    }

    @Test
    void actuatorEndpoint_shouldBePublic() throws Exception {

        mockMvc.perform(get("/actuator/test"))
                .andExpect(status().isOk());
    }

    @Test
    void protectedPostEndpoint_shouldNotRequireCsrfToken()
            throws Exception {

        mockValidJwt();

        mockMvc.perform(
                post("/protected/action")
                        .header("Authorization", "Bearer valid-token")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk());
    }

    @Test
    void securityConfiguration_shouldNotCreateHttpSession()
            throws Exception {

        mockMvc.perform(post("/api/v1/auth/login"))
                .andExpect(status().isOk())
                .andExpect(result -> assertNull(result.getRequest().getSession(false)));
    }

    private void mockValidJwt() {

        Claims claims = mock(Claims.class);

        when(jwtProvider.isValid("valid-token"))
                .thenReturn(true);

        when(jwtProvider.extractClaims("valid-token"))
                .thenReturn(claims);

        when(claims.getSubject())
                .thenReturn(userId.toString());

        when(claims.get("role", String.class))
                .thenReturn("RIDER");
    }

    @RestController
    static class TestController {

        @PostMapping("/api/v1/auth/register")
        String register() {
            return "registered";
        }

        @PostMapping("/api/v1/auth/login")
        String login() {
            return "logged-in";
        }

        @PostMapping("/api/v1/auth/refresh")
        String refresh() {
            return "refreshed";
        }

        @PostMapping("/api/v1/auth/logout")
        String logout() {
            return "logged-out";
        }

        @GetMapping("/actuator/test")
        String actuator() {
            return "actuator";
        }

        @PostMapping("/protected/action")
        String protectedAction() {
            return "success";
        }
    }
}
