package com.jeffgicharu.daraja.web;

import com.jeffgicharu.daraja.daraja.dto.StkCallback;
import com.jeffgicharu.daraja.domain.PaymentTransaction;
import com.jeffgicharu.daraja.service.PaymentService;
import com.jeffgicharu.daraja.web.dto.InitiatePaymentRequest;
import com.jeffgicharu.daraja.web.dto.PaymentResponse;
import jakarta.validation.Valid;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/payments")
public class PaymentController {

    private static final Logger log = LoggerFactory.getLogger(PaymentController.class);

    private final PaymentService paymentService;

    public PaymentController(PaymentService paymentService) {
        this.paymentService = paymentService;
    }

    @PostMapping
    public ResponseEntity<PaymentResponse> initiate(@Valid @RequestBody InitiatePaymentRequest request) {
        PaymentTransaction transaction = paymentService.initiatePayment(
                request.phoneNumber(), request.amount(), request.accountReference());
        return ResponseEntity.status(HttpStatus.ACCEPTED).body(PaymentResponse.from(transaction));
    }

    @GetMapping
    public List<PaymentResponse> list() {
        return paymentService.findAll().stream().map(PaymentResponse::from).toList();
    }

    @GetMapping("/{checkoutRequestId}")
    public ResponseEntity<PaymentResponse> get(@PathVariable String checkoutRequestId) {
        return paymentService.findByCheckoutRequestId(checkoutRequestId)
                .map(PaymentResponse::from)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    /**
     * Daraja result callback endpoint. Always returns 200 with the acknowledgement
     * shape Daraja expects; processing is idempotent so retries are safe.
     */
    @PostMapping("/callback")
    public ResponseEntity<CallbackAcknowledgement> callback(@RequestBody StkCallback callback) {
        log.info("Received STK callback");
        paymentService.handleCallback(callback);
        return ResponseEntity.ok(new CallbackAcknowledgement(0, "Accepted"));
    }

    public record CallbackAcknowledgement(int ResultCode, String ResultDesc) {
    }
}
