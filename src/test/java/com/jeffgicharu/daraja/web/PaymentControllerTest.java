package com.jeffgicharu.daraja.web;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.jeffgicharu.daraja.config.AppConfig;
import com.jeffgicharu.daraja.config.SecurityConfig;
import com.jeffgicharu.daraja.domain.PaymentTransaction;
import com.jeffgicharu.daraja.service.PaymentService;
import java.math.BigDecimal;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(PaymentController.class)
@Import({AppConfig.class, SecurityConfig.class})
class PaymentControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private PaymentService paymentService;

    @Test
    void initiateReturns202WithCheckoutId() throws Exception {
        PaymentTransaction tx = new PaymentTransaction(
                "c-1", "m-1", "254712345678", new BigDecimal("100"), "ORDER-1");
        when(paymentService.initiatePayment(eq("254712345678"), any(), eq("ORDER-1"))).thenReturn(tx);

        mockMvc.perform(post("/api/v1/payments")
                        .contentType("application/json")
                        .content("""
                                {"phoneNumber":"254712345678","amount":100,"accountReference":"ORDER-1"}
                                """))
                .andExpect(status().isAccepted())
                .andExpect(jsonPath("$.checkoutRequestId").value("c-1"))
                .andExpect(jsonPath("$.status").value("PENDING"));
    }

    @Test
    void initiateRejectsInvalidPhoneNumberWith400() throws Exception {
        mockMvc.perform(post("/api/v1/payments")
                        .contentType("application/json")
                        .content("""
                                {"phoneNumber":"0712345678","amount":100,"accountReference":"ORDER-1"}
                                """))
                .andExpect(status().isBadRequest());
    }

    @Test
    void callbackAlwaysAcknowledgesWith200() throws Exception {
        mockMvc.perform(post("/api/v1/payments/callback")
                        .contentType("application/json")
                        .content("""
                                {"Body":{"stkCallback":{"MerchantRequestID":"m","CheckoutRequestID":"c-1",
                                "ResultCode":0,"ResultDesc":"OK"}}}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.ResultCode").value(0));

        verify(paymentService).handleCallback(any());
    }
}
