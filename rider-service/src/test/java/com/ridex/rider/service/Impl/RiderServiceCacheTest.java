package com.ridex.rider.service.Impl;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import java.math.BigDecimal;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.cache.concurrent.ConcurrentMapCacheManager;

import com.ridex.rider.dto.request.UpdateRiderRequest;
import com.ridex.rider.entity.Rider;
import com.ridex.rider.repository.RiderRepository;
import com.ridex.rider.service.RiderService;

@ExtendWith(SpringExtension.class)
@ContextConfiguration(classes = RiderServiceCacheTest.TestConfig.class)
class RiderServiceCacheTest {

    @Configuration
    @EnableCaching
    static class TestConfig {

        @Bean
        RiderRepository riderRepository() {
            return mock(RiderRepository.class);
        }

        @Bean
        RiderService riderService(RiderRepository repository) {
            return new RiderServiceImpl(repository);
        }

        @Bean
        CacheManager cacheManager() {
            return new ConcurrentMapCacheManager("riderProfile");
        }
    }

    @Autowired
    private RiderService riderService;

    @Autowired
    private RiderRepository riderRepository;

    @Autowired
    private CacheManager cacheManager;

    @BeforeEach
    void setUp() {
        reset(riderRepository);
        cacheManager.getCache("riderProfile").clear();
    }

    @Test
    void getMyProfile_shouldCacheSuccessfulResult() {
        UUID userId = UUID.randomUUID();

        Rider rider = createRider(userId, "John");

        when(riderRepository.findByUserId(userId))
                .thenReturn(Optional.of(rider));

        var firstResponse = riderService.getMyProfile(userId);
        var secondResponse = riderService.getMyProfile(userId);

        assertEquals(firstResponse, secondResponse);

        verify(riderRepository, times(1))
                .findByUserId(userId);
    }

    @Test
    void getMyProfile_shouldMaintainSeparateCacheEntriesPerRider() {
        UUID firstUserId = UUID.randomUUID();
        UUID secondUserId = UUID.randomUUID();

        when(riderRepository.findByUserId(firstUserId))
                .thenReturn(Optional.of(createRider(firstUserId, "John")));

        when(riderRepository.findByUserId(secondUserId))
                .thenReturn(Optional.of(createRider(secondUserId, "Jane")));

        var firstResponse = riderService.getMyProfile(firstUserId);
        var secondResponse = riderService.getMyProfile(secondUserId);

        assertEquals("John", firstResponse.name());
        assertEquals("Jane", secondResponse.name());

        verify(riderRepository).findByUserId(firstUserId);
        verify(riderRepository).findByUserId(secondUserId);
    }

    @Test
    void updateMyProfile_shouldEvictCachedProfile() {
        UUID userId = UUID.randomUUID();

        Rider rider = createRider(userId, "John");

        when(riderRepository.findByUserId(userId))
                .thenReturn(Optional.of(rider));

        when(riderRepository.save(any(Rider.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        var firstResponse = riderService.getMyProfile(userId);

        assertEquals("John", firstResponse.name());

        riderService.updateMyProfile(
                userId,
                new UpdateRiderRequest("John Updated")
        );

        var updatedResponse = riderService.getMyProfile(userId);

        assertEquals("John Updated", updatedResponse.name());

        verify(riderRepository, times(3))
                .findByUserId(userId);
    }

    @Test
    void getMyProfile_shouldNotCacheMissingRider() {
        UUID userId = UUID.randomUUID();

        when(riderRepository.findByUserId(userId))
                .thenReturn(Optional.empty());

        assertThrows(
                RuntimeException.class,
                () -> riderService.getMyProfile(userId)
        );

        assertThrows(
                RuntimeException.class,
                () -> riderService.getMyProfile(userId)
        );

        verify(riderRepository, times(2))
                .findByUserId(userId);
    }

    private Rider createRider(UUID userId, String name) {
        return Rider.builder()
                .id(UUID.randomUUID())
                .userId(userId)
                .name(name)
                .rating(BigDecimal.valueOf(5.00))
                .build();
    }
}
