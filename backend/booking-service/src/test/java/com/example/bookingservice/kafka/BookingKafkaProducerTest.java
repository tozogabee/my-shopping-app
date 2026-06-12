package com.example.bookingservice.kafka;

import com.example.bookingservice.events.BookingOutboxEvent;
import com.example.bookingservice.events.enums.BookingEventType;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.kafka.core.KafkaTemplate;

import static org.mockito.Mockito.verify;

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

        verify(kafkaTemplate).send(BookingKafkaProducer.TOPIC, "booking-123", "{\"id\":\"booking-123\"}");
    }

    @Test
    void send_usesPayloadAsValue() {
        String payload = "{\"status\":\"CONFIRMED\"}";
        BookingOutboxEvent event = new BookingOutboxEvent("agg-1", BookingEventType.BOOKING_CONFIRMED, payload);

        producer.send(event);

        verify(kafkaTemplate).send(BookingKafkaProducer.TOPIC, "agg-1", payload);
    }
}