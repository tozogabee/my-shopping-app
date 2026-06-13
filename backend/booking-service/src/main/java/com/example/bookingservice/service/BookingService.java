package com.example.bookingservice.service;

import com.example.bookingservice.api.dto.BookingDTO;
import com.example.bookingservice.api.dto.BookingRequest;
import com.example.bookingservice.events.BookingOutboxEvent;
import com.example.common.events.BookingEventType;
import com.example.bookingservice.events.repository.BookingOutboxEventRepository;
import com.example.bookingservice.exception.ResourceNotFoundException;
import com.example.bookingservice.mapper.BookingMapper;
import com.example.bookingservice.model.Booking;
import com.example.bookingservice.model.repository.BookingRepository;
import jakarta.transaction.Transactional;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import tools.jackson.databind.ObjectMapper;

import java.util.List;
import java.util.UUID;

@Service
@Slf4j
public class BookingService {

    private final BookingRepository bookingRepository;
    private final BookingOutboxEventRepository outboxRepository;
    private final ObjectMapper objectMapper;
    private final BookingMapper bookingMapper;

    public BookingService(BookingRepository bookingRepository,
                          BookingOutboxEventRepository outboxRepository,
                          ObjectMapper objectMapper,
                          BookingMapper bookingMapper) {
        this.bookingRepository = bookingRepository;
        this.outboxRepository = outboxRepository;
        this.objectMapper = objectMapper;
        this.bookingMapper = bookingMapper;
    }

    @Transactional
    public BookingDTO createBooking(UUID userId, BookingRequest request) {
        Booking booking = new Booking(userId, request.getResourceId());
        bookingRepository.save(booking);

        BookingDTO bookingDTO = bookingMapper.toDTO(booking);

        try {
            BookingOutboxEvent event = new BookingOutboxEvent(
                    bookingDTO.getId().toString(),
                    BookingEventType.BOOKING_CREATED,
                    objectMapper.writeValueAsString(bookingDTO)
            );
            outboxRepository.save(event);
        } catch (Exception e) {
            throw new RuntimeException("Failed to serialize booking outbox event", e);
        }

        return bookingDTO;
    }

    public BookingDTO getBookingById(UUID id) {
        Booking booking = bookingRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Booking not found: " + id));
        return bookingMapper.toDTO(booking);
    }

    public List<BookingDTO> getAllBookings() {
        return bookingRepository.findAll().stream()
                .map(bookingMapper::toDTO)
                .toList();
    }

    @Transactional
    public BookingDTO confirmBooking(UUID id) {
        Booking booking = bookingRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Booking not found: " + id));
        booking.confirm();
        bookingRepository.save(booking);

        BookingDTO bookingDTO = bookingMapper.toDTO(booking);

        try {
            BookingOutboxEvent event = new BookingOutboxEvent(
                    bookingDTO.getId().toString(),
                    BookingEventType.BOOKING_CONFIRMED,
                    objectMapper.writeValueAsString(bookingDTO)
            );
            outboxRepository.save(event);
        } catch (Exception e) {
            throw new RuntimeException("Failed to serialize booking outbox event", e);
        }

        return bookingDTO;
    }

    @Transactional
    public void deleteBookingById(UUID id) {
        Booking booking = bookingRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Booking not found: " + id));

        booking.cancel();

        BookingDTO bookingDTO = bookingMapper.toDTO(booking);

        try {
            BookingOutboxEvent event = new BookingOutboxEvent(
                    bookingDTO.getId().toString(),
                    BookingEventType.BOOKING_CANCELLED,
                    objectMapper.writeValueAsString(bookingDTO)
            );
            outboxRepository.save(event);
        } catch (Exception e) {
            throw new RuntimeException("Failed to serialize booking outbox event", e);
        }

        bookingRepository.deleteById(id);
    }

    @Transactional
    public void markOutboxEventAsProcessed(UUID eventId) {
        BookingOutboxEvent event = outboxRepository.findById(eventId)
                .orElseThrow(() -> new ResourceNotFoundException("Outbox event not found: " + eventId));
        event.markAsProcessed();
        outboxRepository.save(event);
    }

    @Transactional
    public BookingDTO cancelBooking(UUID id, String reason) {
        Booking booking = bookingRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Booking not found: " + id));

        // Entitás belső állapotának frissítése (PENDING -> CANCELLED)
        // Megjegyzés: Ehhez a Booking entitásban létre kell hoznod egy cancel() metódust!
        booking.cancel();
        bookingRepository.save(booking);

        BookingDTO bookingDTO = bookingMapper.toDTO(booking);

        try {
            BookingOutboxEvent event = new BookingOutboxEvent(
                    bookingDTO.getId().toString(),
                    BookingEventType.BOOKING_CANCELLED,
                    objectMapper.writeValueAsString(bookingDTO)
            );
            outboxRepository.save(event);
            log.info("Booking {} cancelled. Reason: {}", id, reason);
        } catch (Exception e) {
            throw new RuntimeException("Failed to serialize booking outbox event", e);
        }

        return bookingDTO;
    }
}