package com.ridex.pricing.event;

import com.ridex.pricing.repository.ProcessedEventRepository;
import com.ridex.pricing.service.FareCalculationService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.UUID;

import static org.mockito.Mockito.*;

class TripCompletedConsumerTest {

        private FareCalculationService fareCalculationService;
        private ProcessedEventRepository processedEventRepository;
        private TripCompletedConsumer consumer;

        private TripCompletedEvent event;

        @BeforeEach
        void setUp() {
                fareCalculationService = mock(FareCalculationService.class);
                processedEventRepository = mock(ProcessedEventRepository.class);

                consumer = new TripCompletedConsumer(
                                fareCalculationService,
                                processedEventRepository);

                event = new TripCompletedEvent(
                                UUID.randomUUID(),
                                UUID.randomUUID(),
                                UUID.randomUUID(),
                                UUID.randomUUID(),
                                12.0,
                                77.0,
                                12.1,
                                77.1,
                                Instant.now(),
                                Instant.now().plusSeconds(900));
        }

        @Test
        void shouldCreateFareQuoteAndMarkEventProcessed() {
                when(processedEventRepository.existsById(event.eventId()))
                                .thenReturn(false);

                consumer.consume(event);

                verify(fareCalculationService).createFareQuote(event);
                verify(processedEventRepository)
                                .save(argThat(processed -> processed.getEventId().equals(event.eventId())
                                                && processed.getProcessedAt() != null));
        }

        @Test
        void shouldIgnoreDuplicateEvent() {
                when(processedEventRepository.existsById(event.eventId()))
                                .thenReturn(true);

                consumer.consume(event);

                verifyNoInteractions(fareCalculationService);
                verify(processedEventRepository, never()).save(any());
        }

        @Test
        void shouldNotMarkEventProcessedWhenFareCreationFails() {
                when(processedEventRepository.existsById(event.eventId()))
                                .thenReturn(false);
                when(fareCalculationService.createFareQuote(event))
                                .thenThrow(new RuntimeException("Fare calculation failed"));

                org.junit.jupiter.api.Assertions.assertThrows(
                                RuntimeException.class,
                                () -> consumer.consume(event));

                verify(processedEventRepository, never()).save(any());
        }
}
