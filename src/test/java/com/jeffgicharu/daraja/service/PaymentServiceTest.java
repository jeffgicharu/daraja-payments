package com.jeffgicharu.daraja.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.jeffgicharu.daraja.daraja.DarajaClient;
import com.jeffgicharu.daraja.daraja.dto.StkCallback;
import com.jeffgicharu.daraja.daraja.dto.StkPushResponse;
import com.jeffgicharu.daraja.domain.PaymentStatus;
import com.jeffgicharu.daraja.domain.PaymentTransaction;
import com.jeffgicharu.daraja.events.OutboxWriter;
import com.jeffgicharu.daraja.events.PaymentEvent;
import com.jeffgicharu.daraja.repository.CallbackAuditLogRepository;
import com.jeffgicharu.daraja.repository.PaymentTransactionRepository;
import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import tools.jackson.databind.ObjectMapper;

@ExtendWith(MockitoExtension.class)
class PaymentServiceTest {

    @Mock
    private DarajaClient darajaClient;
    @Mock
    private PaymentTransactionRepository transactionRepository;
    @Mock
    private CallbackAuditLogRepository auditLogRepository;
    @Mock
    private OutboxWriter outboxWriter;

    private PaymentService paymentService;

    @BeforeEach
    void setUp() {
        paymentService = new PaymentService(
                darajaClient, transactionRepository, auditLogRepository, new ObjectMapper(), outboxWriter);
    }

    @Test
    void initiatePaymentPersistsPendingTransactionOnSuccess() {
        when(darajaClient.initiateStkPush(any(), any(), any(), any()))
                .thenReturn(new StkPushResponse("m-1", "c-1", "0", "Success", "Enter PIN"));
        when(transactionRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        PaymentTransaction result = paymentService.initiatePayment(
                "254712345678", new BigDecimal("100"), "ORDER-1");

        assertThat(result.getStatus()).isEqualTo(PaymentStatus.PENDING);
        assertThat(result.getCheckoutRequestId()).isEqualTo("c-1");
        verify(transactionRepository).save(any(PaymentTransaction.class));
        verify(outboxWriter).write(eq(PaymentEvent.TYPE_INITIATED), any(PaymentTransaction.class));
    }

    @Test
    void initiatePaymentThrowsWhenDarajaRejects() {
        when(darajaClient.initiateStkPush(any(), any(), any(), any()))
                .thenReturn(new StkPushResponse(null, null, "1", "Invalid", null));

        assertThatThrownBy(() -> paymentService.initiatePayment(
                "254712345678", new BigDecimal("100"), "ORDER-1"))
                .isInstanceOf(PaymentInitiationException.class);
        verify(transactionRepository, never()).save(any());
    }

    @Test
    void successfulCallbackMarksTransactionCompleted() {
        PaymentTransaction pending = new PaymentTransaction(
                "c-1", "m-1", "254712345678", new BigDecimal("100"), "ORDER-1");
        when(transactionRepository.findByCheckoutRequestId("c-1")).thenReturn(Optional.of(pending));

        paymentService.handleCallback(successCallback("c-1", "NLJ7RT61SV"));

        assertThat(pending.getStatus()).isEqualTo(PaymentStatus.COMPLETED);
        assertThat(pending.getMpesaReceipt()).isEqualTo("NLJ7RT61SV");
        verify(transactionRepository).save(pending);
        verify(auditLogRepository).save(any());
        verify(outboxWriter).write(PaymentEvent.TYPE_COMPLETED, pending);
    }

    @Test
    void failedCallbackMarksTransactionFailed() {
        PaymentTransaction pending = new PaymentTransaction(
                "c-2", "m-2", "254712345678", new BigDecimal("50"), "ORDER-2");
        when(transactionRepository.findByCheckoutRequestId("c-2")).thenReturn(Optional.of(pending));

        paymentService.handleCallback(failureCallback("c-2", 1032, "Request cancelled by user"));

        assertThat(pending.getStatus()).isEqualTo(PaymentStatus.FAILED);
        assertThat(pending.getResultCode()).isEqualTo(1032);
        verify(transactionRepository).save(pending);
    }

    @Test
    void duplicateCallbackIsIdempotentAndDoesNotReapplyResult() {
        PaymentTransaction alreadyCompleted = new PaymentTransaction(
                "c-3", "m-3", "254712345678", new BigDecimal("100"), "ORDER-3");
        alreadyCompleted.markCompleted("RECEIPT1", 0, "The service was accepted successfully");
        when(transactionRepository.findByCheckoutRequestId("c-3")).thenReturn(Optional.of(alreadyCompleted));

        // A second (duplicate) callback arrives — Daraja retries are common.
        paymentService.handleCallback(failureCallback("c-3", 1, "Should be ignored"));

        // Status is unchanged; the duplicate never overwrites the terminal result.
        assertThat(alreadyCompleted.getStatus()).isEqualTo(PaymentStatus.COMPLETED);
        assertThat(alreadyCompleted.getMpesaReceipt()).isEqualTo("RECEIPT1");
        verify(transactionRepository, never()).save(any());
        verify(outboxWriter, never()).write(anyString(), any());
        // ...but the duplicate is still audited.
        ArgumentCaptor<com.jeffgicharu.daraja.domain.CallbackAuditLog> captor =
                ArgumentCaptor.forClass(com.jeffgicharu.daraja.domain.CallbackAuditLog.class);
        verify(auditLogRepository).save(captor.capture());
        assertThat(captor.getValue().getProcessingStatus()).isEqualTo("DUPLICATE");
    }

    @Test
    void callbackForUnknownTransactionIsAuditedButNotApplied() {
        when(transactionRepository.findByCheckoutRequestId("unknown"))
                .thenReturn(Optional.empty());

        paymentService.handleCallback(successCallback("unknown", "X"));

        verify(transactionRepository, never()).save(any());
        ArgumentCaptor<com.jeffgicharu.daraja.domain.CallbackAuditLog> captor =
                ArgumentCaptor.forClass(com.jeffgicharu.daraja.domain.CallbackAuditLog.class);
        verify(auditLogRepository).save(captor.capture());
        assertThat(captor.getValue().getProcessingStatus()).isEqualTo("UNMATCHED");
    }

    @Test
    void malformedCallbackIsAuditedAsUnmatched() {
        paymentService.handleCallback(new StkCallback(null));

        verify(transactionRepository, never()).save(any());
        verify(auditLogRepository).save(any());
        verify(transactionRepository, never()).findByCheckoutRequestId(anyString());
    }

    private StkCallback successCallback(String checkoutRequestId, String receipt) {
        StkCallback.CallbackMetadata metadata = new StkCallback.CallbackMetadata(List.of(
                new StkCallback.Item("Amount", 100),
                new StkCallback.Item("MpesaReceiptNumber", receipt),
                new StkCallback.Item("PhoneNumber", 254712345678L)));
        StkCallback.Callback cb = new StkCallback.Callback(
                "m", checkoutRequestId, 0, "The service was accepted successfully", metadata);
        return new StkCallback(new StkCallback.Body(cb));
    }

    private StkCallback failureCallback(String checkoutRequestId, int resultCode, String desc) {
        StkCallback.Callback cb = new StkCallback.Callback(
                "m", checkoutRequestId, resultCode, desc, null);
        return new StkCallback(new StkCallback.Body(cb));
    }
}
