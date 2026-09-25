package com.ridex.payment.event;

import java.time.Instant;

import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.ridex.payment.entity.ProcessedEvent;
import com.ridex.payment.repository.ProcessedEventRepository;
import com.ridex.payment.service.RiderIdentityService;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class RiderCreatedEventListener {

    private final RiderIdentityService riderIdentityService;
    private final ProcessedEventRepository processedEventRepository;

    @KafkaListener(
            topics = "rider.created",
            groupId = "payment-service"
    )
    @Transactional 
    public void handle(RiderCreatedEvent event) {

        if (processedEventRepository.existsById(event.eventId())) {
            return;
        }

        riderIdentityService.createIdentity(event);

        processedEventRepository.save(
                new ProcessedEvent(
                        event.eventId(),
                        Instant.now()
                )
        );
    }
}