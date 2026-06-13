package com.example.bookingservice.events.saga;

import java.util.UUID;

public record SagaSuccessEvent(UUID aggregateId) {}