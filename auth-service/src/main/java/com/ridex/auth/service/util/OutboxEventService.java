package com.ridex.auth.service.util;

import org.springframework.stereotype.Service;

import com.ridex.auth.entity.OutboxEvent;
import com.ridex.auth.event.EventSerializer;
import com.ridex.auth.event.UserRegisteredEvent;
import com.ridex.auth.repository.OutboxEventRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class OutboxEventService {

    private final OutboxEventRepository outboxEventRepository;
    private final EventSerializer eventSerializer;

    public void saveUserRegisteredEvent(UserRegisteredEvent event) {

        OutboxEvent outboxEvent = OutboxEvent.builder()
                .aggregateType("USER")
                .aggregateId(event.userId())
                .eventType("USER_REGISTERED")
                .payload(eventSerializer.serialize(event))
                .build();

        outboxEventRepository.save(outboxEvent);
    }
}