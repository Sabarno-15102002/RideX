package com.ridex.rider.service.util;

import org.springframework.stereotype.Service;

import com.ridex.rider.entity.OutboxEvent;
import com.ridex.rider.event.EventSerializer;
import com.ridex.rider.event.RiderCreatedEvent;
import com.ridex.rider.repository.OutboxEventRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class OutboxEventService {

    private final OutboxEventRepository outboxEventRepository;
    private final EventSerializer eventSerializer;

    public void saveRiderCreatedEvent(RiderCreatedEvent event) {

        OutboxEvent outboxEvent = OutboxEvent.builder()
                .aggregateType("RIDER")
                .aggregateId(event.riderId())
                .eventType("RIDER_CREATED")
                .payload(eventSerializer.serialize(event))
                .build();

        outboxEventRepository.save(outboxEvent);
    }
}