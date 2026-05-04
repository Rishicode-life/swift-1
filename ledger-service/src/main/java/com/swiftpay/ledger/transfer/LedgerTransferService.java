package com.swiftpay.ledger.transfer;

import com.swiftpay.common.events.PaymentCompletedEvent;
import com.swiftpay.common.events.PaymentFailedEvent;
import com.swiftpay.common.events.PaymentInitiatedEvent;
import com.swiftpay.common.model.PaymentStatus;
import com.swiftpay.ledger.cache.BalanceCacheWriter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.sql.ResultSet;
import java.sql.SQLException;

@Service
public class LedgerTransferService {

    private static final Logger log = LoggerFactory.getLogger(LedgerTransferService.class);

    private final JdbcTemplate jdbc;
    private final BalanceCacheWriter balanceCacheWriter;

    public LedgerTransferService(JdbcTemplate jdbc, BalanceCacheWriter balanceCacheWriter) {
        this.jdbc = jdbc;
        this.balanceCacheWriter = balanceCacheWriter;
    }

    @Transactional
    public TransferOutcome process(PaymentInitiatedEvent event) {
        var status = jdbc.query(
                "SELECT status FROM payments WHERE transaction_id = ? FOR UPDATE",
                rs -> rs.next() ? mapStatus(rs) : null,
                event.transactionId()
        );
        if (status == null) {
            log.warn("Payment {} not found in ledger database", event.transactionId());
            var failed = new PaymentFailedEvent(
                    event.transactionId(),
                    "PAYMENT_NOT_FOUND",
                    System.currentTimeMillis()
            );
            return new TransferOutcome.Failed(failed);
        }
        if (status == PaymentStatus.COMPLETED) {
            return new TransferOutcome.Duplicate(event.transactionId());
        }
        if (status == PaymentStatus.FAILED) {
            return new TransferOutcome.Duplicate(event.transactionId());
        }

        int debited = jdbc.update(
                """
                        UPDATE accounts
                        SET balance_cents = balance_cents - ?,
                            version = version + 1
                        WHERE user_id = ?
                          AND balance_cents >= ?
                        """,
                event.amountCents(),
                event.senderId(),
                event.amountCents()
        );
        if (debited == 0) {
            markPaymentFailed(event.transactionId(), "INSUFFICIENT_FUNDS_OR_MISSING_ACCOUNT");
            var failed = new PaymentFailedEvent(
                    event.transactionId(),
                    "INSUFFICIENT_FUNDS_OR_MISSING_ACCOUNT",
                    System.currentTimeMillis()
            );
            return new TransferOutcome.Failed(failed);
        }

        jdbc.update(
                """
                        INSERT INTO accounts (user_id, balance_cents, currency, version)
                        VALUES (?, ?, ?, 0)
                        ON CONFLICT (user_id) DO UPDATE SET
                            balance_cents = accounts.balance_cents + EXCLUDED.balance_cents,
                            version = accounts.version + 1
                        """,
                event.receiverId(),
                event.amountCents(),
                event.currency()
        );

        jdbc.update(
                """
                        UPDATE payments
                        SET status = 'COMPLETED',
                            updated_at = NOW()
                        WHERE transaction_id = ?
                          AND status = 'PENDING'
                        """,
                event.transactionId()
        );

        refreshCachedBalances(event.senderId(), event.receiverId());

        var completed = new PaymentCompletedEvent(
                event.transactionId(),
                event.senderId(),
                event.receiverId(),
                event.amountCents(),
                event.currency(),
                System.currentTimeMillis()
        );
        return new TransferOutcome.Completed(completed);
    }

    private void markPaymentFailed(String transactionId, String reason) {
        jdbc.update(
                """
                        UPDATE payments
                        SET status = 'FAILED',
                            failure_reason = ?,
                            updated_at = NOW()
                        WHERE transaction_id = ?
                          AND status = 'PENDING'
                        """,
                reason,
                transactionId
        );
    }

    private void refreshCachedBalances(String senderId, String receiverId) {
        writeBalanceIfPresent(senderId);
        writeBalanceIfPresent(receiverId);
    }

    private void writeBalanceIfPresent(String userId) {
        jdbc.query(
                "SELECT balance_cents FROM accounts WHERE user_id = ?",
                rs -> {
                    if (rs.next()) {
                        balanceCacheWriter.writeBalance(userId, rs.getLong(1));
                    }
                    return null;
                },
                userId
        );
    }

    private static PaymentStatus mapStatus(ResultSet rs) throws SQLException {
        return PaymentStatus.valueOf(rs.getString(1));
    }
}
