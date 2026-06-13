package com.example.bookingservice.controller;

import com.example.bookingservice.api.dto.BookingDTO;
import com.example.bookingservice.api.dto.BookingRequest;
import com.example.bookingservice.api.dto.BookingResponse;
import com.example.bookingservice.api.dto.ErrorResponse;
import com.example.bookingservice.events.repository.BookingOutboxEventRepository;
import com.example.bookingservice.exception.UnauthorizedException;
import com.example.bookingservice.model.repository.BookingRepository;
import com.example.bookingservice.security.JwtUserResolver;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClient;
import tools.jackson.databind.ObjectMapper;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.when;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
class BookingControllerTest {

    private static final UUID FIXED_USER_ID = UUID.fromString("00000000-0000-0000-0000-000000000001");

    @LocalServerPort
    private int port;

    @Autowired
    private BookingRepository bookingRepository;

    @Autowired
    private BookingOutboxEventRepository outboxRepository;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private JwtUserResolver jwtUserResolver;

    private RestClient restClient;

    @BeforeEach
    void setUp() {
        when(jwtUserResolver.getCurrentUserId()).thenReturn(FIXED_USER_ID);
        restClient = RestClient.builder()
                .baseUrl("http://localhost:" + port)
                .build();
    }

    @AfterEach
    void cleanup() {
        outboxRepository.deleteAll();
        bookingRepository.deleteAll();
    }

    // ── POST /api/v1/bookings ────────────────────────────────────────────────

    @Test
    void createBooking_returns201WithPendingStatus() {
        BookingRequest request = new BookingRequest(UUID.randomUUID());

        ResponseEntity<BookingResponse> response = restClient.post()
                .uri("/api/v1/bookings")
                .contentType(MediaType.APPLICATION_JSON)
                .body(request)
                .retrieve()
                .toEntity(BookingResponse.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getId()).isNotNull();
        assertThat(response.getBody().getStatus()).isEqualTo(BookingResponse.StatusEnum.PENDING);
    }

    @Test
    void createBooking_userIdTakenFromJwt() {
        ResponseEntity<BookingResponse> response = restClient.post()
                .uri("/api/v1/bookings")
                .contentType(MediaType.APPLICATION_JSON)
                .body(new BookingRequest(UUID.randomUUID()))
                .retrieve()
                .toEntity(BookingResponse.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        UUID created = response.getBody().getId();
        BookingDTO dto = restClient.get()
                .uri("/api/v1/bookings/{id}", created)
                .retrieve()
                .body(BookingDTO.class);
        assertThat(dto.getUserId()).isEqualTo(FIXED_USER_ID);
    }

    // ── GET /api/v1/bookings ─────────────────────────────────────────────────

    @Test
    void getAllBookings_emptyDatabase_returns200WithEmptyList() {
        ResponseEntity<List<BookingDTO>> response = restClient.get()
                .uri("/api/v1/bookings")
                .retrieve()
                .toEntity(new ParameterizedTypeReference<>() {});

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isEmpty();
    }

    @Test
    void getAllBookings_withTwoBookings_returns200WithAll() {
        createBookingViaRest();
        createBookingViaRest();

        ResponseEntity<List<BookingDTO>> response = restClient.get()
                .uri("/api/v1/bookings")
                .retrieve()
                .toEntity(new ParameterizedTypeReference<>() {});

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).hasSize(2);
    }

    // ── GET /api/v1/bookings/{id} ────────────────────────────────────────────

    @Test
    void getBookingById_existingId_returns200WithBody() {
        BookingResponse created = createBookingViaRest();

        ResponseEntity<BookingDTO> response = restClient.get()
                .uri("/api/v1/bookings/{id}", created.getId())
                .retrieve()
                .toEntity(BookingDTO.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getId()).isEqualTo(created.getId());
        assertThat(response.getBody().getStatus()).isEqualTo(BookingDTO.StatusEnum.PENDING);
    }

    @Test
    void getBookingById_unknownId_returns404WithErrorResponse() throws Exception {
        HttpClientErrorException ex = assertThrows(
                HttpClientErrorException.class,
                () -> restClient.get()
                        .uri("/api/v1/bookings/{id}", UUID.randomUUID())
                        .retrieve()
                        .toBodilessEntity()
        );

        assertThat(ex.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        ErrorResponse error = objectMapper.readValue(ex.getResponseBodyAsString(), ErrorResponse.class);
        assertThat(error.getCode()).isEqualTo(404);
        assertThat(error.getMessage()).isNotBlank();
    }

    // ── DELETE /api/v1/bookings/{id} ─────────────────────────────────────────

    @Test
    void deleteBookingById_existingId_returns204AndBookingIsGone() {
        BookingResponse created = createBookingViaRest();

        ResponseEntity<Void> response = restClient.delete()
                .uri("/api/v1/bookings/{id}", created.getId())
                .retrieve()
                .toBodilessEntity();

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);
        assertThat(bookingRepository.findById(created.getId())).isEmpty();
    }

    @Test
    void deleteBookingById_unknownId_returns404WithErrorResponse() throws Exception {
        HttpClientErrorException ex = assertThrows(
                HttpClientErrorException.class,
                () -> restClient.delete()
                        .uri("/api/v1/bookings/{id}", UUID.randomUUID())
                        .retrieve()
                        .toBodilessEntity()
        );

        assertThat(ex.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        ErrorResponse error = objectMapper.readValue(ex.getResponseBodyAsString(), ErrorResponse.class);
        assertThat(error.getCode()).isEqualTo(404);
        assertThat(error.getMessage()).isNotBlank();
    }

    // ── security ─────────────────────────────────────────────────────────────

    @Test
    void createBooking_unauthorizedUser_returns401() {
        when(jwtUserResolver.getCurrentUserId()).thenThrow(new UnauthorizedException("No JWT"));

        HttpClientErrorException ex = assertThrows(
                HttpClientErrorException.class,
                () -> restClient.post()
                        .uri("/api/v1/bookings")
                        .contentType(MediaType.APPLICATION_JSON)
                        .body(new BookingRequest(UUID.randomUUID()))
                        .retrieve()
                        .toBodilessEntity()
        );

        assertThat(ex.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    }

    // ── helper ───────────────────────────────────────────────────────────────

    private BookingResponse createBookingViaRest() {
        return restClient.post()
                .uri("/api/v1/bookings")
                .contentType(MediaType.APPLICATION_JSON)
                .body(new BookingRequest(UUID.randomUUID()))
                .retrieve()
                .body(BookingResponse.class);
    }
}