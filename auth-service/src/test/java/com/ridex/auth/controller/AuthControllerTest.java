package com.ridex.auth.controller;

import com.ridex.auth.dto.request.*;
import com.ridex.auth.dto.response.*;
import com.ridex.auth.service.AuthService;
import com.ridex.auth.utilities.UserRole;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuthControllerTest {

        @Mock
        private AuthService authService;

        @InjectMocks
        private AuthController authController;

        @Test
        void register_shouldReturnCreated() {
                RegisterRequest request = new RegisterRequest(
                                "Test Rider", "rider@test.com", "Password123",
                                "9876543210", UserRole.RIDER);

                RegisterResponse response = new RegisterResponse(
                                UUID.randomUUID(), "Test Rider",
                                "rider@test.com", UserRole.RIDER);

                when(authService.register(request)).thenReturn(response);

                var result = authController.register(request);

                assertEquals(HttpStatus.CREATED, result.getStatusCode());
                assertEquals(response, result.getBody());
                verify(authService).register(request);
        }

        @Test
        void login_shouldReturnOk() {
                LoginRequest request = new LoginRequest(
                                "rider@test.com", "Password123");

                LoginResponse response = new LoginResponse(
                                "access", "refresh", "Bearer", 900,
                                UUID.randomUUID(), UserRole.RIDER);

                when(authService.login(request)).thenReturn(response);

                var result = authController.login(request);

                assertEquals(HttpStatus.OK, result.getStatusCode());
                assertEquals(response, result.getBody());
                verify(authService).login(request);
        }

        @Test
        void refresh_shouldReturnOk() {
                RefreshTokenRequest request = new RefreshTokenRequest("refresh-token");

                RefreshTokenResponse response = new RefreshTokenResponse(
                                "new-access", "new-refresh", "Bearer", 900,
                                UUID.randomUUID(), UserRole.RIDER);

                when(authService.refresh("refresh-token"))
                                .thenReturn(response);

                var result = authController.refresh(request);

                assertEquals(HttpStatus.OK, result.getStatusCode());
                assertEquals(response, result.getBody());
                verify(authService).refresh("refresh-token");
        }

        @Test
        void logout_shouldReturnNoContent() {
                LogoutRequest request = new LogoutRequest("refresh-token");

                var result = authController.logout(request);

                assertEquals(HttpStatus.NO_CONTENT, result.getStatusCode());
                assertNull(result.getBody());
                verify(authService).logout("refresh-token");
        }

        @Test
        void register_shouldPropagateServiceException() {
                RegisterRequest request = new RegisterRequest(
                                "Test Rider", "rider@test.com", "Password123",
                                "9876543210", UserRole.RIDER);

                when(authService.register(request))
                                .thenThrow(new RuntimeException("Registration failed"));

                assertThrows(RuntimeException.class,
                                () -> authController.register(request));
        }
}
