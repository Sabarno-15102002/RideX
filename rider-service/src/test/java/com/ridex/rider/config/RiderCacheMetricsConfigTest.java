package com.ridex.rider.config;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.cache.metrics.CacheMetricsRegistrar;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;

class RiderCacheMetricsConfigTest {

        private RiderCacheMetricsConfig config;
        private CacheManager cacheManager;
        private CacheMetricsRegistrar registrar;
        private Cache cache;

        @BeforeEach
        void setUp() {
                config = new RiderCacheMetricsConfig();
                cacheManager = mock(CacheManager.class);
                registrar = mock(CacheMetricsRegistrar.class);
                cache = mock(Cache.class);
        }

        @Test
        void runner_shouldRegisterMetricsWhenCacheExists()
                        throws Exception {

                when(cacheManager.getCache("riderProfile"))
                                .thenReturn(cache);

                when(registrar.bindCacheToRegistry(cache))
                                .thenReturn(true);

                ApplicationRunner runner = config.registerRiderProfileCacheMetrics(
                                cacheManager,
                                registrar);

                runner.run(mock(ApplicationArguments.class));

                verify(cacheManager).getCache("riderProfile");
                verify(registrar).bindCacheToRegistry(cache);
        }

        @Test
        void runner_shouldSkipRegistrationWhenCacheDoesNotExist()
                        throws Exception {

                when(cacheManager.getCache("riderProfile"))
                                .thenReturn(null);

                ApplicationRunner runner = config.registerRiderProfileCacheMetrics(
                                cacheManager,
                                registrar);

                assertDoesNotThrow(() -> runner.run(mock(ApplicationArguments.class)));

                verify(registrar, never()).bindCacheToRegistry(any());
        }

        @Test
        void runner_shouldThrowWhenMetricsRegistrationFails()
                        throws Exception {

                when(cacheManager.getCache("riderProfile"))
                                .thenReturn(cache);

                when(registrar.bindCacheToRegistry(cache))
                                .thenReturn(false);

                ApplicationRunner runner = config.registerRiderProfileCacheMetrics(
                                cacheManager,
                                registrar);

                assertThrows(
                                IllegalStateException.class,
                                () -> runner.run(mock(ApplicationArguments.class)));
        }
}
