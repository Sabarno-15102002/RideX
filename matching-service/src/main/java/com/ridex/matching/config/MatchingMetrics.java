package com.ridex.matching.config;

import java.util.concurrent.TimeUnit;

import org.springframework.stereotype.Component;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;

@Component
public class MatchingMetrics {

    private final Counter matchingAttempts;
    private final Counter matchingSuccesses;
    private final Counter matchingFailures;
    private final Timer matchingDuration;

    public MatchingMetrics(MeterRegistry meterRegistry) {
        this.matchingAttempts = Counter.builder("ridex.matching.attempts")
                .description("Number of trip matching attempts")
                .register(meterRegistry);

        this.matchingSuccesses = Counter.builder("ridex.matching.successes")
                .description("Number of successfully matched trips")
                .register(meterRegistry);

        this.matchingFailures = Counter.builder("ridex.matching.failures")
                .description("Number of failed matching attempts")
                .register(meterRegistry);

        this.matchingDuration = Timer.builder("ridex.matching.duration")
                .description("Duration of trip matching")
                .publishPercentiles(0.50, 0.95, 0.99)
                .register(meterRegistry);
    }

    public void recordMatchingAttempt() {
        matchingAttempts.increment();
    }

    public void recordMatchingSuccess() {
        matchingSuccesses.increment();
    }

    public void recordMatchingFailure() {
        matchingFailures.increment();
    }

    public void recordMatchingDuration(long duration) {
        matchingDuration.record(duration, TimeUnit.MILLISECONDS);
    }
}