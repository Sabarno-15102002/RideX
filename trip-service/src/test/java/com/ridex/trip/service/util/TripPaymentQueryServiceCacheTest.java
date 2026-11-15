
package com.ridex.trip.service.util;

import com.ridex.trip.dto.PaymentStatus;
import com.ridex.trip.entity.TripPayment;
import com.ridex.trip.exception.IllegalOperationException;
import com.ridex.trip.repository.TripPaymentRepository;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.concurrent.ConcurrentMapCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(SpringExtension.class)
@ContextConfiguration(classes = TripPaymentQueryServiceCacheTest.TestConfig.class)
class TripPaymentQueryServiceCacheTest {

    @Configuration
    @EnableCaching
    @Import(TripPaymentQueryService.class)
    static class TestConfig {

        @Bean
        CacheManager cacheManager() {
            return new ConcurrentMapCacheManager("tripPayment");
        }

        @Bean
        TripPaymentRepository tripPaymentRepository() {
            return mock(TripPaymentRepository.class);
        }
    }

    @Autowired
    private TripPaymentQueryService service;

    @Autowired
    private TripPaymentRepository repository;

    @Autowired
    private CacheManager cacheManager;

    private UUID tripId;
    private UUID riderId;
    private UUID anotherRiderId;

    @BeforeEach
    void setUp() {
        reset(repository);

        cacheManager.getCache("tripPayment").clear();

        tripId = UUID.randomUUID();
        riderId = UUID.randomUUID();
        anotherRiderId = UUID.randomUUID();
    }

    @Test
    void getPaymentForTrip_shouldNotExposeCachedPaymentToAnotherRider() {

        UUID paymentId = UUID.randomUUID();

        TripPayment payment = new TripPayment(
                tripId,
                paymentId,
                riderId,
                new BigDecimal("250.00"),
                "INR",
                PaymentStatus.PENDING,
                Instant.now()
        );

        when(repository.findById(tripId))
                .thenReturn(Optional.of(payment));

        // First rider accesses their payment.
        assertDoesNotThrow(
                () -> service.getPaymentForTrip(tripId, riderId)
        );

        // Another rider must still undergo ownership validation.
        assertThrows(
                IllegalOperationException.class,
                () -> service.getPaymentForTrip(
                        tripId,
                        anotherRiderId
                )
        );

        // Both requests must reach the repository.
        verify(repository, times(2)).findById(tripId);
    }

    @Test
    void getPaymentForTrip_shouldReuseCacheForSameRider() {

        TripPayment payment = new TripPayment(
                tripId,
                UUID.randomUUID(),
                riderId,
                new BigDecimal("250.00"),
                "INR",
                PaymentStatus.PENDING,
                Instant.now()
        );

        when(repository.findById(tripId))
                .thenReturn(Optional.of(payment));

        service.getPaymentForTrip(tripId, riderId);
        service.getPaymentForTrip(tripId, riderId);

        // The second request should use the cache.
        verify(repository, times(1)).findById(tripId);
    }
}
