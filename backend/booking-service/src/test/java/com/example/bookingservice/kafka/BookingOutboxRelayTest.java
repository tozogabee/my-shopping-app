package com.example.bookingservice.kafka;

import com.example.bookingservice.events.BookingOutboxEvent;
import com.example.common.events.BookingEventType;
import com.example.bookingservice.events.repository.BookingOutboxEventRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Collections;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class BookingOutboxRelayTest {

    @Mock
    private BookingOutboxEventRepository outboxRepository;

    @Mock
    private BookingKafkaProducer producer;

    @InjectMocks
    private BookingOutboxRelay relay;

    @Test
    void relay_publishesEachUnprocessedEventAndMarksItProcessed() {
        BookingOutboxEvent event1 = new BookingOutboxEvent("agg-1", BookingEventType.BOOKING_CREATED, "{}");
        BookingOutboxEvent event2 = new BookingOutboxEvent("agg-2", BookingEventType.BOOKING_CONFIRMED, "{}");
        when(outboxRepository.findTop50ByProcessedFalseOrderByCreatedAtAsc())
                .thenReturn(List.of(event1, event2));

        relay.relay();

        verify(producer).send(event1);
        verify(producer).send(event2);
        assertThat(event1.isProcessed()).isTrue();
        assertThat(event2.isProcessed()).isTrue();
    }

    @Test
    void relay_noUnprocessedEvents_doesNotCallProducer() {
        when(outboxRepository.findTop50ByProcessedFalseOrderByCreatedAtAsc())
                .thenReturn(Collections.emptyList());

        relay.relay();

        verify(producer, never()).send(any());
    }

    @Test
    void relay_processedInOrder_oldestFirst() {
        BookingOutboxEvent first = new BookingOutboxEvent("agg-1", BookingEventType.BOOKING_CREATED, "{}");
        BookingOutboxEvent second = new BookingOutboxEvent("agg-2", BookingEventType.BOOKING_CREATED, "{}");
        when(outboxRepository.findTop50ByProcessedFalseOrderByCreatedAtAsc())
                .thenReturn(List.of(first, second));

        relay.relay();

        var ordered = org.mockito.Mockito.inOrder(producer);
        ordered.verify(producer).send(first);
        ordered.verify(producer).send(second);
    }
}