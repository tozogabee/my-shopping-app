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
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import tools.jackson.databind.ObjectMapper;

import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class BookingServiceTest {

    @Mock
    private BookingRepository bookingRepository;

    @Mock
    private BookingOutboxEventRepository outboxRepository;

    @Mock
    private ObjectMapper objectMapper;

    @Mock
    private BookingMapper bookingMapper;

    @InjectMocks
    private BookingService bookingService;

    private static final UUID USER_ID = UUID.randomUUID();
    private static final UUID RESOURCE_ID = UUID.randomUUID();

    // ── createBooking ────────────────────────────────────────────────────────

    @Test
    void createBooking_returnsDTOWithPendingStatus() throws Exception {
        BookingDTO dto = new BookingDTO();
        dto.setId(UUID.randomUUID());
        dto.setStatus(BookingDTO.StatusEnum.PENDING);

        when(bookingMapper.toDTO(any())).thenReturn(dto);
        when(objectMapper.writeValueAsString(any())).thenReturn("{}");

        BookingDTO result = bookingService.createBooking(USER_ID, new BookingRequest(RESOURCE_ID));

        assertThat(result.getStatus()).isEqualTo(BookingDTO.StatusEnum.PENDING);
    }

    @Test
    void createBooking_savesBookingAndOutboxEvent() throws Exception {
        BookingDTO dto = new BookingDTO();
        dto.setId(UUID.randomUUID());

        when(bookingMapper.toDTO(any())).thenReturn(dto);
        when(objectMapper.writeValueAsString(any())).thenReturn("{}");

        bookingService.createBooking(USER_ID, new BookingRequest(RESOURCE_ID));

        verify(bookingRepository).save(any(Booking.class));
        verify(outboxRepository).save(any(BookingOutboxEvent.class));
    }

    @Test
    void createBooking_outboxEventHasCorrectAggregateIdAndEventType() throws Exception {
        UUID bookingId = UUID.randomUUID();
        BookingDTO dto = new BookingDTO();
        dto.setId(bookingId);

        when(bookingMapper.toDTO(any())).thenReturn(dto);
        when(objectMapper.writeValueAsString(any())).thenReturn("{}");

        bookingService.createBooking(USER_ID, new BookingRequest(RESOURCE_ID));

        ArgumentCaptor<BookingOutboxEvent> captor = ArgumentCaptor.forClass(BookingOutboxEvent.class);
        verify(outboxRepository).save(captor.capture());
        assertThat(captor.getValue().getAggregateId()).isEqualTo(bookingId.toString());
        assertThat(captor.getValue().getEventType()).isEqualTo(BookingEventType.BOOKING_CREATED);
        assertThat(captor.getValue().isProcessed()).isFalse();
    }

    @Test
    void createBooking_nullUserId_throwsIllegalArgumentException() {
        assertThatThrownBy(() -> bookingService.createBooking(null, new BookingRequest(RESOURCE_ID)))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void createBooking_nullResourceId_throwsIllegalArgumentException() {
        assertThatThrownBy(() -> bookingService.createBooking(USER_ID, new BookingRequest(null)))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void createBooking_serializationFails_propagatesException() throws Exception {
        BookingDTO dto = new BookingDTO();
        dto.setId(UUID.randomUUID());

        when(bookingMapper.toDTO(any())).thenReturn(dto);
        when(objectMapper.writeValueAsString(any())).thenThrow(new RuntimeException("serialization failed"));

        assertThatThrownBy(() -> bookingService.createBooking(USER_ID, new BookingRequest(RESOURCE_ID)))
                .isInstanceOf(RuntimeException.class)
                .hasMessage("Failed to serialize booking outbox event")
                .cause().hasMessage("serialization failed");
    }

    // ── confirmBooking ───────────────────────────────────────────────────────

    @Test
    void confirmBooking_returnsDTOWithConfirmedStatus() throws Exception {
        UUID id = UUID.randomUUID();
        Booking booking = new Booking(USER_ID, RESOURCE_ID);
        BookingDTO dto = new BookingDTO();
        dto.setId(id);
        dto.setStatus(BookingDTO.StatusEnum.CONFIRMED);

        when(bookingRepository.findById(id)).thenReturn(Optional.of(booking));
        when(bookingMapper.toDTO(booking)).thenReturn(dto);
        when(objectMapper.writeValueAsString(any())).thenReturn("{}");

        BookingDTO result = bookingService.confirmBooking(id);

        assertThat(result.getStatus()).isEqualTo(BookingDTO.StatusEnum.CONFIRMED);
    }

    @Test
    void confirmBooking_savesBookingAndOutboxEvent() throws Exception {
        UUID id = UUID.randomUUID();
        Booking booking = new Booking(USER_ID, RESOURCE_ID);
        BookingDTO dto = new BookingDTO();
        dto.setId(id);

        when(bookingRepository.findById(id)).thenReturn(Optional.of(booking));
        when(bookingMapper.toDTO(booking)).thenReturn(dto);
        when(objectMapper.writeValueAsString(any())).thenReturn("{}");

        bookingService.confirmBooking(id);

        verify(bookingRepository).save(booking);
        verify(outboxRepository).save(any(BookingOutboxEvent.class));
    }

    @Test
    void confirmBooking_outboxEventHasConfirmedEventType() throws Exception {
        UUID id = UUID.randomUUID();
        Booking booking = new Booking(USER_ID, RESOURCE_ID);
        BookingDTO dto = new BookingDTO();
        dto.setId(id);

        when(bookingRepository.findById(id)).thenReturn(Optional.of(booking));
        when(bookingMapper.toDTO(booking)).thenReturn(dto);
        when(objectMapper.writeValueAsString(any())).thenReturn("{}");

        bookingService.confirmBooking(id);

        ArgumentCaptor<BookingOutboxEvent> captor = ArgumentCaptor.forClass(BookingOutboxEvent.class);
        verify(outboxRepository).save(captor.capture());
        assertThat(captor.getValue().getEventType()).isEqualTo(BookingEventType.BOOKING_CONFIRMED);
        assertThat(captor.getValue().getAggregateId()).isEqualTo(id.toString());
    }

    @Test
    void confirmBooking_notFound_throwsResourceNotFoundException() {
        UUID id = UUID.randomUUID();
        when(bookingRepository.findById(id)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> bookingService.confirmBooking(id))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining(id.toString());
    }

    // ── getBookingById ───────────────────────────────────────────────────────

    @Test
    void getBookingById_returnsDTO() {
        UUID bookingId = UUID.randomUUID();
        Booking booking = new Booking(USER_ID, RESOURCE_ID);
        BookingDTO expected = new BookingDTO();
        expected.setId(bookingId);

        when(bookingRepository.findById(bookingId)).thenReturn(Optional.of(booking));
        when(bookingMapper.toDTO(booking)).thenReturn(expected);

        BookingDTO result = bookingService.getBookingById(bookingId);

        assertThat(result.getId()).isEqualTo(bookingId);
    }

    @Test
    void getBookingById_notFound_throwsResourceNotFoundException() {
        UUID id = UUID.randomUUID();
        when(bookingRepository.findById(id)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> bookingService.getBookingById(id))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining(id.toString());
    }

    // ── getAllBookings ────────────────────────────────────────────────────────

    @Test
    void getAllBookings_returnsMappedList() {
        UUID bookingId = UUID.randomUUID();
        Booking booking = new Booking(USER_ID, RESOURCE_ID);
        BookingDTO dto = new BookingDTO();
        dto.setId(bookingId);

        when(bookingRepository.findAll()).thenReturn(List.of(booking));
        when(bookingMapper.toDTO(booking)).thenReturn(dto);

        List<BookingDTO> result = bookingService.getAllBookings();

        assertThat(result).hasSize(1);
        assertThat(result.getFirst().getId()).isEqualTo(bookingId);
    }

    @Test
    void getAllBookings_noBookings_returnsEmptyList() {
        when(bookingRepository.findAll()).thenReturn(Collections.emptyList());

        assertThat(bookingService.getAllBookings()).isEmpty();
    }

    // ── deleteBookingById ────────────────────────────────────────────────────

    @Test
    void deleteBookingById_publishesCancelledEventAndDeletesBooking() throws Exception {
        UUID id = UUID.randomUUID();
        Booking booking = new Booking(USER_ID, RESOURCE_ID);
        BookingDTO dto = new BookingDTO();
        dto.setId(id);
        dto.setStatus(BookingDTO.StatusEnum.CANCELLED);

        when(bookingRepository.findById(id)).thenReturn(Optional.of(booking));
        when(bookingMapper.toDTO(booking)).thenReturn(dto);
        when(objectMapper.writeValueAsString(dto)).thenReturn("{\"id\":\"" + id + "\",\"status\":\"CANCELLED\"}");

        bookingService.deleteBookingById(id);

        ArgumentCaptor<BookingOutboxEvent> captor = ArgumentCaptor.forClass(BookingOutboxEvent.class);
        verify(outboxRepository).save(captor.capture());
        assertThat(captor.getValue().getEventType()).isEqualTo(BookingEventType.BOOKING_CANCELLED);
        assertThat(captor.getValue().getAggregateId()).isEqualTo(id.toString());
        assertThat(captor.getValue().getPayload()).contains("\"status\":\"CANCELLED\"");

        verify(bookingRepository).deleteById(id);
    }

    @Test
    void deleteBookingById_notFound_throwsResourceNotFoundException() {
        UUID id = UUID.randomUUID();
        when(bookingRepository.findById(id)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> bookingService.deleteBookingById(id))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining(id.toString());

        verify(bookingRepository, never()).deleteById(any());
        verify(outboxRepository, never()).save(any());
    }

    // ── cancelBooking ────────────────────────────────────────────────────────

    @Test
    void cancelBooking_returnsDTOWithCancelledStatus() throws Exception {
        UUID id = UUID.randomUUID();
        Booking booking = new Booking(USER_ID, RESOURCE_ID);
        BookingDTO dto = new BookingDTO();
        dto.setId(id);
        dto.setStatus(BookingDTO.StatusEnum.CANCELLED);

        when(bookingRepository.findById(id)).thenReturn(Optional.of(booking));
        when(bookingMapper.toDTO(booking)).thenReturn(dto);
        when(objectMapper.writeValueAsString(any())).thenReturn("{}");

        BookingDTO result = bookingService.cancelBooking(id, "no stock");

        assertThat(result.getStatus()).isEqualTo(BookingDTO.StatusEnum.CANCELLED);
    }

    @Test
    void cancelBooking_savesBookingAndOutboxEventWithCancelledType() throws Exception {
        UUID id = UUID.randomUUID();
        Booking booking = new Booking(USER_ID, RESOURCE_ID);
        BookingDTO dto = new BookingDTO();
        dto.setId(id);

        when(bookingRepository.findById(id)).thenReturn(Optional.of(booking));
        when(bookingMapper.toDTO(booking)).thenReturn(dto);
        when(objectMapper.writeValueAsString(any())).thenReturn("{}");

        bookingService.cancelBooking(id, "timeout");

        verify(bookingRepository).save(booking);
        ArgumentCaptor<BookingOutboxEvent> captor = ArgumentCaptor.forClass(BookingOutboxEvent.class);
        verify(outboxRepository).save(captor.capture());
        assertThat(captor.getValue().getEventType()).isEqualTo(BookingEventType.BOOKING_CANCELLED);
    }

    @Test
    void cancelBooking_notFound_throwsResourceNotFoundException() {
        UUID id = UUID.randomUUID();
        when(bookingRepository.findById(id)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> bookingService.cancelBooking(id, "reason"))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining(id.toString());
    }

    // ── markOutboxEventAsProcessed ───────────────────────────────────────────

    @Test
    void markOutboxEventAsProcessed_savesEventWithProcessedTrue() {
        UUID eventId = UUID.randomUUID();
        BookingOutboxEvent event = new BookingOutboxEvent("agg-id", BookingEventType.BOOKING_CREATED, "{}");
        when(outboxRepository.findById(eventId)).thenReturn(Optional.of(event));

        bookingService.markOutboxEventAsProcessed(eventId);

        assertThat(event.isProcessed()).isTrue();
        verify(outboxRepository).save(event);
    }

    @Test
    void markOutboxEventAsProcessed_notFound_throwsResourceNotFoundException() {
        UUID eventId = UUID.randomUUID();
        when(outboxRepository.findById(eventId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> bookingService.markOutboxEventAsProcessed(eventId))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining(eventId.toString());

        verify(outboxRepository, never()).save(any());
    }
}