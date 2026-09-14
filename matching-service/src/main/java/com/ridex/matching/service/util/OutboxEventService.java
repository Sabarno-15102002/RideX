package com.ridex.matching.service.util;

import org.springframework.stereotype.Service;

import com.ridex.matching.entity.OutboxEvent;
import com.ridex.matching.event.DriverMatchRequestedEvent;
import com.ridex.matching.event.EventSerializer;
import com.ridex.matching.repository.OutboxEventRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class OutboxEventService {

    private final OutboxEventRepository outboxEventRepository;
    private final EventSerializer eventSerializer;

    public void saveDriverMatchRequestedEvent(
            DriverMatchRequestedEvent event
    ) {
        OutboxEvent outboxEvent = OutboxEvent.builder()
                .id(event.eventId())
                .aggregateType("DRIVER_MATCH")
                .aggregateId(event.driverId())
                .eventType("DRIVER_MATCH_REQUESTED")
                .payload(eventSerializer.serialize(event))
                .build();

        outboxEventRepository.save(outboxEvent);
    }
}