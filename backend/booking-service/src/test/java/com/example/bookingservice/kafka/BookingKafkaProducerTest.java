package com.example.bookingservice.kafka;

import com.example.bookingservice.events.BookingOutboxEvent;
import com.example.bookingservice.events.enums.BookingEventType;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.Message;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class BookingKafkaProducerTest {

    @Mock
    private KafkaTemplate<String, String> kafkaTemplate;

    @InjectMocks
    private BookingKafkaProducer producer;

    @Test
    void send_publishesToBookingEventsTopicWithAggregateIdAsKey() {
        BookingOutboxEvent event = new BookingOutboxEvent(
                "booking-123",
                BookingEventType.BOOKING_CREATED,
                "{\"id\":\"booking-123\"}"
        );

        producer.send(event);

        ArgumentCaptor<Message<String>> captor = messageCaptor();
        verify(kafkaTemplate).send(captor.capture());
        Message<String> msg = captor.getValue();
        assertThat(msg.getHeaders().get(KafkaHeaders.TOPIC)).isEqualTo(BookingKafkaProducer.TOPIC);
        assertThat(msg.getHeaders().get(KafkaHeaders.KEY)).isEqualTo("booking-123");
        assertThat(msg.getPayload()).isEqualTo("{\"id\":\"booking-123\"}");
    }

    @Test
    void send_usesPayloadAsValue() {
        String payload = "{\"status\":\"CONFIRMED\"}";
        BookingOutboxEvent event = new BookingOutboxEvent("agg-1", BookingEventType.BOOKING_CONFIRMED, payload);

        producer.send(event);

        ArgumentCaptor<Message<String>> captor = messageCaptor();
        verify(kafkaTemplate).send(captor.capture());
        assertThat(captor.getValue().getPayload()).isEqualTo(payload);
    }

    @Test
    void send_includesEventTypeHeader() {
        BookingOutboxEvent event = new BookingOutboxEvent(
                "agg-2",
                BookingEventType.BOOKING_CONFIRMED,
                "{}"
        );

        producer.send(event);

        ArgumentCaptor<Message<String>> captor = messageCaptor();
        verify(kafkaTemplate).send(captor.capture());
        assertThat(captor.getValue().getHeaders().get("eventType")).isEqualTo("BOOKING_CONFIRMED");
    }

    @Test
    void send_kafkaFailure_throwsRuntimeException() throws Exception {
        BookingOutboxEvent event = new BookingOutboxEvent("agg-3", BookingEventType.BOOKING_CREATED, "{}");
        when(kafkaTemplate.send(any(Message.class)))
                .thenThrow(new RuntimeException("broker down"));

        assertThatThrownBy(() -> producer.send(event))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Failed to publish event agg-3");
    }

    @SuppressWarnings("unchecked")
    private static ArgumentCaptor<Message<String>> messageCaptor() {
        return ArgumentCaptor.forClass((Class<Message<String>>) (Class<?>) Message.class);
    }
}
