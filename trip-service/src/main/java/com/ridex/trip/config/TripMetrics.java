package com.ridex.trip.config;

import java.time.Duration;

import org.springframework.stereotype.Component;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;

@Component
public class TripMetrics {

    private final Counter tripsRequested;
    private final Counter tripsCompleted;
    private final Counter tripsCancelled;
    private final Timer tripDuration;

    public TripMetrics(MeterRegistry registry) {
        this.tripsRequested = Counter.builder("ridex.trips.requested")
                .description("Number of successfully created trips")
                .register(registry);

        this.tripsCompleted = Counter.builder("ridex.trips.completed")
                .description("Number of completed trips")
                .register(registry);

        this.tripsCancelled = Counter.builder("ridex.trips.cancelled")
                .description("Number of cancelled trips")
                .register(registry);

        this.tripDuration = Timer.builder("ridex.trip.duration")
                .description("Duration of completed trips")
                .publishPercentiles(0.50, 0.95, 0.99)
                .register(registry);
    }

    public void tripRequested() {
        tripsRequested.increment();
    }

    public void tripCompleted(Duration duration) {
        tripsCompleted.increment();
        tripDuration.record(duration);
    }

    public void tripCancelled() {
        tripsCancelled.increment();
    }
}