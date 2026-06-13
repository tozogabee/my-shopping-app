package com.example.bookingservice.kafka;

import com.example.bookingservice.events.saga.SagaFailureEvent;
import com.example.bookingservice.events.saga.SagaSuccessEvent;
import com.example.bookingservice.service.BookingService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class BookingKafkaConsumer {

    private final BookingService bookingService;

    public BookingKafkaConsumer(BookingService bookingService) {
        this.bookingService = bookingService;
    }

    @KafkaListener(topics = "saga-successful-events", groupId = "booking-service-group",
            containerFactory = "sagaSuccessFactory")
    public void onSagaSuccess(SagaSuccessEvent event) {
        try {
            log.info("Saga sikeres. Foglalás megerősítése: {}", event.aggregateId());
            bookingService.confirmBooking(event.aggregateId());
        } catch (Exception e) {
            log.error("Kritikus hiba a Saga megerősítésekor. aggregateId: {}", event.aggregateId(), e);
        }
    }

    @KafkaListener(topics = "saga-failed-events", groupId = "booking-service-group",
            containerFactory = "sagaFailureFactory")
    public void onSagaFailure(SagaFailureEvent event) {
        try {
            String reason = event.reason() != null ? event.reason() : "Ismeretlen hiba";
            log.warn("Saga elhasalt. Foglalás törlése: {}. Ok: {}", event.aggregateId(), reason);
            bookingService.cancelBooking(event.aggregateId(), reason);
        } catch (Exception e) {
            log.error("Hiba a kompenzáció során. aggregateId: {}", event.aggregateId(), e);
        }
    }
}