package com.swiftpay.common.events;

import com.fasterxml.jackson.annotation.JsonInclude;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record PaymentFailedEvent(
        String transactionId,
        String reason,
        long failedAtEpochMs
) {
}
