package com.example.inventoryservice.kafka.event;

import java.util.UUID;

public record BookingCreatedEvent(UUID id, UUID resourceId) {}