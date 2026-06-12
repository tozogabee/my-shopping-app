package com.example.bookingservice.kafka;

import com.example.bookingservice.events.BookingOutboxEvent;
import com.example.bookingservice.events.repository.BookingOutboxEventRepository;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Slf4j
@Component
@ConditionalOnProperty(name = "booking.relay.enabled", havingValue = "true", matchIfMissing = true)
public class BookingOutboxRelay {

    private final BookingOutboxEventRepository outboxRepository;
    private final BookingKafkaProducer producer;

    public BookingOutboxRelay(BookingOutboxEventRepository outboxRepository,
                              BookingKafkaProducer producer) {
        this.outboxRepository = outboxRepository;
        this.producer = producer;
    }

    @PostConstruct
    void init() {
        log.info("BookingOutboxRelay started — polling every {}ms", "${booking.relay.fixed-delay-ms:5000}");
    }

    @Scheduled(fixedDelayString = "${booking.relay.fixed-delay-ms:5000}")
    @Transactional
    public void relay() {
        List<BookingOutboxEvent> events =
                this.outboxRepository.findTop50ByProcessedFalseOrderByCreatedAtAsc();
        log.info("Relay tick: found {} unprocessed outbox event(s)", events.size());
        if (events.isEmpty()) {
            return;
        }
        for (BookingOutboxEvent event : events) {
            this.producer.send(event);
            event.markAsProcessed();
            log.info("Event {} ({}) published and marked processed", event.getAggregateId(), event.getEventType());
        }
    }
}