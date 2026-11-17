package com.ridex.rider.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.ridex.rider.dto.AuthenticatedUser;
import com.ridex.rider.dto.request.CreateSavedLocationRequest;
import com.ridex.rider.dto.response.SavedLocationResponse;
import com.ridex.rider.exception.GlobalExceptionHandler;
import com.ridex.rider.exception.SavedLocationNotFoundException;
import com.ridex.rider.service.SavedLocationService;

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
class SavedLocationControllerTest {

    @Mock
    private SavedLocationService savedLocationService;

    @InjectMocks
    private SavedLocationController savedLocationController;

    private MockMvc mockMvc;
    private ObjectMapper objectMapper;
    private LocalValidatorFactoryBean validator;

    private final UUID userId = UUID.randomUUID();
    private final UUID locationId = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper()
                .registerModule(new JavaTimeModule());

        validator = new LocalValidatorFactoryBean();
        validator.afterPropertiesSet();

        mockMvc = MockMvcBuilders
                .standaloneSetup(savedLocationController)
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
    void create_shouldReturnCreatedLocation() throws Exception {
        CreateSavedLocationRequest request = new CreateSavedLocationRequest(
                "Home", 12.9716, 77.5946, "Bengaluru");

        SavedLocationResponse response = new SavedLocationResponse(
                locationId,
                "Home",
                12.9716,
                77.5946,
                "Bengaluru",
                Instant.parse("2026-01-01T10:00:00Z"));

        when(savedLocationService.create(eq(userId), any(CreateSavedLocationRequest.class)))
                .thenReturn(response);

        mockMvc.perform(post("/api/v1/riders/me/locations")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(locationId.toString()))
                .andExpect(jsonPath("$.label").value("Home"))
                .andExpect(jsonPath("$.latitude").value(12.9716))
                .andExpect(jsonPath("$.longitude").value(77.5946))
                .andExpect(jsonPath("$.address").value("Bengaluru"));

        verify(savedLocationService).create(userId, request);
    }

    @Test
    void create_whenLabelIsBlank_shouldReturnBadRequest() throws Exception {
        mockMvc.perform(post("/api/v1/riders/me/locations")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {
                          "label": " ",
                          "latitude": 12.9716,
                          "longitude": 77.5946,
                          "address": "Bengaluru"
                        }
                        """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"));

        verifyNoInteractions(savedLocationService);
    }

    @Test
    void create_whenLatitudeIsOutOfRange_shouldReturnBadRequest() throws Exception {
        mockMvc.perform(post("/api/v1/riders/me/locations")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {
                          "label": "Home",
                          "latitude": 91.0,
                          "longitude": 77.5946,
                          "address": "Bengaluru"
                        }
                        """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"));

        verifyNoInteractions(savedLocationService);
    }

    @Test
    void create_whenLongitudeIsOutOfRange_shouldReturnBadRequest() throws Exception {
        mockMvc.perform(post("/api/v1/riders/me/locations")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {
                          "label": "Home",
                          "latitude": 12.9716,
                          "longitude": 181.0,
                          "address": "Bengaluru"
                        }
                        """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"));

        verifyNoInteractions(savedLocationService);
    }

    @Test
    void getAll_shouldReturnSavedLocations() throws Exception {
        List<SavedLocationResponse> locations = List.of(
                new SavedLocationResponse(
                        locationId, "Home", 12.9716, 77.5946,
                        "Bengaluru", Instant.parse("2026-01-01T10:00:00Z")));

        when(savedLocationService.getAll(userId)).thenReturn(locations);

        mockMvc.perform(get("/api/v1/riders/me/locations"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].id").value(locationId.toString()))
                .andExpect(jsonPath("$[0].label").value("Home"));

        verify(savedLocationService).getAll(userId);
    }

    @Test
    void getAll_whenNoLocations_shouldReturnEmptyList() throws Exception {
        when(savedLocationService.getAll(userId)).thenReturn(List.of());

        mockMvc.perform(get("/api/v1/riders/me/locations"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(0));

        verify(savedLocationService).getAll(userId);
    }

    @Test
    void delete_shouldReturnNoContent() throws Exception {
        doNothing().when(savedLocationService).delete(userId, locationId);

        mockMvc.perform(delete("/api/v1/riders/me/locations/{locationId}", locationId))
                .andExpect(status().isNoContent());

        verify(savedLocationService).delete(userId, locationId);
    }

    @Test
    void delete_whenLocationDoesNotExist_shouldReturnNotFound() throws Exception {
        doThrow(
                new SavedLocationNotFoundException("Saved location not found"))
                .when(savedLocationService).delete(userId, locationId);

        mockMvc.perform(delete("/api/v1/riders/me/locations/{locationId}", locationId))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("SAVED_LOCATION_NOT_FOUND"))
                .andExpect(jsonPath("$.message").value("Saved location not found"));
    }
}