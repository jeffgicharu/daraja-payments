package com.jeffgicharu.daraja.daraja.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

/** Synchronous response from a Daraja STK push request. */
public record StkPushResponse(
        @JsonProperty("MerchantRequestID") String merchantRequestId,
        @JsonProperty("CheckoutRequestID") String checkoutRequestId,
        @JsonProperty("ResponseCode") String responseCode,
        @JsonProperty("ResponseDescription") String responseDescription,
        @JsonProperty("CustomerMessage") String customerMessage
) {
}
