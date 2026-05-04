package com.swiftpay.ledger.kafka;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.swiftpay.common.events.PaymentCompletedEvent;
import com.swiftpay.common.events.PaymentFailedEvent;
import com.swiftpay.common.kafka.KafkaTopics;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

import java.util.concurrent.ExecutionException;

@Component
public class OutcomeEventPublisher {

    private static final Logger log = LoggerFactory.getLogger(OutcomeEventPublisher.class);

    private final KafkaTemplate<String, String> kafkaTemplate;
    private final ObjectMapper objectMapper;

    public OutcomeEventPublisher(KafkaTemplate<String, String> kafkaTemplate, ObjectMapper objectMapper) {
        this.kafkaTemplate = kafkaTemplate;
        this.objectMapper = objectMapper;
    }

    public void publishCompleted(PaymentCompletedEvent event) {
        publish(KafkaTopics.PAYMENT_COMPLETED, event.transactionId(), event);
    }

    public void publishFailed(PaymentFailedEvent event) {
        publish(KafkaTopics.PAYMENT_FAILED, event.transactionId(), event);
    }

    private void publish(String topic, String key, Object payload) {
        try {
            var json = objectMapper.writeValueAsString(payload);
            var result = kafkaTemplate.send(topic, key, json).get();
            log.debug("Published to {} partition {} offset {}", topic, result.getRecordMetadata().partition(),
                    result.getRecordMetadata().offset());
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Cannot serialize event", e);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new org.springframework.kafka.KafkaException("Interrupted while publishing outcome", e);
        } catch (ExecutionException e) {
            var cause = e.getCause() != null ? e.getCause() : e;
            throw new org.springframework.kafka.KafkaException("Failed to publish outcome event", cause);
        }
    }
}
