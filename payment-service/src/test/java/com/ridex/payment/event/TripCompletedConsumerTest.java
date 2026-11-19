package com.ridex.payment.event;

import com.ridex.payment.client.PricingServiceClient;
import com.ridex.payment.dto.response.FareQuoteResponse;
import com.ridex.payment.repository.ProcessedEventRepository;
import com.ridex.payment.service.PaymentService;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TripCompletedConsumerTest {

    @Mock
    PaymentService paymentService;
    @Mock
    PricingServiceClient pricingServiceClient;
    @Mock
    ProcessedEventRepository processedEventRepository;

    @InjectMocks
    TripCompletedConsumer consumer;

    private TripCompletedEvent event() {
        return new TripCompletedEvent(
                UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(),
                UUID.randomUUID(), 12.9, 77.5, 13.0, 77.6,
                Instant.now().minusSeconds(600), Instant.now());
    }

    private FareQuoteResponse fare(TripCompletedEvent event) {
        return new FareQuoteResponse(
                event.tripId(), event.riderId(),
                new BigDecimal("20.00"), new BigDecimal("3.00"),
                new BigDecimal("10.00"), new BigDecimal("30.00"),
                new BigDecimal("40.00"), new BigDecimal("90.00"), "INR");
    }

    @Test
    void handlesNewTripCompletedEvent() {
        TripCompletedEvent event = event();
        when(processedEventRepository.existsById(event.eventId())).thenReturn(false);
        when(pricingServiceClient.getFareQuote(event.tripId())).thenReturn(fare(event));

        consumer.handle(event);

        verify(paymentService).createPendingPayment(event, fare(event));
        verify(processedEventRepository).save(argThat(processed -> processed.getEventId().equals(event.eventId())));
    }

    @Test
    void ignoresAlreadyProcessedEvent() {
        TripCompletedEvent event = event();
        when(processedEventRepository.existsById(event.eventId())).thenReturn(true);

        consumer.handle(event);

        verifyNoInteractions(pricingServiceClient, paymentService);
        verify(processedEventRepository, never()).save(any());
    }

    @Test
    void doesNotMarkEventProcessedWhenPricingFails() {
        TripCompletedEvent event = event();
        when(processedEventRepository.existsById(event.eventId())).thenReturn(false);
        when(pricingServiceClient.getFareQuote(event.tripId()))
                .thenThrow(new RuntimeException("pricing unavailable"));

        org.junit.jupiter.api.Assertions.assertThrows(
                RuntimeException.class, () -> consumer.handle(event));

        verify(paymentService, never()).createPendingPayment(any(), any());
        verify(processedEventRepository, never()).save(any());
    }
}
