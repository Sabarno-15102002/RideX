package com.ridex.payment.event;

import java.time.Instant;
import java.util.UUID;

public record RiderCreatedEvent(
        UUID eventId,
        UUID riderId,
        UUID userId,
        Instant createdAt
) {}