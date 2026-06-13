package com.example.inventoryservice.model;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface ResourceInventoryRepository extends JpaRepository<ResourceInventory, UUID> {

}
