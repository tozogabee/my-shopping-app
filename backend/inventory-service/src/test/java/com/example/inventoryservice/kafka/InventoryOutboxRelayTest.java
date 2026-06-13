package com.example.inventoryservice.kafka;

import com.example.inventoryservice.event.InventoryEventType;
import com.example.inventoryservice.event.InventoryOutboxEvent;
import com.example.inventoryservice.event.InventoryOutboxEventRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Collections;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class InventoryOutboxRelayTest {

    @Mock
    private InventoryOutboxEventRepository outboxRepository;

    @Mock
    private InventoryKafkaProducer producer;

    @InjectMocks
    private InventoryOutboxRelay relay;

    @Test
    void relay_publishesEachUnprocessedEventAndMarksItProcessed() {
        InventoryOutboxEvent event1 = new InventoryOutboxEvent("agg-1", InventoryEventType.INVENTORY_RESERVED, "{}");
        InventoryOutboxEvent event2 = new InventoryOutboxEvent("agg-2", InventoryEventType.INVENTORY_RELEASED, "{}");
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
    void relay_processesEventsInOrder_oldestFirst() {
        InventoryOutboxEvent first = new InventoryOutboxEvent("agg-1", InventoryEventType.INVENTORY_RESERVED, "{}");
        InventoryOutboxEvent second = new InventoryOutboxEvent("agg-2", InventoryEventType.INVENTORY_RESERVATION_FAILED, "{}");
        when(outboxRepository.findTop50ByProcessedFalseOrderByCreatedAtAsc())
                .thenReturn(List.of(first, second));

        relay.relay();

        var ordered = inOrder(producer);
        ordered.verify(producer).send(first);
        ordered.verify(producer).send(second);
    }

    @Test
    void relay_marksEventProcessedAfterSend() {
        InventoryOutboxEvent event = new InventoryOutboxEvent("agg-1", InventoryEventType.INVENTORY_RESERVED, "{}");
        assertThat(event.isProcessed()).isFalse();
        when(outboxRepository.findTop50ByProcessedFalseOrderByCreatedAtAsc()).thenReturn(List.of(event));

        relay.relay();

        assertThat(event.isProcessed()).isTrue();
    }
}
