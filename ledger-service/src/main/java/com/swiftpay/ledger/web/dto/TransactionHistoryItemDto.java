package com.swiftpay.ledger.web.dto;

import com.swiftpay.common.model.PaymentStatus;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.Instant;

@Schema(description = "Single payment row visible to a user (as sender or receiver)")
public record TransactionHistoryItemDto(
        String transactionId,
        String counterpartyId,
        String direction,
        long amountCents,
        String currency,
        PaymentStatus status,
        Instant createdAt
) {
}
