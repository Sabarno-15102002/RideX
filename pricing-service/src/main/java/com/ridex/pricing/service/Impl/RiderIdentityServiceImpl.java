package com.ridex.pricing.service.Impl;

import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.ridex.pricing.entity.ProcessedEvent;
import com.ridex.pricing.entity.RiderIdentity;
import com.ridex.pricing.event.RiderCreatedEvent;
import com.ridex.pricing.repository.ProcessedEventRepository;
import com.ridex.pricing.repository.RiderIdentityRepository;
import com.ridex.pricing.service.RiderIdentityService;

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

    @Override
    public UUID getRiderIdByUserId(UUID userId) {
        RiderIdentity riderIdentity = riderIdentityRepository.findById(userId).orElseThrow(() -> new RuntimeException("Mapping Not found for the user id:" + userId));

        return riderIdentity.getRiderId();
    }
}