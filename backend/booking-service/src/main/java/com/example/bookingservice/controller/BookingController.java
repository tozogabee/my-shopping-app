package com.example.bookingservice.controller;

import com.example.bookingservice.api.BookingsApi;
import com.example.bookingservice.api.dto.BookingDTO;
import com.example.bookingservice.api.dto.BookingRequest;
import com.example.bookingservice.api.dto.BookingResponse;
import com.example.bookingservice.mapper.BookingMapper;
import com.example.bookingservice.security.JwtUserResolver;
import com.example.bookingservice.service.BookingService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
public class BookingController implements BookingsApi {

    private final BookingService bookingService;
    private final BookingMapper bookingMapper;
    private final JwtUserResolver jwtUserResolver;

    public BookingController(BookingService bookingService, BookingMapper bookingMapper, JwtUserResolver jwtUserResolver) {
        this.bookingService = bookingService;
        this.bookingMapper = bookingMapper;
        this.jwtUserResolver = jwtUserResolver;
    }

    @Override
    public ResponseEntity<BookingResponse> createBooking(BookingRequest bookingRequest) {
        UUID userId = jwtUserResolver.getCurrentUserId();
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(bookingMapper.toResponse(bookingService.createBooking(userId, bookingRequest)));
    }

    @Override
    public ResponseEntity<BookingDTO> getBookingById(UUID id) {
        return ResponseEntity.ok(bookingService.getBookingById(id));
    }

    @Override
    public ResponseEntity<List<BookingDTO>> getAllBookings() {
        return ResponseEntity.ok(this.bookingService.getAllBookings());
    }

    @Override
    public ResponseEntity<Void> deleteBookingById(UUID id) {
        this.bookingService.deleteBookingById(id);
        return ResponseEntity.noContent().build();
    }
}