package com.example.inventoryservice.service;

import com.example.inventoryservice.event.InventoryEventType;
import com.example.inventoryservice.event.InventoryOutboxEvent;
import com.example.inventoryservice.event.InventoryOutboxEventRepository;
import com.example.inventoryservice.model.ResourceInventory;
import com.example.inventoryservice.model.ResourceInventoryRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import tools.jackson.databind.json.JsonMapper;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class InventoryServiceTest {

    @Mock
    private ResourceInventoryRepository inventoryRepository;

    @Mock
    private InventoryOutboxEventRepository outboxRepository;

    private InventoryService inventoryService;

    private static final UUID BOOKING_ID = UUID.randomUUID();
    private static final UUID RESOURCE_ID = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        inventoryService = new InventoryService(inventoryRepository, outboxRepository, JsonMapper.builder().build());
    }

    // ── reserveInventory — success ───────────────────────────────────────────

    @Test
    void reserveInventory_success_savesInventoryAndOutboxEvent() {
        ResourceInventory inventory = new ResourceInventory(RESOURCE_ID, 5);
        when(inventoryRepository.findById(RESOURCE_ID)).thenReturn(Optional.of(inventory));

        inventoryService.reserveInventory(BOOKING_ID, RESOURCE_ID);

        verify(inventoryRepository).save(inventory);
        verify(outboxRepository).save(any(InventoryOutboxEvent.class));
    }

    @Test
    void reserveInventory_success_outboxEventHasReservedTypeAndCorrectAggregateId() {
        ResourceInventory inventory = new ResourceInventory(RESOURCE_ID, 5);
        when(inventoryRepository.findById(RESOURCE_ID)).thenReturn(Optional.of(inventory));

        inventoryService.reserveInventory(BOOKING_ID, RESOURCE_ID);

        ArgumentCaptor<InventoryOutboxEvent> captor = ArgumentCaptor.forClass(InventoryOutboxEvent.class);
        verify(outboxRepository).save(captor.capture());
        assertThat(captor.getValue().getEventType()).isEqualTo(InventoryEventType.INVENTORY_RESERVED);
        assertThat(captor.getValue().getAggregateId()).isEqualTo(BOOKING_ID.toString());
        assertThat(captor.getValue().isProcessed()).isFalse();
    }

    @Test
    void reserveInventory_success_payloadContainsReservedStatusAndIds() {
        ResourceInventory inventory = new ResourceInventory(RESOURCE_ID, 5);
        when(inventoryRepository.findById(RESOURCE_ID)).thenReturn(Optional.of(inventory));

        inventoryService.reserveInventory(BOOKING_ID, RESOURCE_ID);

        ArgumentCaptor<InventoryOutboxEvent> captor = ArgumentCaptor.forClass(InventoryOutboxEvent.class);
        verify(outboxRepository).save(captor.capture());
        String payload = captor.getValue().getPayload();
        assertThat(payload).contains(BOOKING_ID.toString());
        assertThat(payload).contains(RESOURCE_ID.toString());
        assertThat(payload).contains(InventoryEventType.INVENTORY_RESERVED.name());
    }

    @Test
    void reserveInventory_success_decrementsAvailableQuantity() {
        ResourceInventory inventory = new ResourceInventory(RESOURCE_ID, 3);
        when(inventoryRepository.findById(RESOURCE_ID)).thenReturn(Optional.of(inventory));

        inventoryService.reserveInventory(BOOKING_ID, RESOURCE_ID);

        assertThat(inventory.getAvailableQuantity()).isEqualTo(2);
    }

    // ── reserveInventory — failure ───────────────────────────────────────────

    @Test
    void reserveInventory_resourceNotFound_savesFailedOutboxEvent() {
        when(inventoryRepository.findById(RESOURCE_ID)).thenReturn(Optional.empty());

        inventoryService.reserveInventory(BOOKING_ID, RESOURCE_ID);

        ArgumentCaptor<InventoryOutboxEvent> captor = ArgumentCaptor.forClass(InventoryOutboxEvent.class);
        verify(outboxRepository).save(captor.capture());
        assertThat(captor.getValue().getEventType()).isEqualTo(InventoryEventType.INVENTORY_RESERVATION_FAILED);
        assertThat(captor.getValue().getAggregateId()).isEqualTo(BOOKING_ID.toString());
    }

    @Test
    void reserveInventory_insufficientInventory_savesFailedOutboxEvent() {
        ResourceInventory inventory = new ResourceInventory(RESOURCE_ID, 0);
        when(inventoryRepository.findById(RESOURCE_ID)).thenReturn(Optional.of(inventory));

        inventoryService.reserveInventory(BOOKING_ID, RESOURCE_ID);

        ArgumentCaptor<InventoryOutboxEvent> captor = ArgumentCaptor.forClass(InventoryOutboxEvent.class);
        verify(outboxRepository).save(captor.capture());
        assertThat(captor.getValue().getEventType()).isEqualTo(InventoryEventType.INVENTORY_RESERVATION_FAILED);
    }

    @Test
    void reserveInventory_failure_payloadContainsFailedStatus() {
        when(inventoryRepository.findById(RESOURCE_ID)).thenReturn(Optional.empty());

        inventoryService.reserveInventory(BOOKING_ID, RESOURCE_ID);

        ArgumentCaptor<InventoryOutboxEvent> captor = ArgumentCaptor.forClass(InventoryOutboxEvent.class);
        verify(outboxRepository).save(captor.capture());
        assertThat(captor.getValue().getPayload()).contains(InventoryEventType.INVENTORY_RESERVATION_FAILED.name());
    }

    @Test
    void reserveInventory_failure_doesNotSaveInventory() {
        when(inventoryRepository.findById(RESOURCE_ID)).thenReturn(Optional.empty());

        inventoryService.reserveInventory(BOOKING_ID, RESOURCE_ID);

        verify(inventoryRepository, never()).save(any());
    }

    // ── releaseInventory — success ───────────────────────────────────────────

    @Test
    void releaseInventory_success_savesInventoryAndOutboxEvent() {
        ResourceInventory inventory = new ResourceInventory(RESOURCE_ID, 0);
        when(inventoryRepository.findById(RESOURCE_ID)).thenReturn(Optional.of(inventory));

        inventoryService.releaseInventory(BOOKING_ID, RESOURCE_ID);

        verify(inventoryRepository).save(inventory);
        verify(outboxRepository).save(any(InventoryOutboxEvent.class));
    }

    @Test
    void releaseInventory_success_outboxEventHasReleasedTypeAndCorrectAggregateId() {
        ResourceInventory inventory = new ResourceInventory(RESOURCE_ID, 0);
        when(inventoryRepository.findById(RESOURCE_ID)).thenReturn(Optional.of(inventory));

        inventoryService.releaseInventory(BOOKING_ID, RESOURCE_ID);

        ArgumentCaptor<InventoryOutboxEvent> captor = ArgumentCaptor.forClass(InventoryOutboxEvent.class);
        verify(outboxRepository).save(captor.capture());
        assertThat(captor.getValue().getEventType()).isEqualTo(InventoryEventType.INVENTORY_RELEASED);
        assertThat(captor.getValue().getAggregateId()).isEqualTo(BOOKING_ID.toString());
    }

    @Test
    void releaseInventory_success_incrementsAvailableQuantity() {
        ResourceInventory inventory = new ResourceInventory(RESOURCE_ID, 2);
        when(inventoryRepository.findById(RESOURCE_ID)).thenReturn(Optional.of(inventory));

        inventoryService.releaseInventory(BOOKING_ID, RESOURCE_ID);

        assertThat(inventory.getAvailableQuantity()).isEqualTo(3);
    }

    @Test
    void releaseInventory_success_payloadContainsReleasedStatusAndIds() {
        ResourceInventory inventory = new ResourceInventory(RESOURCE_ID, 0);
        when(inventoryRepository.findById(RESOURCE_ID)).thenReturn(Optional.of(inventory));

        inventoryService.releaseInventory(BOOKING_ID, RESOURCE_ID);

        ArgumentCaptor<InventoryOutboxEvent> captor = ArgumentCaptor.forClass(InventoryOutboxEvent.class);
        verify(outboxRepository).save(captor.capture());
        String payload = captor.getValue().getPayload();
        assertThat(payload).contains(BOOKING_ID.toString());
        assertThat(payload).contains(RESOURCE_ID.toString());
        assertThat(payload).contains(InventoryEventType.INVENTORY_RELEASED.name());
    }

    // ── releaseInventory — failure ───────────────────────────────────────────

    @Test
    void releaseInventory_resourceNotFound_doesNotSaveOutboxEvent() {
        when(inventoryRepository.findById(RESOURCE_ID)).thenReturn(Optional.empty());

        inventoryService.releaseInventory(BOOKING_ID, RESOURCE_ID);

        verify(outboxRepository, never()).save(any());
    }
}
