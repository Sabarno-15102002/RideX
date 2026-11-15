package com.ridex.trip.dto.response;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;

import com.ridex.trip.dto.PaymentStatus;
import com.ridex.trip.dto.PaymentSummary;
import com.ridex.trip.dto.TripStatus;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class ResponseDtoTest {

    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper()
                .registerModule(new JavaTimeModule());
    }

    @Test
    void errorResponse_shouldSerializeAndDeserializeCorrectly()
            throws Exception {

        ErrorResponse original = new ErrorResponse(
                Instant.parse("2026-01-01T10:00:00Z"),
                404,
                "RESOURCE_NOT_FOUND",
                "Trip not found",
                "/api/v1/trips/123",
                "correlation-123");

        String json = objectMapper.writeValueAsString(original);

        ErrorResponse restored = objectMapper.readValue(json, ErrorResponse.class);

        assertEquals(original, restored);
        assertEquals(404, restored.status());
        assertEquals("RESOURCE_NOT_FOUND", restored.code());
        assertEquals("correlation-123", restored.correlationId());
    }

    @Test
    void fareQuoteResponse_shouldSerializeAndDeserializeCorrectly()
            throws Exception {

        FareQuoteResponse original = new FareQuoteResponse(
                UUID.randomUUID(),
                UUID.randomUUID(),
                new BigDecimal("30.00"),
                new BigDecimal("5.00"),
                new BigDecimal("15.00"),
                new BigDecimal("20.00"),
                new BigDecimal("10.00"),
                new BigDecimal("60.00"),
                "INR");

        String json = objectMapper.writeValueAsString(original);

        FareQuoteResponse restored = objectMapper.readValue(json, FareQuoteResponse.class);

        assertEquals(original, restored);
        assertEquals(new BigDecimal("60.00"), restored.totalFare());
        assertEquals("INR", restored.currency());
    }

    @Test
    void tripPaymentResponse_shouldSerializeAndDeserializeCorrectly()
            throws Exception {

        Instant paidAt = Instant.parse("2026-01-01T10:00:00Z");

        TripPaymentResponse original = new TripPaymentResponse(
                UUID.randomUUID(),
                UUID.randomUUID(),
                new BigDecimal("250.00"),
                "INR",
                PaymentStatus.SUCCESS,
                paidAt,
                null);

        String json = objectMapper.writeValueAsString(original);

        TripPaymentResponse restored = objectMapper.readValue(json, TripPaymentResponse.class);

        assertEquals(original, restored);
        assertEquals(PaymentStatus.SUCCESS, restored.status());
        assertEquals(paidAt, restored.paidAt());
        assertNull(restored.refundedAt());
    }

    @Test
    void tripPaymentResponse_shouldSupportRefundedPayment()
            throws Exception {

        Instant refundedAt = Instant.parse("2026-01-02T10:00:00Z");

        TripPaymentResponse original = new TripPaymentResponse(
                UUID.randomUUID(),
                UUID.randomUUID(),
                new BigDecimal("250.00"),
                "INR",
                PaymentStatus.REFUNDED,
                null,
                refundedAt);

        String json = objectMapper.writeValueAsString(original);

        TripPaymentResponse restored = objectMapper.readValue(json, TripPaymentResponse.class);

        assertEquals(original, restored);
        assertNull(restored.paidAt());
        assertEquals(refundedAt, restored.refundedAt());
    }

    @Test
    void tripResponse_shouldSerializeNestedPaymentSummary()
            throws Exception {

        PaymentSummary payment = new PaymentSummary(
                new BigDecimal("250.00"),
                "INR",
                PaymentStatus.SUCCESS);

        TripResponse original = new TripResponse(
                UUID.randomUUID(),
                UUID.randomUUID(),
                UUID.randomUUID(),
                12.9716,
                77.5946,
                12.9352,
                77.6245,
                TripStatus.TRIP_COMPLETED,
                Instant.parse("2026-01-01T10:00:00Z"),
                payment);

        String json = objectMapper.writeValueAsString(original);

        TripResponse restored = objectMapper.readValue(json, TripResponse.class);

        assertEquals(original, restored);
        assertEquals(payment, restored.payment());
        assertEquals(TripStatus.TRIP_COMPLETED, restored.status());
    }

    @Test
    void tripResponse_shouldSupportNullDriverAndPayment()
            throws Exception {

        TripResponse original = new TripResponse(
                UUID.randomUUID(),
                UUID.randomUUID(),
                null,
                12.9716,
                77.5946,
                12.9352,
                77.6245,
                TripStatus.REQUESTED,
                Instant.parse("2026-01-01T10:00:00Z"),
                null);

        String json = objectMapper.writeValueAsString(original);

        TripResponse restored = objectMapper.readValue(json, TripResponse.class);

        assertEquals(original, restored);
        assertNull(restored.driverId());
        assertNull(restored.payment());
    }

    @Test
    void tripStatusResponse_shouldSerializeAndDeserializeCorrectly()
            throws Exception {

        TripStatusResponse original = new TripStatusResponse(TripStatus.DRIVER_ASSIGNED);

        String json = objectMapper.writeValueAsString(original);

        TripStatusResponse restored = objectMapper.readValue(json, TripStatusResponse.class);

        assertEquals(original, restored);
        assertEquals(TripStatus.DRIVER_ASSIGNED, restored.status());
    }
}
