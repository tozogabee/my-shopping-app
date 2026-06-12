package com.example.bookingservice.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.AccessLevel;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "bookings")
@Getter
public class Booking {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "user_id", nullable = false, updatable = false)
    private UUID userId;

    @Column(name = "resource_id", nullable = false, updatable = false)
    private UUID resourceId;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private BookingStatus status;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at")
    private Instant updatedAt;

    protected Booking() {}

    public Booking(UUID userId, UUID resourceId) {
        if (userId == null || resourceId == null) {
            throw new IllegalArgumentException("The userId or resourceId must not be null");
        }
        this.userId = userId;
        this.resourceId = resourceId;
        this.status = BookingStatus.PENDING;
        this.createdAt = Instant.now();
        this.updatedAt = Instant.now();
    }

    public void confirm() {
        if (this.status != BookingStatus.PENDING) throw new IllegalStateException("Hiba...");
        this.status = BookingStatus.CONFIRMED;
    }

}