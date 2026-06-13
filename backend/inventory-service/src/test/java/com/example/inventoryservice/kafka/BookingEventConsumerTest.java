package com.example.inventoryservice.kafka;

import com.example.common.events.BookingEventType;
import com.example.inventoryservice.kafka.event.BookingCreatedEvent;
import com.example.inventoryservice.service.InventoryService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThatNoException;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class BookingEventConsumerTest {

    @Mock
    private InventoryService inventoryService;

    @InjectMocks
    private BookingEventConsumer consumer;

    private static final UUID BOOKING_ID = UUID.randomUUID();
    private static final UUID RESOURCE_ID = UUID.randomUUID();

    // ── BOOKING_CREATED ──────────────────────────────────────────────────────

    @Test
    void handleBookingCreated_bookingCreatedEvent_callsReserveInventory() {
        BookingCreatedEvent event = new BookingCreatedEvent(BOOKING_ID, RESOURCE_ID);

        consumer.handleBookingCreated(event, BookingEventType.BOOKING_CREATED.name());

        verify(inventoryService).reserveInventory(BOOKING_ID, RESOURCE_ID);
        verify(inventoryService, never()).releaseInventory(BOOKING_ID, RESOURCE_ID);
    }

    // ── BOOKING_CANCELLED ────────────────────────────────────────────────────

    @Test
    void handleBookingCreated_bookingCancelledEvent_callsReleaseInventory() {
        BookingCreatedEvent event = new BookingCreatedEvent(BOOKING_ID, RESOURCE_ID);

        consumer.handleBookingCreated(event, BookingEventType.BOOKING_CANCELLED.name());

        verify(inventoryService).releaseInventory(BOOKING_ID, RESOURCE_ID);
        verify(inventoryService, never()).reserveInventory(BOOKING_ID, RESOURCE_ID);
    }

    // ── unknown event type ───────────────────────────────────────────────────

    @Test
    void handleBookingCreated_unknownEventType_callsNeitherService() {
        BookingCreatedEvent event = new BookingCreatedEvent(BOOKING_ID, RESOURCE_ID);

        consumer.handleBookingCreated(event, BookingEventType.BOOKING_CONFIRMED.name());

        verify(inventoryService, never()).reserveInventory(BOOKING_ID, RESOURCE_ID);
        verify(inventoryService, never()).releaseInventory(BOOKING_ID, RESOURCE_ID);
    }

    @Test
    void handleBookingCreated_nullEventType_callsNeitherService() {
        BookingCreatedEvent event = new BookingCreatedEvent(BOOKING_ID, RESOURCE_ID);

        assertThatNoException().isThrownBy(() -> consumer.handleBookingCreated(event, "UNKNOWN_TYPE"));

        verify(inventoryService, never()).reserveInventory(BOOKING_ID, RESOURCE_ID);
        verify(inventoryService, never()).releaseInventory(BOOKING_ID, RESOURCE_ID);
    }

    // ── error handling ───────────────────────────────────────────────────────

    @Test
    void handleBookingCreated_bookingCreatedEvent_passesCorrectIdsToService() {
        UUID specificBookingId = UUID.randomUUID();
        UUID specificResourceId = UUID.randomUUID();
        BookingCreatedEvent event = new BookingCreatedEvent(specificBookingId, specificResourceId);

        consumer.handleBookingCreated(event, BookingEventType.BOOKING_CREATED.name());

        verify(inventoryService).reserveInventory(specificBookingId, specificResourceId);
    }

    @Test
    void handleBookingCreated_bookingCancelledEvent_passesCorrectIdsToService() {
        UUID specificBookingId = UUID.randomUUID();
        UUID specificResourceId = UUID.randomUUID();
        BookingCreatedEvent event = new BookingCreatedEvent(specificBookingId, specificResourceId);

        consumer.handleBookingCreated(event, BookingEventType.BOOKING_CANCELLED.name());

        verify(inventoryService).releaseInventory(specificBookingId, specificResourceId);
    }
}
