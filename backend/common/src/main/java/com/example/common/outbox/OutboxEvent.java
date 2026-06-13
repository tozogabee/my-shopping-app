package com.example.common.outbox;

import jakarta.persistence.*;
import lombok.Getter;

import java.time.Instant;
import java.util.UUID;

@MappedSuperclass
@Getter
public abstract class OutboxEvent {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false)
    private String aggregateId;

    @Column(nullable = false, length = 4000)
    private String payload;

    @Column(nullable = false)
    private boolean processed;

    @Column(nullable = false, updatable = false)
    private Instant createdAt;

    protected OutboxEvent() {}

    protected OutboxEvent(String aggregateId, String payload) {
        this.aggregateId = aggregateId;
        this.payload = payload;
        this.processed = false;
        this.createdAt = Instant.now();
    }

    public void markAsProcessed() {
        this.processed = true;
    }
}