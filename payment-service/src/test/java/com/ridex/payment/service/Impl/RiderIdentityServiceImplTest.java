package com.ridex.payment.service.Impl;

import com.ridex.payment.entity.RiderIdentity;
import com.ridex.payment.event.RiderCreatedEvent;
import com.ridex.payment.exception.ResourceNotFoundException;
import com.ridex.payment.repository.RiderIdentityRepository;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class RiderIdentityServiceImplTest {

    @Mock
    RiderIdentityRepository repository;
    @InjectMocks
    RiderIdentityServiceImpl service;

    @Test
    void createsIdentityWhenItDoesNotExist() {
        UUID userId = UUID.randomUUID();
        UUID riderId = UUID.randomUUID();
        Instant createdAt = Instant.now();

        when(repository.existsById(userId)).thenReturn(false);

        service.createIdentity(new RiderCreatedEvent(
                UUID.randomUUID(), riderId, userId, createdAt));

        verify(repository).save(argThat(identity -> identity.getUserId().equals(userId)
                && identity.getRiderId().equals(riderId)
                && identity.getCreatedAt().equals(createdAt)));
    }

    @Test
    void doesNotCreateDuplicateIdentity() {
        UUID userId = UUID.randomUUID();

        when(repository.existsById(userId)).thenReturn(true);

        service.createIdentity(new RiderCreatedEvent(
                UUID.randomUUID(), UUID.randomUUID(), userId, Instant.now()));

        verify(repository, never()).save(any());
    }

    @Test
    void returnsRiderIdForUser() {
        UUID userId = UUID.randomUUID();
        UUID riderId = UUID.randomUUID();

        when(repository.findByUserId(userId))
                .thenReturn(Optional.of(new RiderIdentity(
                        userId, riderId, Instant.now())));

        assertEquals(riderId, service.getRiderIdByUserId(userId));
    }

    @Test
    void throwsWhenRiderIdentityDoesNotExist() {
        UUID userId = UUID.randomUUID();
        when(repository.findByUserId(userId)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class,
                () -> service.getRiderIdByUserId(userId));
    }
}
