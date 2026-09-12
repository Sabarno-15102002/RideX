package com.ridex.trip.service.util;

import org.springframework.stereotype.Service;

import com.ridex.trip.entity.OutboxEvent;
import com.ridex.trip.event.EventSerializer;
import com.ridex.trip.event.TripRequestedEvent;
import com.ridex.trip.repository.OutboxEventRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class OutboxEventService {

    private final OutboxEventRepository outboxEventRepository;
    private final EventSerializer eventSerializer;

    public void saveTripRequestedEvent(
            TripRequestedEvent event
    ) {
        OutboxEvent outboxEvent = OutboxEvent.builder()
                .id(event.eventId())
                .aggregateType("TRIP")
                .aggregateId(event.tripId())
                .eventType("TRIP_REQUESTED")
                .payload(eventSerializer.serialize(event))
                .build();

        outboxEventRepository.save(outboxEvent);
    }
}