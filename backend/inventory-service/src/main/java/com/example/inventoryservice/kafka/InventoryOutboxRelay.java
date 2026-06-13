package com.example.inventoryservice.kafka;

import com.example.inventoryservice.event.InventoryOutboxEvent;
import com.example.inventoryservice.event.InventoryOutboxEventRepository;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Slf4j
@Component
@ConditionalOnProperty(name = "inventory.relay.enabled", havingValue = "true", matchIfMissing = true)
public class InventoryOutboxRelay {

    private final InventoryOutboxEventRepository outboxRepository;
    private final InventoryKafkaProducer producer;

    public InventoryOutboxRelay(InventoryOutboxEventRepository outboxRepository,
                                InventoryKafkaProducer producer) {
        this.outboxRepository = outboxRepository;
        this.producer = producer;
    }

    @PostConstruct
    void init() {
        log.info("InventoryOutboxRelay started — polling every {}ms", "${inventory.relay.fixed-delay-ms:5000}");
    }

    @Scheduled(fixedDelayString = "${inventory.relay.fixed-delay-ms:5000}")
    @Transactional
    public void relay() {
        List<InventoryOutboxEvent> events =
                outboxRepository.findTop50ByProcessedFalseOrderByCreatedAtAsc();
        log.info("Relay tick: found {} unprocessed outbox event(s)", events.size());
        if (events.isEmpty()) {
            return;
        }
        for (InventoryOutboxEvent event : events) {
            producer.send(event);
            event.markAsProcessed();
            log.info("Event {} ({}) published and marked processed", event.getAggregateId(), event.getEventType());
        }
    }
}