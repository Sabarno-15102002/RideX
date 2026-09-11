package com.ridex.location.service.util;

import java.util.Set;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Component
@RequiredArgsConstructor
@Slf4j
public class StaleDriverLocationCleanup {

    private static final String DRIVER_LOCATIONS_KEY =
            "ridex:drivers:locations";

    private static final String AVAILABLE_DRIVERS_KEY =
            "ridex:drivers:available";

    private static final String DRIVER_LAST_SEEN_KEY =
            "ridex:drivers:last-seen";

    private final RedisTemplate<String, String> redisTemplate;

    @Value("${ridex.location.stale-after-seconds:30}")
    private long staleAfterSeconds;

    @Scheduled(
            fixedDelayString = "${ridex.location.cleanup-interval-ms:10000}"
    )
    public void removeStaleDrivers() {

        long cutoff =
                System.currentTimeMillis()
                        - (staleAfterSeconds * 1000);

        Set<String> staleDrivers =
                redisTemplate.opsForZSet()
                        .rangeByScore(
                                DRIVER_LAST_SEEN_KEY,
                                0,
                                cutoff
                        );

        if (staleDrivers == null || staleDrivers.isEmpty()) {
            return;
        }

        for (String driverId : staleDrivers) {

            redisTemplate.opsForGeo()
                    .remove(
                            DRIVER_LOCATIONS_KEY,
                            driverId
                    );

            redisTemplate.opsForSet()
                    .remove(
                            AVAILABLE_DRIVERS_KEY,
                            driverId
                    );

            redisTemplate.opsForZSet()
                    .remove(
                            DRIVER_LAST_SEEN_KEY,
                            driverId
                    );

            log.info(
                    "Removed stale driver location: driverId={}",
                    driverId
            );
        }
    }
}