package com.ridex.matching.config;

import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class MatchingMetricsTest {

    private SimpleMeterRegistry meterRegistry;
    private MatchingMetrics matchingMetrics;

    @BeforeEach
    void setUp() {
        meterRegistry = new SimpleMeterRegistry();
        matchingMetrics = new MatchingMetrics(meterRegistry);
    }

    @AfterEach
    void tearDown() {
        meterRegistry.close();
    }

    @Test
    void shouldRecordMatchingAttempt() {
        matchingMetrics.recordMatchingAttempt();

        assertEquals(
                1.0,
                meterRegistry.get("ridex.matching.attempts")
                        .counter()
                        .count());
    }

    @Test
    void shouldRecordMatchingSuccess() {
        matchingMetrics.recordMatchingSuccess();

        assertEquals(
                1.0,
                meterRegistry.get("ridex.matching.successes")
                        .counter()
                        .count());
    }

    @Test
    void shouldRecordMatchingFailure() {
        matchingMetrics.recordMatchingFailure();

        assertEquals(
                1.0,
                meterRegistry.get("ridex.matching.failures")
                        .counter()
                        .count());
    }

    @Test
    void shouldRecordMatchingDurationInMilliseconds() {
        matchingMetrics.recordMatchingDuration(250);

        assertEquals(
                1,
                meterRegistry.get("ridex.matching.duration")
                        .timer()
                        .count());

        assertEquals(
                250.0,
                meterRegistry.get("ridex.matching.duration")
                        .timer()
                        .totalTime(java.util.concurrent.TimeUnit.MILLISECONDS),
                0.001);
    }
}
