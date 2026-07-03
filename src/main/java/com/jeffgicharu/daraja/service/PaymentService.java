package com.jeffgicharu.daraja.service;

import tools.jackson.core.JacksonException;
import tools.jackson.databind.ObjectMapper;
import com.jeffgicharu.daraja.daraja.DarajaClient;
import com.jeffgicharu.daraja.daraja.dto.StkCallback;
import com.jeffgicharu.daraja.daraja.dto.StkPushResponse;
import com.jeffgicharu.daraja.domain.CallbackAuditLog;
import com.jeffgicharu.daraja.domain.PaymentTransaction;
import com.jeffgicharu.daraja.repository.CallbackAuditLogRepository;
import com.jeffgicharu.daraja.repository.PaymentTransactionRepository;
import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class PaymentService {

    private static final Logger log = LoggerFactory.getLogger(PaymentService.class);

    private final DarajaClient darajaClient;
    private final PaymentTransactionRepository transactionRepository;
    private final CallbackAuditLogRepository auditLogRepository;
    private final ObjectMapper objectMapper;

    public PaymentService(DarajaClient darajaClient, PaymentTransactionRepository transactionRepository,
                          CallbackAuditLogRepository auditLogRepository, ObjectMapper objectMapper) {
        this.darajaClient = darajaClient;
        this.transactionRepository = transactionRepository;
        this.auditLogRepository = auditLogRepository;
        this.objectMapper = objectMapper;
    }

    /** Initiate an STK push and persist a PENDING transaction keyed by CheckoutRequestID. */
    @Transactional
    public PaymentTransaction initiatePayment(String phoneNumber, BigDecimal amount, String accountReference) {
        StkPushResponse response = darajaClient.initiateStkPush(
                phoneNumber, amount, accountReference, "Payment for " + accountReference);

        if (response == null || !"0".equals(response.responseCode())) {
            throw new PaymentInitiationException(
                    "Daraja rejected the STK push: "
                            + (response == null ? "no response" : response.responseDescription()));
        }

        PaymentTransaction transaction = new PaymentTransaction(
                response.checkoutRequestId(), response.merchantRequestId(),
                phoneNumber, amount, accountReference);
        return transactionRepository.save(transaction);
    }

    /**
     * Process a Daraja result callback idempotently. Every payload is audited.
     * A callback for an unknown or already-finalized transaction is a no-op on
     * the transaction itself, so Daraja's retries never double-apply a result.
     */
    @Transactional
    public void handleCallback(StkCallback callback) {
        StkCallback.Callback body = extractCallback(callback);
        String checkoutRequestId = body == null ? "UNKNOWN" : body.checkoutRequestId();

        Optional<PaymentTransaction> existing = body == null
                ? Optional.empty()
                : transactionRepository.findByCheckoutRequestId(checkoutRequestId);

        String processingStatus = determineProcessingStatus(body, existing);
        audit(checkoutRequestId, callback, processingStatus);

        if (body == null || existing.isEmpty()) {
            log.warn("Callback for unrecognised CheckoutRequestID={} ignored", checkoutRequestId);
            return;
        }
        PaymentTransaction transaction = existing.get();
        if (transaction.isTerminal()) {
            log.info("Duplicate callback for CheckoutRequestID={} ignored (already {})",
                    checkoutRequestId, transaction.getStatus());
            return;
        }

        if (body.resultCode() == 0) {
            transaction.markCompleted(extractReceipt(body), body.resultCode(), body.resultDesc());
        } else {
            transaction.markFailed(body.resultCode(), body.resultDesc());
        }
        transactionRepository.save(transaction);
    }

    private String determineProcessingStatus(StkCallback.Callback body, Optional<PaymentTransaction> existing) {
        if (body == null || existing.isEmpty()) {
            return "UNMATCHED";
        }
        return existing.get().isTerminal() ? "DUPLICATE" : "PROCESSED";
    }

    private StkCallback.Callback extractCallback(StkCallback callback) {
        if (callback == null || callback.body() == null) {
            return null;
        }
        return callback.body().stkCallback();
    }

    private String extractReceipt(StkCallback.Callback body) {
        if (body.callbackMetadata() == null || body.callbackMetadata().items() == null) {
            return null;
        }
        return body.callbackMetadata().items().stream()
                .filter(item -> "MpesaReceiptNumber".equals(item.name()))
                .map(item -> String.valueOf(item.value()))
                .findFirst()
                .orElse(null);
    }

    private void audit(String checkoutRequestId, StkCallback callback, String processingStatus) {
        String raw;
        try {
            raw = objectMapper.writeValueAsString(callback);
        } catch (JacksonException e) {
            raw = "{\"error\":\"could not serialize callback\"}";
        }
        auditLogRepository.save(new CallbackAuditLog(checkoutRequestId, raw, processingStatus));
    }

    @Transactional(readOnly = true)
    public Optional<PaymentTransaction> findByCheckoutRequestId(String checkoutRequestId) {
        return transactionRepository.findByCheckoutRequestId(checkoutRequestId);
    }

    @Transactional(readOnly = true)
    public List<PaymentTransaction> findAll() {
        return transactionRepository.findAll();
    }
}
