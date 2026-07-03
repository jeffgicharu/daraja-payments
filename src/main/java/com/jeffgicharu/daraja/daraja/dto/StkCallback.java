package com.jeffgicharu.daraja.daraja.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;

/**
 * Models the Daraja result callback payload:
 * <pre>{ "Body": { "stkCallback": { ... } } }</pre>
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record StkCallback(@JsonProperty("Body") Body body) {

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Body(@JsonProperty("stkCallback") Callback stkCallback) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Callback(
            @JsonProperty("MerchantRequestID") String merchantRequestId,
            @JsonProperty("CheckoutRequestID") String checkoutRequestId,
            @JsonProperty("ResultCode") int resultCode,
            @JsonProperty("ResultDesc") String resultDesc,
            @JsonProperty("CallbackMetadata") CallbackMetadata callbackMetadata
    ) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record CallbackMetadata(@JsonProperty("Item") List<Item> items) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Item(@JsonProperty("Name") String name, @JsonProperty("Value") Object value) {
    }
}
