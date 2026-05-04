package com.swiftpay.ledger.kafka;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.swiftpay.common.events.PaymentInitiatedEvent;
import com.swiftpay.ledger.transfer.LedgerTransferService;
import com.swiftpay.ledger.transfer.TransferOutcome;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Component;

@Component
public class PaymentInitiatedListener {

    private static final Logger log = LoggerFactory.getLogger(PaymentInitiatedListener.class);

    private final ObjectMapper objectMapper;
    private final LedgerTransferService ledgerTransferService;
    private final OutcomeEventPublisher outcomeEventPublisher;

    public PaymentInitiatedListener(
            ObjectMapper objectMapper,
            LedgerTransferService ledgerTransferService,
            OutcomeEventPublisher outcomeEventPublisher
    ) {
        this.objectMapper = objectMapper;
        this.ledgerTransferService = ledgerTransferService;
        this.outcomeEventPublisher = outcomeEventPublisher;
    }

    @KafkaListener(
            topics = com.swiftpay.common.kafka.KafkaTopics.PAYMENT_INITIATED,
            containerFactory = "kafkaListenerContainerFactory"
    )
    public void consume(@Payload String payload, Acknowledgment acknowledgment) {
        try {
            var event = objectMapper.readValue(payload, PaymentInitiatedEvent.class);
            log.info("Consumed PaymentInitiated {}", event.transactionId());
            var outcome = ledgerTransferService.process(event);
            switch (outcome) {
                case TransferOutcome.Completed c -> outcomeEventPublisher.publishCompleted(c.event());
                case TransferOutcome.Failed f -> outcomeEventPublisher.publishFailed(f.event());
                case TransferOutcome.Duplicate d ->
                        log.debug("Skipping duplicate processing for {}", d.transactionId());
            }
            acknowledgment.acknowledge();
        } catch (RuntimeException e) {
            log.error("Error processing payment event", e);
            throw e;
        } catch (Exception e) {
            log.error("Error processing payment event", e);
            throw new RuntimeException(e);
        }
    }
}
