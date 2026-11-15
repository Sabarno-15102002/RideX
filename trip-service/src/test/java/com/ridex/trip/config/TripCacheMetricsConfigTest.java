package com.ridex.trip.config;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.parallel.Execution;
import org.junit.jupiter.api.parallel.ExecutionMode;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.DefaultApplicationArguments;
import org.springframework.boot.cache.metrics.CacheMetricsRegistrar;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@Execution(ExecutionMode.SAME_THREAD)
class TripCacheMetricsConfigTest {

        private final TripCacheMetricsConfig config = new TripCacheMetricsConfig();

        @Test
        void registerTripCacheMetrics_shouldSkipMissingCaches()
                        throws Exception {

                CacheManager cacheManager = mock(CacheManager.class);
                CacheMetricsRegistrar registrar = mock(CacheMetricsRegistrar.class);

                when(cacheManager.getCache(anyString()))
                                .thenReturn(null);

                ApplicationRunner runner = config.registerTripCacheMetrics(
                                cacheManager,
                                registrar);

                ApplicationArguments args = new DefaultApplicationArguments(new String[0]);

                assertDoesNotThrow(() -> runner.run(args));

                verify(cacheManager, times(4)).getCache(anyString());
                verifyNoInteractions(registrar);
        }

        @Test
        void registerTripCacheMetrics_shouldThrowWhenRegistrationFails()
                        throws Exception {

                CacheManager cacheManager = mock(CacheManager.class);
                CacheMetricsRegistrar registrar = mock(CacheMetricsRegistrar.class);

                Cache cache = mock(Cache.class);

                when(cacheManager.getCache(anyString()))
                                .thenReturn(cache);

                when(registrar.bindCacheToRegistry(cache))
                                .thenReturn(false);

                ApplicationRunner runner = config.registerTripCacheMetrics(
                                cacheManager,
                                registrar);

                ApplicationArguments args = new DefaultApplicationArguments(new String[0]);

                assertThrows(
                                IllegalStateException.class,
                                () -> runner.run(args));

                verify(registrar, times(1))
                                .bindCacheToRegistry(cache);
        }
}
