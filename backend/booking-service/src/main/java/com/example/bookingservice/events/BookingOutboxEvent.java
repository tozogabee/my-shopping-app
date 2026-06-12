package com.example.bookingservice.events;

import com.example.bookingservice.events.enums.BookingEventType;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "outbox_events")
@Getter
@ToString
public class BookingOutboxEvent {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false)
    private String aggregateId; // A Booking UUID-ja

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 50)
    private BookingEventType eventType;

    @Column(nullable = false, length = 4000)
    private String payload;

    @Column(nullable = false)
    private boolean processed;

    @Column(nullable = false, updatable = false)
    private Instant createdAt;

    protected BookingOutboxEvent() {}

    public BookingOutboxEvent(String aggregateId, BookingEventType eventType, String payload) {
        this.aggregateId = aggregateId;
        this.eventType = eventType;
        this.payload = payload;
        this.processed = false;
        this.createdAt = Instant.now();
    }

    public void markAsProcessed() {
        this.processed = true;
    }

}
