
package com.ridex.driver.config;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.cache.metrics.CacheMetricsRegistrar;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class DriverCacheMetricsConfigTest {

    private DriverCacheMetricsConfig config;
    private CacheManager cacheManager;
    private CacheMetricsRegistrar registrar;

    @BeforeEach
    void setUp() {
        config = new DriverCacheMetricsConfig();
        cacheManager = mock(CacheManager.class);
        registrar = mock(CacheMetricsRegistrar.class);
    }

    @Test
    void registerMetrics_shouldRegisterAllDriverCaches() throws Exception {
        Cache profile = mock(Cache.class);
        Cache rating = mock(Cache.class);
        Cache vehicle = mock(Cache.class);

        when(cacheManager.getCache("driverProfile")).thenReturn(profile);
        when(cacheManager.getCache("driverRating")).thenReturn(rating);
        when(cacheManager.getCache("vehicleDetails")).thenReturn(vehicle);

        when(registrar.bindCacheToRegistry(any(Cache.class)))
                .thenReturn(true);

        ApplicationRunner runner = config.registerDriverProfileCacheMetrics(
                cacheManager, registrar);

        runner.run(mock(ApplicationArguments.class));

        verify(registrar).bindCacheToRegistry(profile);
        verify(registrar).bindCacheToRegistry(rating);
        verify(registrar).bindCacheToRegistry(vehicle);
    }

    @Test
    void registerMetrics_whenCacheDoesNotExist_shouldSkipIt()
            throws Exception {

        when(cacheManager.getCache("driverProfile")).thenReturn(null);
        when(cacheManager.getCache("driverRating")).thenReturn(null);
        when(cacheManager.getCache("vehicleDetails")).thenReturn(null);

        ApplicationRunner runner = config.registerDriverProfileCacheMetrics(
                cacheManager, registrar);

        assertDoesNotThrow(() -> runner.run(mock(ApplicationArguments.class)));

        verifyNoInteractions(registrar);
    }

    @Test
    void registerMetrics_whenRegistrationFails_shouldThrowException()
            throws Exception {

        Cache profile = mock(Cache.class);

        when(cacheManager.getCache("driverProfile")).thenReturn(profile);
        when(registrar.bindCacheToRegistry(profile)).thenReturn(false);

        ApplicationRunner runner = config.registerDriverProfileCacheMetrics(
                cacheManager, registrar);

        IllegalStateException exception = assertThrows(
                IllegalStateException.class,
                () -> runner.run(mock(ApplicationArguments.class)));

        assertTrue(exception.getMessage()
                .contains("driverProfile"));
    }
}
