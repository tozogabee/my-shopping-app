package com.example.bookingservice.kafka;

import com.example.bookingservice.events.saga.SagaFailureEvent;
import com.example.bookingservice.events.saga.SagaSuccessEvent;
import com.example.bookingservice.service.BookingService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThatNoException;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class BookingKafkaConsumerTest {

    @Mock
    private BookingService bookingService;

    @InjectMocks
    private BookingKafkaConsumer consumer;

    // ── onSagaSuccess ────────────────────────────────────────────────────────

    @Test
    void onSagaSuccess_callsConfirmBooking() {
        UUID bookingId = UUID.randomUUID();

        consumer.onSagaSuccess(new SagaSuccessEvent(bookingId));

        verify(bookingService).confirmBooking(bookingId);
    }

    @Test
    void onSagaSuccess_serviceThrows_doesNotPropagateException() {
        UUID bookingId = UUID.randomUUID();
        doThrow(new RuntimeException("DB error")).when(bookingService).confirmBooking(bookingId);

        assertThatNoException().isThrownBy(() -> consumer.onSagaSuccess(new SagaSuccessEvent(bookingId)));
    }

    // ── onSagaFailure ────────────────────────────────────────────────────────

    @Test
    void onSagaFailure_callsCancelBookingWithReason() {
        UUID bookingId = UUID.randomUUID();

        consumer.onSagaFailure(new SagaFailureEvent(bookingId, "Insufficient stock"));

        verify(bookingService).cancelBooking(bookingId, "Insufficient stock");
    }

    @Test
    void onSagaFailure_nullReason_usesDefaultReason() {
        UUID bookingId = UUID.randomUUID();

        consumer.onSagaFailure(new SagaFailureEvent(bookingId, null));

        verify(bookingService).cancelBooking(bookingId, "Ismeretlen hiba");
    }

    @Test
    void onSagaFailure_serviceThrows_doesNotPropagateException() {
        UUID bookingId = UUID.randomUUID();
        doThrow(new RuntimeException("error")).when(bookingService).cancelBooking(any(), any());

        assertThatNoException().isThrownBy(() ->
                consumer.onSagaFailure(new SagaFailureEvent(bookingId, "reason")));
    }
}