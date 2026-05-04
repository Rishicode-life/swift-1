package com.swiftpay.ledger.transfer;

import com.swiftpay.common.events.PaymentCompletedEvent;
import com.swiftpay.common.events.PaymentFailedEvent;

public sealed interface TransferOutcome permits TransferOutcome.Completed, TransferOutcome.Failed, TransferOutcome.Duplicate {

    record Completed(PaymentCompletedEvent event) implements TransferOutcome {
    }

    record Failed(PaymentFailedEvent event) implements TransferOutcome {
    }

    record Duplicate(String transactionId) implements TransferOutcome {
    }
}
