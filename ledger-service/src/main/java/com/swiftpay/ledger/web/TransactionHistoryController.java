package com.swiftpay.ledger.web;

import com.swiftpay.common.model.PaymentStatus;
import com.swiftpay.ledger.web.dto.TransactionHistoryItemDto;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/v1/users")
@Tag(name = "History", description = "User transaction history for audit and reporting")
public class TransactionHistoryController {

    private final JdbcTemplate jdbcTemplate;

    public TransactionHistoryController(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @GetMapping("/{userId}/transactions")
    @Operation(summary = "List transactions for a user")
    public List<TransactionHistoryItemDto> history(
            @PathVariable("userId") String userId,
            @RequestParam(name = "limit", defaultValue = "100") int limit
    ) {
        var capped = Math.min(Math.max(limit, 1), 500);
        return jdbcTemplate.query(
                """
                        SELECT transaction_id,
                               sender_id,
                               receiver_id,
                               amount_cents,
                               currency,
                               status,
                               created_at
                        FROM payments
                        WHERE sender_id = ? OR receiver_id = ?
                        ORDER BY created_at DESC
                        LIMIT ?
                        """,
                (rs, rowNum) -> {
                    var sender = rs.getString("sender_id");
                    var receiver = rs.getString("receiver_id");
                    var direction = userId.equals(sender) ? "DEBIT" : "CREDIT";
                    var counterparty = userId.equals(sender) ? receiver : sender;
                    return new TransactionHistoryItemDto(
                            rs.getString("transaction_id"),
                            counterparty,
                            direction,
                            rs.getLong("amount_cents"),
                            rs.getString("currency"),
                            PaymentStatus.valueOf(rs.getString("status")),
                            rs.getTimestamp("created_at").toInstant()
                    );
                },
                userId,
                userId,
                capped
        );
    }
}
