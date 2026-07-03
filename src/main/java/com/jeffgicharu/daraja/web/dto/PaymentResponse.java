package com.jeffgicharu.daraja.web.dto;

import com.jeffgicharu.daraja.domain.PaymentStatus;
import com.jeffgicharu.daraja.domain.PaymentTransaction;
import java.math.BigDecimal;

public record PaymentResponse(
        String checkoutRequestId,
        String phoneNumber,
        BigDecimal amount,
        String accountReference,
        PaymentStatus status,
        String mpesaReceipt
) {
    public static PaymentResponse from(PaymentTransaction transaction) {
        return new PaymentResponse(
                transaction.getCheckoutRequestId(),
                transaction.getPhoneNumber(),
                transaction.getAmount(),
                transaction.getAccountReference(),
                transaction.getStatus(),
                transaction.getMpesaReceipt()
        );
    }
}
