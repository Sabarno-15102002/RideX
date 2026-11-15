package com.ridex.auth.service.impl;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import com.ridex.auth.dto.request.LoginRequest;
import com.ridex.auth.dto.request.RegisterRequest;
import com.ridex.auth.dto.response.LoginResponse;
import com.ridex.auth.dto.response.RefreshTokenResponse;
import com.ridex.auth.dto.response.RegisterResponse;
import com.ridex.auth.entity.RefreshToken;
import com.ridex.auth.entity.User;
import com.ridex.auth.event.UserRegisteredEvent;
import com.ridex.auth.exception.InvalidCredentialsException;
import com.ridex.auth.exception.InvalidRefreshTokenException;
import com.ridex.auth.exception.ResourceAlreadyExistsException;
import com.ridex.auth.repository.RefreshTokenRepository;
import com.ridex.auth.repository.UserRepository;
import com.ridex.auth.service.RefreshTokenService;
import com.ridex.auth.service.util.GeneratedRefreshToken;
import com.ridex.auth.service.util.OutboxEventService;
import com.ridex.auth.service.util.TokenHashUtil;
import com.ridex.auth.config.JwtProvider;
import com.ridex.auth.utilities.UserRole;
import com.ridex.auth.utilities.UserStatus;

@ExtendWith(MockitoExtension.class)
class AuthServiceImplTest {

        @Mock
        private UserRepository userRepository;

        @Mock
        private RefreshTokenRepository refreshTokenRepository;

        @Mock
        private OutboxEventService outboxEventService;

        @Mock
        private PasswordEncoder passwordEncoder;

        @Mock
        private JwtProvider jwtProvider;

        @Mock
        private RefreshTokenService refreshTokenService;

        @Mock
        private TokenHashUtil tokenHashUtil;

        @InjectMocks
        private AuthServiceImpl authService;

        private RegisterRequest riderRequest;
        private RegisterRequest driverRequest;
        private RegisterRequest adminRequest;

        @BeforeEach
        void setUp() {
                riderRequest = new RegisterRequest(
                                "John Rider",
                                "rider@example.com",
                                "password123",
                                "9876543210",
                                UserRole.RIDER);

                driverRequest = new RegisterRequest(
                                "John Driver",
                                "driver@example.com",
                                "password123",
                                "9876543211",
                                UserRole.DRIVER);

                adminRequest = new RegisterRequest(
                                "John Admin",
                                "admin@example.com",
                                "password123",
                                "9876543212",
                                UserRole.ADMIN);
        }

        @Test
        void register_shouldSuccessfullyRegisterRider() {

                when(userRepository.existsByEmail(riderRequest.email()))
                                .thenReturn(false);

                when(userRepository.existsByPhone(riderRequest.phone()))
                                .thenReturn(false);

                when(passwordEncoder.encode(riderRequest.password()))
                                .thenReturn("encoded-password");

                RegisterResponse response = authService.register(riderRequest);

                assertNotNull(response);
                assertEquals("John Rider", response.name());
                assertEquals("rider@example.com", response.email());
                assertEquals(UserRole.RIDER, response.role());

                ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);

                verify(userRepository).save(userCaptor.capture());

                User savedUser = userCaptor.getValue();

                assertNotNull(savedUser.getId());
                assertEquals("John Rider", savedUser.getName());
                assertEquals("rider@example.com", savedUser.getEmail());
                assertEquals("9876543210", savedUser.getPhone());
                assertEquals("encoded-password", savedUser.getPasswordHash());
                assertEquals(UserRole.RIDER, savedUser.getRole());
                assertEquals(UserStatus.ACTIVE, savedUser.getStatus());
                assertNotNull(savedUser.getCreatedAt());
                assertNotNull(savedUser.getUpdatedAt());

                verify(passwordEncoder).encode(riderRequest.password());
                verify(outboxEventService)
                                .saveUserRegisteredEvent(any(UserRegisteredEvent.class));
        }

        @Test
        void register_shouldSuccessfullyRegisterDriver() {

                when(userRepository.existsByEmail(driverRequest.email()))
                                .thenReturn(false);

                when(userRepository.existsByPhone(driverRequest.phone()))
                                .thenReturn(false);

                when(passwordEncoder.encode(driverRequest.password()))
                                .thenReturn("encoded-password");

                RegisterResponse response = authService.register(driverRequest);

                assertNotNull(response);
                assertEquals("John Driver", response.name());
                assertEquals("driver@example.com", response.email());
                assertEquals(UserRole.DRIVER, response.role());

                ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);

                verify(userRepository).save(userCaptor.capture());

                User savedUser = userCaptor.getValue();

                assertEquals(UserRole.DRIVER, savedUser.getRole());
                assertEquals(UserStatus.ACTIVE, savedUser.getStatus());
                assertEquals("encoded-password", savedUser.getPasswordHash());

                verify(outboxEventService)
                                .saveUserRegisteredEvent(any(UserRegisteredEvent.class));
        }

        @Test
        void register_shouldRejectAdminRegistration() {

                assertThrows(
                                IllegalArgumentException.class,
                                () -> authService.register(adminRequest));

                verify(userRepository, never()).save(any(User.class));

                verify(outboxEventService, never())
                                .saveUserRegisteredEvent(any(UserRegisteredEvent.class));

                verify(passwordEncoder, never()).encode(any());
        }

        @Test
        void register_shouldRejectDuplicateEmail() {

                when(userRepository.existsByEmail(riderRequest.email()))
                                .thenReturn(true);

                assertThrows(
                                ResourceAlreadyExistsException.class,
                                () -> authService.register(riderRequest));

                verify(userRepository, never()).save(any(User.class));

                verify(outboxEventService, never())
                                .saveUserRegisteredEvent(any(UserRegisteredEvent.class));

                verify(passwordEncoder, never()).encode(any());
        }

        @Test
        void register_shouldRejectDuplicatePhone() {

                when(userRepository.existsByEmail(riderRequest.email()))
                                .thenReturn(false);

                when(userRepository.existsByPhone(riderRequest.phone()))
                                .thenReturn(true);

                assertThrows(
                                ResourceAlreadyExistsException.class,
                                () -> authService.register(riderRequest));

                verify(userRepository, never()).save(any(User.class));

                verify(outboxEventService, never())
                                .saveUserRegisteredEvent(any(UserRegisteredEvent.class));

                verify(passwordEncoder, never()).encode(any());
        }

        @Test
        void register_shouldAllowRegistrationWithoutPhone() {

                RegisterRequest request = new RegisterRequest(
                                "Jane Rider",
                                "jane@example.com",
                                "password123",
                                null,
                                UserRole.RIDER);

                when(userRepository.existsByEmail(request.email()))
                                .thenReturn(false);

                when(passwordEncoder.encode(request.password()))
                                .thenReturn("encoded-password");

                RegisterResponse response = authService.register(request);

                assertNotNull(response);
                assertEquals(UserRole.RIDER, response.role());

                verify(userRepository, never()).existsByPhone(any());

                ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);

                verify(userRepository).save(userCaptor.capture());

                assertNull(userCaptor.getValue().getPhone());
        }

        @Test
        void register_shouldPublishUserRegisteredEventWithCorrectRole() {

                when(userRepository.existsByEmail(driverRequest.email()))
                                .thenReturn(false);

                when(userRepository.existsByPhone(driverRequest.phone()))
                                .thenReturn(false);

                when(passwordEncoder.encode(driverRequest.password()))
                                .thenReturn("encoded-password");

                authService.register(driverRequest);

                ArgumentCaptor<UserRegisteredEvent> eventCaptor = ArgumentCaptor.forClass(UserRegisteredEvent.class);

                verify(outboxEventService)
                                .saveUserRegisteredEvent(eventCaptor.capture());

                UserRegisteredEvent event = eventCaptor.getValue();

                assertNotNull(event.eventId());
                assertNotNull(event.userId());
                assertEquals("John Driver", event.name());
                assertEquals("DRIVER", event.role());
                assertNotNull(event.registeredAt());
        }

        @Test
        void register_shouldDefaultToRiderWhenRoleIsNull() {
                RegisterRequest request = new RegisterRequest(
                                "Jane Rider",
                                "jane-default@example.com",
                                "password123",
                                null,
                                null);

                when(userRepository.existsByEmail(request.email()))
                                .thenReturn(false);

                when(passwordEncoder.encode(request.password()))
                                .thenReturn("encoded-password");

                RegisterResponse response = authService.register(request);

                assertNotNull(response);
                assertEquals(UserRole.RIDER, response.role());

                ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);

                verify(userRepository).save(userCaptor.capture());

                assertEquals(UserRole.RIDER, userCaptor.getValue().getRole());

                verify(userRepository, never()).existsByPhone(any());
        }

        private User createActiveUser() {
                return User.builder()
                                .id(UUID.randomUUID())
                                .name("John Rider")
                                .email("rider@example.com")
                                .phone("9876543210")
                                .passwordHash("encoded-password")
                                .role(UserRole.RIDER)
                                .status(UserStatus.ACTIVE)
                                .build();
        }

        private GeneratedRefreshToken generatedToken(String rawToken) {
                return new GeneratedRefreshToken(
                                rawToken,
                                RefreshToken.builder()
                                                .userId(UUID.randomUUID())
                                                .tokenHash("hashed-token")
                                                .familyId(UUID.randomUUID())
                                                .expiresAt(java.time.Instant.now().plusSeconds(3600))
                                                .revoked(false)
                                                .createdAt(java.time.Instant.now())
                                                .build());
        }

        @Test
        void login_shouldSuccessfullyLoginUser() {
                User user = createActiveUser();

                LoginRequest request = new LoginRequest(
                                user.getEmail(),
                                "password123");

                when(userRepository.findByEmail(user.getEmail()))
                                .thenReturn(Optional.of(user));
                when(passwordEncoder.matches("password123", "encoded-password"))
                                .thenReturn(true);
                when(jwtProvider.generateToken(user))
                                .thenReturn("access-token");
                when(refreshTokenService.create(user))
                                .thenReturn(generatedToken("refresh-token"));
                when(jwtProvider.getExpirationMs())
                                .thenReturn(900000L);

                LoginResponse response = authService.login(request);

                assertNotNull(response);
                assertEquals("access-token", response.accessToken());
                assertEquals("refresh-token", response.refreshToken());
                assertEquals("Bearer", response.tokenType());
                assertEquals(900000L, response.expiresIn());
                assertEquals(user.getId(), response.userId());
                assertEquals(UserRole.RIDER, response.role());

                verify(refreshTokenService).create(user);
                verify(jwtProvider).generateToken(user);
        }

        @Test
        void login_shouldRejectUnknownEmail() {
                LoginRequest request = new LoginRequest(
                                "unknown@example.com",
                                "password123");

                when(userRepository.findByEmail(request.email()))
                                .thenReturn(Optional.empty());

                assertThrows(
                                InvalidCredentialsException.class,
                                () -> authService.login(request));

                verify(passwordEncoder, never()).matches(any(), any());
                verify(jwtProvider, never()).generateToken(any(User.class));
                verify(refreshTokenService, never()).create(any(User.class));
        }

        @Test
        void login_shouldRejectIncorrectPassword() {
                User user = createActiveUser();

                LoginRequest request = new LoginRequest(
                                user.getEmail(),
                                "wrong-password");

                when(userRepository.findByEmail(user.getEmail()))
                                .thenReturn(Optional.of(user));
                when(passwordEncoder.matches("wrong-password", "encoded-password"))
                                .thenReturn(false);

                assertThrows(
                                InvalidCredentialsException.class,
                                () -> authService.login(request));

                verify(jwtProvider, never()).generateToken(any(User.class));
                verify(refreshTokenService, never()).create(any(User.class));
        }

        @Test
        void login_shouldRejectInactiveUser() {
                User user = createActiveUser();
                user.setStatus(UserStatus.INACTIVE);

                LoginRequest request = new LoginRequest(
                                user.getEmail(),
                                "password123");

                when(userRepository.findByEmail(user.getEmail()))
                                .thenReturn(Optional.of(user));
                when(passwordEncoder.matches("password123", "encoded-password"))
                                .thenReturn(true);

                assertThrows(
                                InvalidCredentialsException.class,
                                () -> authService.login(request));

                verify(jwtProvider, never()).generateToken(any(User.class));
                verify(refreshTokenService, never()).create(any(User.class));
        }

        @Test
        void refresh_shouldSuccessfullyRotateToken() {
                User user = createActiveUser();

                RefreshToken currentToken = RefreshToken.builder()
                                .id(UUID.randomUUID())
                                .userId(user.getId())
                                .tokenHash("hashed-refresh-token")
                                .familyId(UUID.randomUUID())
                                .revoked(false)
                                .expiresAt(java.time.Instant.now().plusSeconds(3600))
                                .createdAt(java.time.Instant.now())
                                .build();

                when(tokenHashUtil.hash("old-refresh-token"))
                                .thenReturn("hashed-refresh-token");

                when(refreshTokenRepository.findByTokenHash("hashed-refresh-token"))
                                .thenReturn(Optional.of(currentToken));

                when(userRepository.findById(user.getId()))
                                .thenReturn(Optional.of(user));

                when(refreshTokenService.rotate(currentToken, user))
                                .thenReturn(generatedToken("new-refresh-token"));

                when(jwtProvider.generateToken(user))
                                .thenReturn("new-access-token");

                when(jwtProvider.getExpirationMs())
                                .thenReturn(900000L);

                RefreshTokenResponse response = authService.refresh("old-refresh-token");

                assertNotNull(response);
                assertEquals("new-access-token", response.accessToken());
                assertEquals("new-refresh-token", response.refreshToken());
                assertEquals("Bearer", response.tokenType());
                assertEquals(900000L, response.expiresIn());
                assertEquals(user.getId(), response.userId());
                assertEquals(UserRole.RIDER, response.role());

                verify(refreshTokenService).rotate(currentToken, user);
        }

        @Test
        void refresh_shouldRejectUnknownRefreshToken() {
                when(tokenHashUtil.hash("invalid-token"))
                                .thenReturn("hashed-invalid-token");

                when(refreshTokenRepository.findByTokenHash("hashed-invalid-token"))
                                .thenReturn(Optional.empty());

                assertThrows(
                                InvalidRefreshTokenException.class,
                                () -> authService.refresh("invalid-token"));

                verify(userRepository, never()).findById(any());
                verify(refreshTokenService, never()).rotate(any(), any());
        }

        @Test
        void refresh_shouldRejectWhenAssociatedUserDoesNotExist() {
                UUID userId = UUID.randomUUID();

                RefreshToken currentToken = RefreshToken.builder()
                                .id(UUID.randomUUID())
                                .userId(userId)
                                .tokenHash("hashed-refresh-token")
                                .familyId(UUID.randomUUID())
                                .revoked(false)
                                .expiresAt(java.time.Instant.now().plusSeconds(3600))
                                .createdAt(java.time.Instant.now())
                                .build();

                when(tokenHashUtil.hash("refresh-token"))
                                .thenReturn("hashed-refresh-token");

                when(refreshTokenRepository.findByTokenHash("hashed-refresh-token"))
                                .thenReturn(Optional.of(currentToken));

                when(userRepository.findById(userId))
                                .thenReturn(Optional.empty());

                assertThrows(
                                InvalidRefreshTokenException.class,
                                () -> authService.refresh("refresh-token"));

                verify(refreshTokenService, never()).rotate(any(), any());
        }

        @Test
        void refresh_shouldRejectInactiveUser() {
                User user = createActiveUser();
                user.setStatus(UserStatus.BLOCKED);

                RefreshToken currentToken = RefreshToken.builder()
                                .id(UUID.randomUUID())
                                .userId(user.getId())
                                .tokenHash("hashed-refresh-token")
                                .familyId(UUID.randomUUID())
                                .revoked(false)
                                .expiresAt(java.time.Instant.now().plusSeconds(3600))
                                .createdAt(java.time.Instant.now())
                                .build();

                when(tokenHashUtil.hash("refresh-token"))
                                .thenReturn("hashed-refresh-token");

                when(refreshTokenRepository.findByTokenHash("hashed-refresh-token"))
                                .thenReturn(Optional.of(currentToken));

                when(userRepository.findById(user.getId()))
                                .thenReturn(Optional.of(user));

                assertThrows(
                                InvalidRefreshTokenException.class,
                                () -> authService.refresh("refresh-token"));

                verify(refreshTokenService, never()).rotate(any(), any());
                verify(jwtProvider, never()).generateToken(any(User.class));
        }

        @Test
        void logout_shouldRevokeRefreshToken() {
                authService.logout("refresh-token");

                verify(refreshTokenService).revoke("refresh-token");
        }

        @Test
        void logoutAll_shouldRevokeAllUserTokens() {
                UUID userId = UUID.randomUUID();

                authService.logoutAll(userId);

                verify(refreshTokenService).revokeAll(userId);
        }
}
