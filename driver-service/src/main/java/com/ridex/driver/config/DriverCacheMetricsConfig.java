package com.ridex.driver.config;

import java.util.List;

import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.cache.metrics.CacheMetricsRegistrar;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class DriverCacheMetricsConfig {

    private static final List<String> DRIVER_CACHE_NAMES = List.of(
            "driverProfile",
            "driverRating",
            "vehicleDetails"
    );

    @Bean
    ApplicationRunner registerDriverProfileCacheMetrics(
            CacheManager cacheManager,
            CacheMetricsRegistrar cacheMetricsRegistrar
    ) {
        return args -> {
            for (String cacheName : DRIVER_CACHE_NAMES) {

                Cache cache = cacheManager.getCache(cacheName);

                if (cache != null) {
                    boolean registered =
                            cacheMetricsRegistrar.bindCacheToRegistry(cache);

                    if (!registered) {
                        throw new IllegalStateException(
                                "Could not register metrics for cache: "
                                        + cacheName
                        );
                    }
                }
            }
        };

    }
}