package com.example.inventoryservice.model;

import jakarta.persistence.*;
import lombok.Getter;

import java.util.UUID;

@Entity
@Table(name = "resource_inventory")
@Getter
public class ResourceInventory {

    @Id
    private UUID resourceId;

    @Column(nullable = false)
    private int availableQuantity;

    @Version
    private Long version;

    protected ResourceInventory() {}

    public ResourceInventory(UUID resourceId, int initialQuantity) {
        this.resourceId = resourceId;
        this.availableQuantity = initialQuantity;
    }

    public void reserve(int quantity) {
        if (this.availableQuantity < quantity) {
            throw new IllegalStateException("Insufficient inventory!");
        }
        this.availableQuantity -= quantity;
    }

    public void release(int quantity) {
        this.availableQuantity += quantity;
    }
}