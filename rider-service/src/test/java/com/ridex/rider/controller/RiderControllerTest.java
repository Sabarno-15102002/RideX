package com.ridex.rider.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ridex.rider.dto.AuthenticatedUser;
import com.ridex.rider.dto.request.UpdateRiderRequest;
import com.ridex.rider.dto.response.RiderResponse;
import com.ridex.rider.exception.GlobalExceptionHandler;
import com.ridex.rider.exception.RiderNotFoundException;
import com.ridex.rider.service.RiderService;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.validation.beanvalidation.LocalValidatorFactoryBean;

@ExtendWith(MockitoExtension.class)
class RiderControllerTest {

    @Mock
    private RiderService riderService;

    @InjectMocks
    private RiderController riderController;

    private MockMvc mockMvc;
    private ObjectMapper objectMapper;
    private LocalValidatorFactoryBean validator;

    private final UUID userId = UUID.randomUUID();
    private final UUID riderId = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();

        validator = new LocalValidatorFactoryBean();
        validator.afterPropertiesSet();

        mockMvc = MockMvcBuilders
                .standaloneSetup(riderController)
                .setControllerAdvice(new GlobalExceptionHandler())
                .setValidator(validator)
                .build();

        var authentication = new UsernamePasswordAuthenticationToken(
                new AuthenticatedUser(userId, "RIDER"),
                null,
                List.of());

        SecurityContextHolder.getContext().setAuthentication(authentication);
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
        validator.close();
    }

    @Test
    void getMyProfile_shouldReturnProfile() throws Exception {
        RiderResponse response = new RiderResponse(
                riderId, userId, "John", new BigDecimal("5.00"));

        when(riderService.getMyProfile(userId)).thenReturn(response);

        mockMvc.perform(get("/api/v1/riders/me"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(riderId.toString()))
                .andExpect(jsonPath("$.userId").value(userId.toString()))
                .andExpect(jsonPath("$.name").value("John"))
                .andExpect(jsonPath("$.rating").value(5.00));

        verify(riderService).getMyProfile(userId);
    }

    @Test
    void getMyProfile_whenRiderDoesNotExist_shouldReturnNotFound() throws Exception {
        when(riderService.getMyProfile(userId))
                .thenThrow(new RiderNotFoundException("Rider not found"));

        mockMvc.perform(get("/api/v1/riders/me"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("RIDER_NOT_FOUND"))
                .andExpect(jsonPath("$.message").value("Rider not found"))
                .andExpect(jsonPath("$.path").value("/api/v1/riders/me"));
    }

    @Test
    void updateMyProfile_shouldUpdateAndReturnProfile() throws Exception {
        UpdateRiderRequest request = new UpdateRiderRequest("John Updated");
        RiderResponse response = new RiderResponse(
                riderId, userId, "John Updated", new BigDecimal("5.00"));

        when(riderService.updateMyProfile(eq(userId), any(UpdateRiderRequest.class)))
                .thenReturn(response);

        mockMvc.perform(put("/api/v1/riders/me")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("John Updated"));

        verify(riderService).updateMyProfile(userId, request);
    }

    @Test
    void updateMyProfile_whenNameIsBlank_shouldReturnBadRequest() throws Exception {
        mockMvc.perform(put("/api/v1/riders/me")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {"name": " "}
                        """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"));

        verifyNoInteractions(riderService);
    }

    @Test
    void updateMyProfile_whenNameExceeds150Characters_shouldReturnBadRequest() throws Exception {
        String longName = "a".repeat(151);

        mockMvc.perform(put("/api/v1/riders/me")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(
                        new UpdateRiderRequest(longName))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"));

        verifyNoInteractions(riderService);
    }
}