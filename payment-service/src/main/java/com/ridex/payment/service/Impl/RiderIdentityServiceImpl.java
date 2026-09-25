package com.ridex.payment.service.Impl;

import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.ridex.payment.entity.RiderIdentity;
import com.ridex.payment.event.RiderCreatedEvent;
import com.ridex.payment.repository.RiderIdentityRepository;
import com.ridex.payment.service.RiderIdentityService;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class RiderIdentityServiceImpl implements RiderIdentityService{

    private final RiderIdentityRepository riderIdentityRepository;

    @Override 
    @Transactional
    public void createIdentity(RiderCreatedEvent event) {

        if (riderIdentityRepository.existsById(event.userId())) {
            return;
        }

        RiderIdentity identity = new RiderIdentity(
                event.userId(),
                event.riderId(),
                event.createdAt()
        );

        riderIdentityRepository.save(identity);
    }

    @Override 
    @Transactional(readOnly = true)
    public UUID getRiderIdByUserId(UUID userId) {

        return riderIdentityRepository.findByUserId(userId)
                .map(RiderIdentity::getRiderId)
                .orElseThrow(() ->
                        new IllegalStateException(
                                "Rider identity not found for user: "
                                        + userId
                        )
                );
    }
}