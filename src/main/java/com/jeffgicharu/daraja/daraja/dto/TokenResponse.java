package com.jeffgicharu.daraja.daraja.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

/** Response from Daraja's OAuth {@code /oauth/v1/generate} endpoint. */
public record TokenResponse(
        @JsonProperty("access_token") String accessToken,
        @JsonProperty("expires_in") String expiresIn
) {
}
