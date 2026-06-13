package com.example.inventoryservice.event;

import com.example.common.outbox.OutboxEvent;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.ToString;

@Entity
@Table(name = "inventory_outbox_events")
@Getter
@ToString(callSuper = true)
public class InventoryOutboxEvent extends OutboxEvent {

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 50)
    private InventoryEventType eventType;

    protected InventoryOutboxEvent() {}

    public InventoryOutboxEvent(String aggregateId, InventoryEventType eventType, String payload) {
        super(aggregateId, payload);
        this.eventType = eventType;
    }
}