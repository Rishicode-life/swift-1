package com.swiftpay.gateway.web.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

@Schema(description = "P2P payment initiation request")
public record PaymentRequestDto(
        @Schema(description = "Client-supplied unique id for idempotency", example = "550e8400-e29b-41d4-a716-446655440000")
        @NotBlank
        @Size(max = 64)
        String transactionId,

        @Schema(example = "user-alice")
        @NotBlank
        @Size(max = 64)
        String senderId,

        @Schema(example = "user-bob")
        @NotBlank
        @Size(max = 64)
        String receiverId,

        @Schema(description = "Amount in major currency units (e.g. dollars)", example = "25.50")
        @NotNull
        @Positive
        BigDecimal amount,

        @Schema(example = "USD")
        @NotBlank
        @Pattern(regexp = "[A-Z]{3}")
        String currency
) {
}
