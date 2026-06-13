package com.example.inventoryservice.kafka;

import com.example.common.events.BookingEventType;
import com.example.inventoryservice.kafka.event.BookingCreatedEvent;
import com.example.inventoryservice.service.InventoryService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.stereotype.Component;

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

        if (!BookingEventType.BOOKING_CREATED.name().equals(eventType)) {
            return;
        }

        try {
            inventoryService.reserveInventory(event.id(), event.resourceId());
        } catch (Exception e) {
            log.error("Kritikus hiba az esemény feldolgozásakor: bookingId={}", event.id(), e);
        }
    }
}