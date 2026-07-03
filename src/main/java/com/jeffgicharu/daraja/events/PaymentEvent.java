package com.jeffgicharu.daraja.events;

import com.jeffgicharu.daraja.domain.PaymentStatus;
import com.jeffgicharu.daraja.domain.PaymentTransaction;
import java.math.BigDecimal;
import java.time.Instant;

/** Payload published to the {@code payments.events} topic for every payment state change. */
public record PaymentEvent(
        String eventType,
        String checkoutRequestId,
        String phoneNumber,
        BigDecimal amount,
        String accountReference,
        PaymentStatus status,
        Integer resultCode,
        String mpesaReceipt,
        Instant occurredAt
) {
    public static final String TYPE_INITIATED = "payment.initiated";
    public static final String TYPE_COMPLETED = "payment.completed";
    public static final String TYPE_FAILED = "payment.failed";

    public static PaymentEvent of(String eventType, PaymentTransaction tx, Instant occurredAt) {
        return new PaymentEvent(eventType, tx.getCheckoutRequestId(), tx.getPhoneNumber(),
                tx.getAmount(), tx.getAccountReference(), tx.getStatus(),
                tx.getResultCode(), tx.getMpesaReceipt(), occurredAt);
    }
}
