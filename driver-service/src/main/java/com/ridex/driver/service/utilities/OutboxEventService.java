package com.ridex.driver.service.utilities;

import org.springframework.stereotype.Service;

import com.ridex.driver.entity.OutboxEvent;
import com.ridex.driver.event.DriverStatusChangedEvent;
import com.ridex.driver.event.EventSerializer;
import com.ridex.driver.repository.OutboxEventRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class OutboxEventService {

    private final OutboxEventRepository outboxEventRepository;
    private final EventSerializer eventSerializer;

    public void saveDriverStatusChangedEvent(DriverStatusChangedEvent event) {

        OutboxEvent outboxEvent = OutboxEvent.builder()
                .aggregateType("DRIVER")
                .aggregateId(event.driverId())
                .eventType("DRIVER_STATUS_CHANGED")
                .payload(eventSerializer.serialize(event))
                .build();

        outboxEventRepository.save(outboxEvent);
    }
}