package com.example.inventoryservice.service;

import com.example.inventoryservice.event.InventoryEventType;
import com.example.inventoryservice.event.InventoryOutboxEvent;
import com.example.inventoryservice.event.InventoryOutboxEventRepository;
import com.example.inventoryservice.model.ResourceInventory;
import com.example.inventoryservice.model.ResourceInventoryRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@ActiveProfiles("test")
class InventoryServiceComponentTest {

    @Autowired
    private InventoryService inventoryService;

    @Autowired
    private ResourceInventoryRepository inventoryRepository;

    @Autowired
    private InventoryOutboxEventRepository outboxRepository;

    private UUID resourceId;
    private UUID bookingId;

    @BeforeEach
    void setUp() {
        resourceId = UUID.randomUUID();
        bookingId = UUID.randomUUID();
    }

    @AfterEach
    void cleanup() {
        outboxRepository.deleteAll();
        inventoryRepository.deleteAll();
    }

    // ── reserveInventory — success ───────────────────────────────────────────

    @Test
    void reserveInventory_success_decrementsQuantityInDatabase() {
        inventoryRepository.save(new ResourceInventory(resourceId, 5));

        inventoryService.reserveInventory(bookingId, resourceId);

        ResourceInventory updated = inventoryRepository.findById(resourceId).orElseThrow();
        assertThat(updated.getAvailableQuantity()).isEqualTo(4);
    }

    @Test
    void reserveInventory_success_persistsOutboxEventWithReservedType() {
        inventoryRepository.save(new ResourceInventory(resourceId, 5));

        inventoryService.reserveInventory(bookingId, resourceId);

        List<InventoryOutboxEvent> events = outboxRepository.findAll();
        assertThat(events).hasSize(1);
        InventoryOutboxEvent event = events.getFirst();
        assertThat(event.getEventType()).isEqualTo(InventoryEventType.INVENTORY_RESERVED);
        assertThat(event.getAggregateId()).isEqualTo(bookingId.toString());
        assertThat(event.isProcessed()).isFalse();
    }

    @Test
    void reserveInventory_success_payloadContainsAllIds() {
        inventoryRepository.save(new ResourceInventory(resourceId, 5));

        inventoryService.reserveInventory(bookingId, resourceId);

        InventoryOutboxEvent event = outboxRepository.findAll().getFirst();
        assertThat(event.getPayload()).contains(bookingId.toString());
        assertThat(event.getPayload()).contains(resourceId.toString());
        assertThat(event.getPayload()).contains(InventoryEventType.INVENTORY_RESERVED.name());
    }

    // ── reserveInventory — failure ───────────────────────────────────────────

    @Test
    void reserveInventory_insufficientInventory_savesFailedOutboxEvent() {
        inventoryRepository.save(new ResourceInventory(resourceId, 0));

        inventoryService.reserveInventory(bookingId, resourceId);

        List<InventoryOutboxEvent> events = outboxRepository.findAll();
        assertThat(events).hasSize(1);
        assertThat(events.getFirst().getEventType()).isEqualTo(InventoryEventType.INVENTORY_RESERVATION_FAILED);
        assertThat(events.getFirst().getAggregateId()).isEqualTo(bookingId.toString());
    }

    @Test
    void reserveInventory_resourceNotFound_savesFailedOutboxEvent() {
        UUID unknownId = UUID.randomUUID();

        inventoryService.reserveInventory(bookingId, unknownId);

        List<InventoryOutboxEvent> events = outboxRepository.findAll();
        assertThat(events).hasSize(1);
        assertThat(events.getFirst().getEventType()).isEqualTo(InventoryEventType.INVENTORY_RESERVATION_FAILED);
    }

    @Test
    void reserveInventory_insufficientInventory_doesNotChangeQuantity() {
        inventoryRepository.save(new ResourceInventory(resourceId, 0));

        inventoryService.reserveInventory(bookingId, resourceId);

        ResourceInventory inventory = inventoryRepository.findById(resourceId).orElseThrow();
        assertThat(inventory.getAvailableQuantity()).isZero();
    }

    // ── releaseInventory — success ───────────────────────────────────────────

    @Test
    void releaseInventory_success_incrementsQuantityInDatabase() {
        inventoryRepository.save(new ResourceInventory(resourceId, 2));

        inventoryService.releaseInventory(bookingId, resourceId);

        ResourceInventory updated = inventoryRepository.findById(resourceId).orElseThrow();
        assertThat(updated.getAvailableQuantity()).isEqualTo(3);
    }

    @Test
    void releaseInventory_success_persistsOutboxEventWithReleasedType() {
        inventoryRepository.save(new ResourceInventory(resourceId, 0));

        inventoryService.releaseInventory(bookingId, resourceId);

        List<InventoryOutboxEvent> events = outboxRepository.findAll();
        assertThat(events).hasSize(1);
        InventoryOutboxEvent event = events.getFirst();
        assertThat(event.getEventType()).isEqualTo(InventoryEventType.INVENTORY_RELEASED);
        assertThat(event.getAggregateId()).isEqualTo(bookingId.toString());
        assertThat(event.isProcessed()).isFalse();
    }

    @Test
    void releaseInventory_success_payloadContainsReleasedStatus() {
        inventoryRepository.save(new ResourceInventory(resourceId, 0));

        inventoryService.releaseInventory(bookingId, resourceId);

        InventoryOutboxEvent event = outboxRepository.findAll().getFirst();
        assertThat(event.getPayload()).contains(InventoryEventType.INVENTORY_RELEASED.name());
        assertThat(event.getPayload()).contains(bookingId.toString());
    }

    @Test
    void releaseInventory_reserveThenRelease_quantityIsRestored() {
        inventoryRepository.save(new ResourceInventory(resourceId, 5));

        inventoryService.reserveInventory(bookingId, resourceId);
        outboxRepository.deleteAll();
        inventoryService.releaseInventory(bookingId, resourceId);

        ResourceInventory inventory = inventoryRepository.findById(resourceId).orElseThrow();
        assertThat(inventory.getAvailableQuantity()).isEqualTo(5);
    }

    // ── releaseInventory — failure ───────────────────────────────────────────

    @Test
    void releaseInventory_resourceNotFound_doesNotPersistOutboxEvent() {
        UUID unknownId = UUID.randomUUID();

        inventoryService.releaseInventory(bookingId, unknownId);

        assertThat(outboxRepository.count()).isZero();
    }
}
