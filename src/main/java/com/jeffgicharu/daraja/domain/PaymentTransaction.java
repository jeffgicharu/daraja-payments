package com.jeffgicharu.daraja.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.Instant;

@Entity
@Table(name = "payment_transactions")
public class PaymentTransaction {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "checkout_request_id", nullable = false, unique = true)
    private String checkoutRequestId;

    @Column(name = "merchant_request_id")
    private String merchantRequestId;

    @Column(name = "phone_number", nullable = false)
    private String phoneNumber;

    @Column(nullable = false)
    private BigDecimal amount;

    @Column(name = "account_reference", nullable = false)
    private String accountReference;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private PaymentStatus status = PaymentStatus.PENDING;

    @Column(name = "result_code")
    private Integer resultCode;

    @Column(name = "result_desc")
    private String resultDesc;

    @Column(name = "mpesa_receipt")
    private String mpesaReceipt;

    @Column(name = "created_at", insertable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", insertable = false, updatable = false)
    private Instant updatedAt;

    protected PaymentTransaction() {
    }

    public PaymentTransaction(String checkoutRequestId, String merchantRequestId, String phoneNumber,
                              BigDecimal amount, String accountReference) {
        this.checkoutRequestId = checkoutRequestId;
        this.merchantRequestId = merchantRequestId;
        this.phoneNumber = phoneNumber;
        this.amount = amount;
        this.accountReference = accountReference;
        this.status = PaymentStatus.PENDING;
    }

    /** True once a result callback has moved this transaction out of PENDING. */
    public boolean isTerminal() {
        return status != PaymentStatus.PENDING;
    }

    public void markCompleted(String mpesaReceipt, int resultCode, String resultDesc) {
        this.status = PaymentStatus.COMPLETED;
        this.mpesaReceipt = mpesaReceipt;
        this.resultCode = resultCode;
        this.resultDesc = resultDesc;
    }

    public void markFailed(int resultCode, String resultDesc) {
        this.status = PaymentStatus.FAILED;
        this.resultCode = resultCode;
        this.resultDesc = resultDesc;
    }

    public Long getId() {
        return id;
    }

    public String getCheckoutRequestId() {
        return checkoutRequestId;
    }

    public String getMerchantRequestId() {
        return merchantRequestId;
    }

    public String getPhoneNumber() {
        return phoneNumber;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public String getAccountReference() {
        return accountReference;
    }

    public PaymentStatus getStatus() {
        return status;
    }

    public Integer getResultCode() {
        return resultCode;
    }

    public String getResultDesc() {
        return resultDesc;
    }

    public String getMpesaReceipt() {
        return mpesaReceipt;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }
}
