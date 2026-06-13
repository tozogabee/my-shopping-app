package com.example.bookingservice.events.saga;

import java.util.UUID;

public record SagaFailureEvent(UUID aggregateId, String reason) {}