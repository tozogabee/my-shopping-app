package com.example.inventoryservice.kafka;

import com.example.inventoryservice.event.InventoryEventType;
import com.example.inventoryservice.event.InventoryOutboxEvent;
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
class InventoryKafkaProducerTest {

    @Mock
    private KafkaTemplate<String, String> kafkaTemplate;

    @InjectMocks
    private InventoryKafkaProducer producer;

    @Test
    void send_publishesToInventoryEventsTopicWithAggregateIdAsKey() {
        InventoryOutboxEvent event = new InventoryOutboxEvent(
                "booking-123",
                InventoryEventType.INVENTORY_RESERVED,
                "{\"aggregateId\":\"booking-123\"}"
        );

        producer.send(event);

        ArgumentCaptor<Message<String>> captor = messageCaptor();
        verify(kafkaTemplate).send(captor.capture());
        Message<String> msg = captor.getValue();
        assertThat(msg.getHeaders().get(KafkaHeaders.TOPIC)).isEqualTo(InventoryKafkaProducer.TOPIC);
        assertThat(msg.getHeaders().get(KafkaHeaders.KEY)).isEqualTo("booking-123");
        assertThat(msg.getPayload()).isEqualTo("{\"aggregateId\":\"booking-123\"}");
    }

    @Test
    void send_usesPayloadAsMessageValue() {
        String payload = "{\"status\":\"INVENTORY_RESERVED\"}";
        InventoryOutboxEvent event = new InventoryOutboxEvent("agg-1", InventoryEventType.INVENTORY_RESERVED, payload);

        producer.send(event);

        ArgumentCaptor<Message<String>> captor = messageCaptor();
        verify(kafkaTemplate).send(captor.capture());
        assertThat(captor.getValue().getPayload()).isEqualTo(payload);
    }

    @Test
    void send_includesEventTypeHeader_reserved() {
        InventoryOutboxEvent event = new InventoryOutboxEvent("agg-2", InventoryEventType.INVENTORY_RESERVED, "{}");

        producer.send(event);

        ArgumentCaptor<Message<String>> captor = messageCaptor();
        verify(kafkaTemplate).send(captor.capture());
        assertThat(captor.getValue().getHeaders().get("eventType"))
                .isEqualTo(InventoryEventType.INVENTORY_RESERVED.name());
    }

    @Test
    void send_includesEventTypeHeader_failed() {
        InventoryOutboxEvent event = new InventoryOutboxEvent("agg-3", InventoryEventType.INVENTORY_RESERVATION_FAILED, "{}");

        producer.send(event);

        ArgumentCaptor<Message<String>> captor = messageCaptor();
        verify(kafkaTemplate).send(captor.capture());
        assertThat(captor.getValue().getHeaders().get("eventType"))
                .isEqualTo(InventoryEventType.INVENTORY_RESERVATION_FAILED.name());
    }

    @Test
    void send_includesEventTypeHeader_released() {
        InventoryOutboxEvent event = new InventoryOutboxEvent("agg-4", InventoryEventType.INVENTORY_RELEASED, "{}");

        producer.send(event);

        ArgumentCaptor<Message<String>> captor = messageCaptor();
        verify(kafkaTemplate).send(captor.capture());
        assertThat(captor.getValue().getHeaders().get("eventType"))
                .isEqualTo(InventoryEventType.INVENTORY_RELEASED.name());
    }

    @Test
    void send_kafkaFailure_throwsRuntimeException() {
        InventoryOutboxEvent event = new InventoryOutboxEvent("agg-5", InventoryEventType.INVENTORY_RESERVED, "{}");
        when(kafkaTemplate.send(any(Message.class))).thenThrow(new RuntimeException("broker down"));

        assertThatThrownBy(() -> producer.send(event))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Failed to publish event agg-5");
    }

    @SuppressWarnings("unchecked")
    private static ArgumentCaptor<Message<String>> messageCaptor() {
        return ArgumentCaptor.forClass((Class<Message<String>>) (Class<?>) Message.class);
    }
}
