package com.jeffgicharu.daraja.events;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;
import tools.jackson.databind.ObjectMapper;

/**
 * Demo downstream consumer: in a real deployment this would send SMS/email
 * receipts. Here it proves the pub/sub flow end to end by logging every event.
 */
@Component
public class NotificationsConsumer {

    private static final Logger log = LoggerFactory.getLogger(NotificationsConsumer.class);

    private final ObjectMapper objectMapper;

    public NotificationsConsumer(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    @KafkaListener(topics = OutboxRelay.TOPIC, groupId = "notifications")
    public void onPaymentEvent(String payload) {
        PaymentEvent event = objectMapper.readValue(payload, PaymentEvent.class);
        log.info("NOTIFICATION: {} for checkoutRequestId={} amount={} status={}",
                event.eventType(), event.checkoutRequestId(), event.amount(), event.status());
    }
}
