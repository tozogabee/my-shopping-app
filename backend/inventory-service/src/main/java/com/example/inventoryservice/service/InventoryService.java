package com.example.inventoryservice.service;

import com.example.inventoryservice.event.InventoryEventType;
import com.example.inventoryservice.event.InventoryOutboxEvent;
import com.example.inventoryservice.event.InventoryOutboxEventRepository;
import com.example.inventoryservice.model.ResourceInventory;
import com.example.inventoryservice.model.ResourceInventoryRepository;
import jakarta.transaction.Transactional;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.node.ObjectNode;

import java.util.UUID;

@Slf4j
@Service
public class InventoryService {

    private final ResourceInventoryRepository inventoryRepository;
    private final InventoryOutboxEventRepository outboxRepository;
    private final ObjectMapper objectMapper;

    public InventoryService(ResourceInventoryRepository inventoryRepository,
                            InventoryOutboxEventRepository outboxRepository,
                            ObjectMapper objectMapper) {
        this.inventoryRepository = inventoryRepository;
        this.outboxRepository = outboxRepository;
        this.objectMapper = objectMapper;
    }

    @Transactional
    public void reserveInventory(UUID bookingId, UUID resourceId) {
        try {
            ResourceInventory inventory = inventoryRepository.findById(resourceId)
                    .orElseThrow(() -> new IllegalArgumentException("Erőforrás nem található"));

            inventory.reserve(1);
            inventoryRepository.save(inventory);

            ObjectNode payload = objectMapper.createObjectNode();
            payload.put("aggregateId", bookingId.toString());
            payload.put("resourceId", resourceId.toString());
            payload.put("status", "RESERVED");

            InventoryOutboxEvent outboxEvent = new InventoryOutboxEvent(
                    bookingId.toString(), // A BookingId a folyamat kulcsa (Saga ID)
                    InventoryEventType.INVENTORY_RESERVED,
                    payload.toString()
            );
            outboxRepository.save(outboxEvent);

            log.info("Készlet sikeresen lefoglalva a {} foglaláshoz", bookingId);

        } catch (Exception e) {
            log.warn("Készletfoglalás elhasalt a {} foglaláshoz. Ok: {}", bookingId, e.getMessage());

            ObjectNode failedPayload = objectMapper.createObjectNode();
            failedPayload.put("aggregateId", bookingId.toString());
            failedPayload.put("reason", "Készlethiány vagy zárolási hiba");

            InventoryOutboxEvent failedEvent = new InventoryOutboxEvent(
                    bookingId.toString(),
                    InventoryEventType.INVENTORY_RESERVATION_FAILED,
                    failedPayload.toString()
            );
            outboxRepository.save(failedEvent);
        }
    }
}
