package com.example.bookingservice.service;

import com.example.bookingservice.api.dto.BookingDTO;
import com.example.bookingservice.api.dto.BookingRequest;
import com.example.bookingservice.events.BookingOutboxEvent;
import com.example.bookingservice.events.enums.BookingEventType;
import com.example.bookingservice.events.repository.BookingOutboxEventRepository;
import com.example.bookingservice.exception.ResourceNotFoundException;
import com.example.bookingservice.model.Booking;
import com.example.bookingservice.model.BookingStatus;
import com.example.bookingservice.model.repository.BookingRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
@ActiveProfiles("test")
class BookingServiceComponentTest {

    @Autowired
    private BookingService bookingService;

    @Autowired
    private BookingRepository bookingRepository;

    @Autowired
    private BookingOutboxEventRepository outboxRepository;

    @AfterEach
    void cleanup() {
        outboxRepository.deleteAll();
        bookingRepository.deleteAll();
    }

    // ── createBooking ────────────────────────────────────────────────────────

    @Test
    void createBooking_persistsBookingInDatabase() {
        UUID userId = UUID.randomUUID();
        UUID resourceId = UUID.randomUUID();
        BookingRequest request = new BookingRequest(userId, resourceId);

        BookingDTO dto = bookingService.createBooking(request);

        Optional<Booking> saved = bookingRepository.findById(dto.getId());
        assertThat(saved).isPresent();
        assertThat(saved.get().getUserId()).isEqualTo(userId);
        assertThat(saved.get().getResourceId()).isEqualTo(resourceId);
    }

    @Test
    void createBooking_persistsOutboxEvent() {
        UUID userId = UUID.randomUUID();
        UUID resourceId = UUID.randomUUID();

        BookingDTO dto = bookingService.createBooking(new BookingRequest(userId, resourceId));

        List<BookingOutboxEvent> events = outboxRepository.findAll();
        assertThat(events).hasSize(1);
        BookingOutboxEvent event = events.getFirst();
        assertThat(event.getAggregateId()).isEqualTo(dto.getId().toString());
        assertThat(event.getEventType()).isEqualTo(BookingEventType.BOOKING_CREATED);
        assertThat(event.isProcessed()).isFalse();
    }

    @Test
    void createBooking_outboxPayloadContainsBookingData() {
        UUID userId = UUID.randomUUID();
        UUID resourceId = UUID.randomUUID();

        bookingService.createBooking(new BookingRequest(userId, resourceId));

        BookingOutboxEvent event = outboxRepository.findAll().getFirst();
        assertThat(event.getPayload()).contains(userId.toString());
        assertThat(event.getPayload()).contains(resourceId.toString());
        assertThat(event.getPayload()).containsIgnoringCase("PENDING");
    }

    @Test
    void createBooking_dtoHasPendingStatusAndNonNullId() {
        BookingDTO dto = bookingService.createBooking(new BookingRequest(UUID.randomUUID(), UUID.randomUUID()));

        assertThat(dto.getId()).isNotNull();
        assertThat(dto.getStatus()).isEqualTo(BookingDTO.StatusEnum.PENDING);
    }

    @Test
    void createBooking_nullUserId_doesNotPersistAnything() {
        assertThatThrownBy(() -> bookingService.createBooking(new BookingRequest(null, UUID.randomUUID())))
                .isInstanceOf(IllegalArgumentException.class);

        assertThat(bookingRepository.count()).isZero();
        assertThat(outboxRepository.count()).isZero();
    }

    @Test
    void markOutboxEventAsProcessed_setsProcessedFlag() {
        bookingService.createBooking(new BookingRequest(UUID.randomUUID(), UUID.randomUUID()));

        BookingOutboxEvent event = outboxRepository.findAll().getFirst();
        assertThat(event.isProcessed()).isFalse();

        bookingService.markOutboxEventAsProcessed(event.getId());

        BookingOutboxEvent updated = outboxRepository.findById(event.getId()).orElseThrow();
        assertThat(updated.isProcessed()).isTrue();
    }

    @Test
    void markOutboxEventAsProcessed_notFound_throwsResourceNotFoundException() {
        assertThatThrownBy(() -> bookingService.markOutboxEventAsProcessed(UUID.randomUUID()))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    // ── confirmBooking ───────────────────────────────────────────────────────

    @Test
    void confirmBooking_changesStatusToConfirmedInDatabase() {
        BookingDTO created = bookingService.createBooking(new BookingRequest(UUID.randomUUID(), UUID.randomUUID()));

        BookingDTO confirmed = bookingService.confirmBooking(created.getId());

        assertThat(confirmed.getStatus()).isEqualTo(BookingDTO.StatusEnum.CONFIRMED);
        Booking booking = bookingRepository.findById(created.getId()).orElseThrow();
        assertThat(booking.getStatus()).isEqualTo(BookingStatus.CONFIRMED);
    }

    @Test
    void confirmBooking_persistsOutboxEventWithConfirmedType() {
        BookingDTO created = bookingService.createBooking(new BookingRequest(UUID.randomUUID(), UUID.randomUUID()));
        outboxRepository.deleteAll();

        bookingService.confirmBooking(created.getId());

        List<BookingOutboxEvent> events = outboxRepository.findAll();
        assertThat(events).hasSize(1);
        assertThat(events.getFirst().getEventType()).isEqualTo(BookingEventType.BOOKING_CONFIRMED);
        assertThat(events.getFirst().getAggregateId()).isEqualTo(created.getId().toString());
    }

    @Test
    void confirmBooking_notFound_throwsResourceNotFoundException() {
        assertThatThrownBy(() -> bookingService.confirmBooking(UUID.randomUUID()))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void confirmBooking_alreadyConfirmed_throwsIllegalStateException() {
        BookingDTO created = bookingService.createBooking(new BookingRequest(UUID.randomUUID(), UUID.randomUUID()));
        bookingService.confirmBooking(created.getId());

        assertThatThrownBy(() -> bookingService.confirmBooking(created.getId()))
                .isInstanceOf(IllegalStateException.class);
    }

    // ── getBookingById ───────────────────────────────────────────────────────

    @Test
    void getBookingById_returnsCorrectDTO() {
        UUID userId = UUID.randomUUID();
        UUID resourceId = UUID.randomUUID();
        BookingDTO created = bookingService.createBooking(new BookingRequest(userId, resourceId));

        BookingDTO found = bookingService.getBookingById(created.getId());

        assertThat(found.getId()).isEqualTo(created.getId());
        assertThat(found.getUserId()).isEqualTo(userId);
        assertThat(found.getResourceId()).isEqualTo(resourceId);
        assertThat(found.getStatus()).isEqualTo(BookingDTO.StatusEnum.PENDING);
    }

    @Test
    void getBookingById_notFound_throwsResourceNotFoundException() {
        assertThatThrownBy(() -> bookingService.getBookingById(UUID.randomUUID()))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    // ── getAllBookings ────────────────────────────────────────────────────────

    @Test
    void getAllBookings_returnsAllPersistedBookings() {
        bookingService.createBooking(new BookingRequest(UUID.randomUUID(), UUID.randomUUID()));
        bookingService.createBooking(new BookingRequest(UUID.randomUUID(), UUID.randomUUID()));

        assertThat(bookingService.getAllBookings()).hasSize(2);
    }

    @Test
    void getAllBookings_noBookings_returnsEmptyList() {
        assertThat(bookingService.getAllBookings()).isEmpty();
    }

    // ── deleteBookingById ────────────────────────────────────────────────────

    @Test
    void deleteBookingById_removesBookingFromDatabase() {
        BookingDTO created = bookingService.createBooking(new BookingRequest(UUID.randomUUID(), UUID.randomUUID()));

        bookingService.deleteBookingById(created.getId());

        assertThat(bookingRepository.findById(created.getId())).isEmpty();
    }

    @Test
    void deleteBookingById_notFound_throwsResourceNotFoundException() {
        assertThatThrownBy(() -> bookingService.deleteBookingById(UUID.randomUUID()))
                .isInstanceOf(ResourceNotFoundException.class);
    }
}