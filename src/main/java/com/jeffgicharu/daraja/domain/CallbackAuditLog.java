package com.jeffgicharu.daraja.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;

/**
 * An immutable record of every callback payload Daraja sends us, stored before
 * processing so that duplicate or malformed callbacks are always auditable.
 */
@Entity
@Table(name = "callback_audit_log")
public class CallbackAuditLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "checkout_request_id", nullable = false)
    private String checkoutRequestId;

    @Column(name = "raw_payload", nullable = false, columnDefinition = "json")
    private String rawPayload;

    @Column(name = "processing_status", nullable = false)
    private String processingStatus;

    @Column(name = "received_at", insertable = false, updatable = false)
    private Instant receivedAt;

    protected CallbackAuditLog() {
    }

    public CallbackAuditLog(String checkoutRequestId, String rawPayload, String processingStatus) {
        this.checkoutRequestId = checkoutRequestId;
        this.rawPayload = rawPayload;
        this.processingStatus = processingStatus;
    }

    public Long getId() {
        return id;
    }

    public String getCheckoutRequestId() {
        return checkoutRequestId;
    }

    public String getRawPayload() {
        return rawPayload;
    }

    public String getProcessingStatus() {
        return processingStatus;
    }

    public Instant getReceivedAt() {
        return receivedAt;
    }
}
