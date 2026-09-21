package com.ridex.driver.service.utilities;

import org.springframework.stereotype.Service;

import com.ridex.driver.entity.OutboxEvent;
import com.ridex.driver.event.DriverArrivedEvent;
import com.ridex.driver.event.DriverRideAcceptedEvent;
import com.ridex.driver.event.DriverRideExpiredEvent;
import com.ridex.driver.event.DriverRideRejectedEvent;
import com.ridex.driver.event.DriverStatusChangedEvent;
import com.ridex.driver.event.DriverTripCompletedEvent;
import com.ridex.driver.event.DriverTripStartedEvent;
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

    public void saveDriverRideAcceptedEvent(DriverRideAcceptedEvent event){
        OutboxEvent outboxEvent = OutboxEvent.builder()
                .aggregateType("DRIVER")
                .aggregateId(event.driverId())
                .eventType("DRIVER_RIDE_ACCEPTED")
                .payload(eventSerializer.serialize(event))
                .build();

        outboxEventRepository.save(outboxEvent);
    }

    public void saveDriverRideRejectedEvent(DriverRideRejectedEvent event){
        OutboxEvent outboxEvent = OutboxEvent.builder()
                .aggregateType("DRIVER")
                .aggregateId(event.driverId())
                .eventType("DRIVER_RIDE_REJECTED")
                .payload(eventSerializer.serialize(event))
                .build();

        outboxEventRepository.save(outboxEvent);
    }

    public void saveDriverRideExpiredEvent(DriverRideExpiredEvent event){
        OutboxEvent outboxEvent = OutboxEvent.builder()
                .aggregateType("DRIVER")
                .aggregateId(event.driverId())
                .eventType("DRIVER_RIDE_EXPIRED")
                .payload(eventSerializer.serialize(event))
                .build();

        outboxEventRepository.save(outboxEvent);
    }

    public void saveDriverArrivedEvent(DriverArrivedEvent event){
        OutboxEvent outboxEvent = OutboxEvent.builder()
                .aggregateType("DRIVER")
                .aggregateId(event.driverId())
                .eventType("DRIVER_ARRIVED")
                .payload(eventSerializer.serialize(event))
                .build();

        outboxEventRepository.save(outboxEvent);
    }

    public void saveDriverTripStartedEvent(DriverTripStartedEvent event){
        OutboxEvent outboxEvent = OutboxEvent.builder()
                .aggregateType("DRIVER")
                .aggregateId(event.driverId())
                .eventType("DRIVER_TRIP_STARTED")
                .payload(eventSerializer.serialize(event))
                .build();

        outboxEventRepository.save(outboxEvent);
    }

    public void saveDriverTripCompletedEvent(DriverTripCompletedEvent event){
        OutboxEvent outboxEvent = OutboxEvent.builder()
                .aggregateType("DRIVER")
                .aggregateId(event.driverId())
                .eventType("DRIVER_TRIP_COMPLETED")
                .payload(eventSerializer.serialize(event))
                .build();

        outboxEventRepository.save(outboxEvent);
    }
}