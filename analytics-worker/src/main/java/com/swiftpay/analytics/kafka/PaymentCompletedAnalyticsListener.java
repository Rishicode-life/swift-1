package com.swiftpay.analytics.kafka;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.swiftpay.common.events.PaymentCompletedEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Component;

@Component
public class PaymentCompletedAnalyticsListener {

    private static final Logger log = LoggerFactory.getLogger(PaymentCompletedAnalyticsListener.class);

    private final ObjectMapper objectMapper;
    private final JdbcTemplate jdbcTemplate;

    public PaymentCompletedAnalyticsListener(ObjectMapper objectMapper, JdbcTemplate jdbcTemplate) {
        this.objectMapper = objectMapper;
        this.jdbcTemplate = jdbcTemplate;
    }

    @KafkaListener(
            topics = com.swiftpay.common.kafka.KafkaTopics.PAYMENT_COMPLETED,
            containerFactory = "kafkaListenerContainerFactory"
    )
    public void onCompleted(@Payload String payload, Acknowledgment acknowledgment) {
        try {
            var event = objectMapper.readValue(payload, PaymentCompletedEvent.class);
            jdbcTemplate.update(
                    """
                            INSERT INTO payment_analytics (
                                transaction_id, sender_id, receiver_id, amount_cents, currency
                            ) VALUES (?, ?, ?, ?, ?)
                            """,
                    event.transactionId(),
                    event.senderId(),
                    event.receiverId(),
                    event.amountCents(),
                    event.currency()
            );
            log.debug("Recorded analytics for {}", event.transactionId());
            acknowledgment.acknowledge();
        } catch (RuntimeException e) {
            log.error("Analytics ingest failed", e);
            throw e;
        } catch (Exception e) {
            log.error("Analytics ingest failed", e);
            throw new RuntimeException(e);
        }
    }
}
