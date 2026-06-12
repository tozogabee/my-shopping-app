package com.example.bookingservice.kafka;

import com.example.bookingservice.events.BookingOutboxEvent;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class BookingKafkaProducer {

    static final String TOPIC = "booking-events";

    private final KafkaTemplate<String, String> kafkaTemplate;

    public BookingKafkaProducer(KafkaTemplate<String, String> kafkaTemplate) {
        this.kafkaTemplate = kafkaTemplate;
    }

    public void send(BookingOutboxEvent event) {
        try {
            kafkaTemplate.send(TOPIC, event.getAggregateId(), event.getPayload()).get();
            log.debug("Published event {} to topic {}", event.getAggregateId(), TOPIC);
        } catch (Exception e) {
            throw new RuntimeException("Failed to publish event " + event.getAggregateId() + " to Kafka", e);
        }
    }
}