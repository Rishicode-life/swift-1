package com.swiftpay.gateway.web.dto;

import com.swiftpay.common.model.PaymentStatus;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.Instant;

@Schema(description = "Accepted payment record (processing continues asynchronously)")
public record PaymentResponseDto(
        @Schema(example = "550e8400-e29b-41d4-a716-446655440000")
        String transactionId,
        PaymentStatus status,
        @Schema(description = "When the payment row was created in the gateway")
        Instant createdAt
) {
}
