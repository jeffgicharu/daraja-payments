package com.jeffgicharu.daraja.daraja.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.math.BigDecimal;

/** Request body for Daraja's {@code /mpesa/stkpush/v1/processrequest} endpoint. */
public record StkPushRequest(
        @JsonProperty("BusinessShortCode") String businessShortCode,
        @JsonProperty("Password") String password,
        @JsonProperty("Timestamp") String timestamp,
        @JsonProperty("TransactionType") String transactionType,
        @JsonProperty("Amount") BigDecimal amount,
        @JsonProperty("PartyA") String partyA,
        @JsonProperty("PartyB") String partyB,
        @JsonProperty("PhoneNumber") String phoneNumber,
        @JsonProperty("CallBackURL") String callBackUrl,
        @JsonProperty("AccountReference") String accountReference,
        @JsonProperty("TransactionDesc") String transactionDesc
) {
}
