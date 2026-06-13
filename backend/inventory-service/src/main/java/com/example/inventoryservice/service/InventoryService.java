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
                    .orElseThrow(() -> new IllegalArgumentException("Resource not found"));

            inventory.reserve(1);
            inventoryRepository.save(inventory);

            ObjectNode payload = objectMapper.createObjectNode();
            payload.put("aggregateId", bookingId.toString());
            payload.put("resourceId", resourceId.toString());
            payload.put("status", InventoryEventType.INVENTORY_RESERVED.name());

            InventoryOutboxEvent outboxEvent = new InventoryOutboxEvent(
                    bookingId.toString(),
                    InventoryEventType.INVENTORY_RESERVED,
                    payload.toString()
            );
            outboxRepository.save(outboxEvent);

            log.info("Inventory successfully reserved for booking {}", bookingId);

        } catch (Exception e) {
            log.warn("Inventory reservation failed for booking {}. Reason: {}", bookingId, e.getMessage());

            ObjectNode failedPayload = this.objectMapper.createObjectNode();
            failedPayload.put("aggregateId", bookingId.toString());
            failedPayload.put("status", InventoryEventType.INVENTORY_RESERVATION_FAILED.name());
            failedPayload.put("reason", "Insufficient inventory or locking error");

            InventoryOutboxEvent failedEvent = new InventoryOutboxEvent(
                    bookingId.toString(),
                    InventoryEventType.INVENTORY_RESERVATION_FAILED,
                    failedPayload.toString()
            );
            this.outboxRepository.save(failedEvent);
        }
    }

    @Transactional
    public void releaseInventory(UUID bookingId, UUID resourceId) {
        try {
            ResourceInventory inventory = inventoryRepository.findById(resourceId)
                    .orElseThrow(() -> new IllegalArgumentException("Resource not found"));

            inventory.release(1);
            inventoryRepository.save(inventory);

            ObjectNode payload = objectMapper.createObjectNode();
            payload.put("aggregateId", bookingId.toString());
            payload.put("resourceId", resourceId.toString());
            payload.put("status", InventoryEventType.INVENTORY_RELEASED.name());

            InventoryOutboxEvent outboxEvent = new InventoryOutboxEvent(
                    bookingId.toString(),
                    InventoryEventType.INVENTORY_RELEASED,
                    payload.toString()
            );
            outboxRepository.save(outboxEvent);

            log.info("Inventory successfully released for booking {} (compensation)", bookingId);

        } catch (Exception e) {
            // Only reached on a physical database error during release — data integrity may be compromised. Requires DLQ or manual intervention.
            log.error("Critical error releasing inventory for booking {}: {}", bookingId, e.getMessage());
        }
    }
}
