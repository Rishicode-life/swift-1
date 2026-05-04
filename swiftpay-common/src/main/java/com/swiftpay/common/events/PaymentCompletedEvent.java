package com.swiftpay.common.events;

import com.fasterxml.jackson.annotation.JsonInclude;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record PaymentCompletedEvent(
        String transactionId,
        String senderId,
        String receiverId,
        long amountCents,
        String currency,
        long completedAtEpochMs
) {
}
