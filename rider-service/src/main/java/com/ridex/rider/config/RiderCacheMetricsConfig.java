package com.ridex.rider.config;

import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.cache.metrics.CacheMetricsRegistrar;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RiderCacheMetricsConfig {

    @Bean
    ApplicationRunner registerRiderProfileCacheMetrics(
            CacheManager cacheManager,
            CacheMetricsRegistrar cacheMetricsRegistrar
    ) {
        return args -> {
            Cache riderProfileCache =
                    cacheManager.getCache("riderProfile");

            if (riderProfileCache != null) {
                boolean registered =
                        cacheMetricsRegistrar.bindCacheToRegistry(
                                riderProfileCache
                        );

                if (!registered) {
                    throw new IllegalStateException(
                            "Could not register Rider profile cache metrics"
                    );
                }
            }
        };
    }
}