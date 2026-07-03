package com.jeffgicharu.daraja.events;

import com.jeffgicharu.daraja.domain.OutboxEvent;
import com.jeffgicharu.daraja.repository.OutboxEventRepository;
import java.time.Clock;
import java.util.List;
import java.util.concurrent.TimeUnit;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Limit;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Polls the outbox and publishes pending events to Kafka in insertion order.
 * A row is only marked published after the broker acknowledges the send, so a
 * crash between send and mark yields at-least-once delivery (consumers must
 * de-duplicate on checkoutRequestId + eventType if they need exactly-once).
 */
@Component
public class OutboxRelay {

    public static final String TOPIC = "payments.events";
    private static final int BATCH_SIZE = 100;
    private static final long SEND_TIMEOUT_SECONDS = 10;

    private static final Logger log = LoggerFactory.getLogger(OutboxRelay.class);

    private final OutboxEventRepository repository;
    private final KafkaTemplate<String, String> kafkaTemplate;
    private final Clock clock;

    public OutboxRelay(OutboxEventRepository repository, KafkaTemplate<String, String> kafkaTemplate,
                       Clock clock) {
        this.repository = repository;
        this.kafkaTemplate = kafkaTemplate;
        this.clock = clock;
    }

    @Scheduled(fixedDelayString = "${outbox.relay-interval-ms:2000}")
    @Transactional
    public void relayPendingEvents() {
        List<OutboxEvent> pending = repository.findByPublishedAtIsNullOrderByIdAsc(Limit.of(BATCH_SIZE));
        for (OutboxEvent event : pending) {
            try {
                kafkaTemplate.send(TOPIC, event.getAggregateId(), event.getPayload())
                        .get(SEND_TIMEOUT_SECONDS, TimeUnit.SECONDS);
                event.markPublished(clock.instant());
                repository.save(event);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                return;
            } catch (Exception e) {
                // Leave the row unpublished; the next tick retries. Stop the batch to
                // preserve per-aggregate ordering.
                log.warn("Failed to publish outbox event id={} ({}); will retry", event.getId(),
                        e.getMessage());
                return;
            }
        }
    }
}
