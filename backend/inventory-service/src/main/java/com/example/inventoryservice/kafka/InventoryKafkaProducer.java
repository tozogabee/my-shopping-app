package com.example.inventoryservice.kafka;

import com.example.inventoryservice.event.InventoryOutboxEvent;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.Message;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class InventoryKafkaProducer {

    static final String TOPIC = "inventory-events";

    private final KafkaTemplate<String, String> kafkaTemplate;

    public InventoryKafkaProducer(KafkaTemplate<String, String> kafkaTemplate) {
        this.kafkaTemplate = kafkaTemplate;
    }

    public void send(InventoryOutboxEvent event) {
        try {
            Message<String> message = MessageBuilder
                    .withPayload(event.getPayload())
                    .setHeader(KafkaHeaders.TOPIC, TOPIC)
                    .setHeader(KafkaHeaders.KEY, event.getAggregateId())
                    .setHeader("eventType", event.getEventType().name())
                    .build();

            kafkaTemplate.send(message).get();
            log.debug("Published event {} to topic {} with type {}",
                    event.getAggregateId(), TOPIC, event.getEventType());
        } catch (Exception e) {
            throw new RuntimeException("Failed to publish event " + event.getAggregateId() + " to Kafka", e);
        }
    }
}