package com.example.inventoryservice.kafka;

import com.example.common.events.BookingEventType;
import com.example.inventoryservice.kafka.event.BookingCreatedEvent;
import com.example.inventoryservice.service.InventoryService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Slf4j
@Component
public class BookingEventConsumer {

    private final InventoryService inventoryService;

    public BookingEventConsumer(InventoryService inventoryService) {
        this.inventoryService = inventoryService;
    }

    @KafkaListener(
            topics = "booking-events",
            groupId = "inventory-service-group",
            containerFactory = "bookingCreatedFactory"
    )
    public void handleBookingCreated(
            BookingCreatedEvent event,
            @Header("eventType") String eventType) {

        UUID bookingId = event.id();
        UUID resourceId = event.resourceId();

        if (BookingEventType.BOOKING_CREATED.name().equals(eventType)) {
            inventoryService.reserveInventory(bookingId, resourceId);
        }
        else if (BookingEventType.BOOKING_CANCELLED.name().equals(eventType)) {
            log.info("Booking cancelled, starting compensation: {}", bookingId);
            inventoryService.releaseInventory(bookingId, resourceId);
        }
    }
}