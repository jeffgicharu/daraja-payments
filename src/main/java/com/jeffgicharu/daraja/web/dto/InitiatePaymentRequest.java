package com.jeffgicharu.daraja.web.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import java.math.BigDecimal;

public record InitiatePaymentRequest(
        @NotBlank
        @Pattern(regexp = "2547\\d{8}", message = "phoneNumber must be in the format 2547XXXXXXXX")
        String phoneNumber,

        @NotNull
        @DecimalMin(value = "1.0", message = "amount must be at least 1")
        BigDecimal amount,

        @NotBlank
        String accountReference
) {
}
