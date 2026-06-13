package com.example.bookingservice.events;

import com.example.common.events.BookingEventType;
import com.example.common.outbox.OutboxEvent;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.ToString;

@Entity
@Table(name = "outbox_events")
@Getter
@ToString(callSuper = true)
public class BookingOutboxEvent extends OutboxEvent {

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 50)
    private BookingEventType eventType;

    protected BookingOutboxEvent() {}

    public BookingOutboxEvent(String aggregateId, BookingEventType eventType, String payload) {
        super(aggregateId, payload);
        this.eventType = eventType;
    }
}