package com.ridex.rider.service.Impl;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.ridex.rider.entity.ProcessedEvent;
import com.ridex.rider.entity.Rider;
import com.ridex.rider.event.UserRegisteredEvent;
import com.ridex.rider.repository.ProcessedEventRepository;
import com.ridex.rider.repository.RiderRepository;
import com.ridex.rider.service.RiderRegistrationService;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class RiderRegistrationServiceImpl implements RiderRegistrationService {

    private final RiderRepository riderRepository;
    private final ProcessedEventRepository processedEventRepository;

    @Override
    @Transactional
    public void handleUserRegistered(UserRegisteredEvent event) {

        if (processedEventRepository.existsById(event.eventId())) {
            return;
        }

        if (riderRepository.existsByUserId(event.userId())) {
            processedEventRepository.save(
                    ProcessedEvent.builder()
                            .eventId(event.eventId())
                            .build()
            );

            return;
        }

        Rider rider = Rider.builder()
                .userId(event.userId())
                .name(event.name())
                .build();

        riderRepository.save(rider);

        processedEventRepository.save(
                ProcessedEvent.builder()
                        .eventId(event.eventId())
                        .build()
        );
    }
}