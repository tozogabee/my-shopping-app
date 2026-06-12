package com.example.bookingservice.model.repository;

import com.example.bookingservice.model.Booking;
import com.example.bookingservice.model.BookingStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface BookingRepository extends JpaRepository<Booking, UUID> {

    List<Booking> findAllByUserId(UUID userId);

    List<Booking> findAllByStatus(BookingStatus status);

    List<Booking> findAllByResourceId(UUID resourceId);
}