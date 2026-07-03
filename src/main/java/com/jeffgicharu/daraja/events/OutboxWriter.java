package com.jeffgicharu.daraja.events;

import com.jeffgicharu.daraja.domain.OutboxEvent;
import com.jeffgicharu.daraja.domain.PaymentTransaction;
import com.jeffgicharu.daraja.repository.OutboxEventRepository;
import java.time.Clock;
import org.springframework.stereotype.Component;
import tools.jackson.databind.ObjectMapper;

/**
 * Serializes payment events into the outbox table. Must be called inside the
 * same transaction as the state change (the caller's @Transactional boundary).
 */
@Component
public class OutboxWriter {

    static final String AGGREGATE_TYPE = "payment";

    private final OutboxEventRepository repository;
    private final ObjectMapper objectMapper;
    private final Clock clock;

    public OutboxWriter(OutboxEventRepository repository, ObjectMapper objectMapper, Clock clock) {
        this.repository = repository;
        this.objectMapper = objectMapper;
        this.clock = clock;
    }

    public void write(String eventType, PaymentTransaction transaction) {
        PaymentEvent event = PaymentEvent.of(eventType, transaction, clock.instant());
        String payload = objectMapper.writeValueAsString(event);
        repository.save(new OutboxEvent(
                AGGREGATE_TYPE, transaction.getCheckoutRequestId(), eventType, payload));
    }
}
