package com.ridex.trip.service.Impl;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.ridex.trip.entity.ProcessedEvent;
import com.ridex.trip.entity.RiderIdentity;
import com.ridex.trip.event.event.RiderCreatedEvent;
import com.ridex.trip.repository.ProcessedEventRepository;
import com.ridex.trip.repository.RiderIdentityRepository;
import com.ridex.trip.service.RiderIdentityService;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class RiderIdentityServiceImpl implements RiderIdentityService {

    private final RiderIdentityRepository riderIdentityRepository;
    private final ProcessedEventRepository processedEventRepository;

    @Override
    @Transactional
    public void handleRiderCreated(RiderCreatedEvent event) {

        if (processedEventRepository.existsById(event.eventId())) {
            return;
        }

        if (!riderIdentityRepository.existsById(event.userId())) {

            RiderIdentity identity = RiderIdentity.builder()
                    .userId(event.userId())
                    .riderId(event.riderId())
                    .build();

            riderIdentityRepository.save(identity);
        }

        processedEventRepository.save(
                ProcessedEvent.builder()
                        .eventId(event.eventId())
                        .build()
        );
    }
}