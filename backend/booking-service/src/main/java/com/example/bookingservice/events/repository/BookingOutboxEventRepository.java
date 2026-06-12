package com.example.bookingservice.events.repository;

import com.example.bookingservice.events.BookingOutboxEvent;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface BookingOutboxEventRepository extends JpaRepository<BookingOutboxEvent, UUID> {
    
    List<BookingOutboxEvent> findTop50ByProcessedFalseOrderByCreatedAtAsc();
}