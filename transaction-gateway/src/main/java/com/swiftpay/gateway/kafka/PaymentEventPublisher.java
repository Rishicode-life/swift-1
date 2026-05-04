package com.swiftpay.gateway.kafka;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.swiftpay.common.events.PaymentInitiatedEvent;
import com.swiftpay.common.kafka.KafkaTopics;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;
import org.springframework.stereotype.Component;

import java.util.concurrent.ExecutionException;

@Component
public class PaymentEventPublisher {

    private static final Logger log = LoggerFactory.getLogger(PaymentEventPublisher.class);

    private final KafkaTemplate<String, String> kafkaTemplate;
    private final ObjectMapper objectMapper;

    public PaymentEventPublisher(KafkaTemplate<String, String> kafkaTemplate, ObjectMapper objectMapper) {
        this.kafkaTemplate = kafkaTemplate;
        this.objectMapper = objectMapper;
    }

    public void publishPaymentInitiated(PaymentInitiatedEvent event) {
        try {
            var json = objectMapper.writeValueAsString(event);
            SendResult<String, String> result =
                    kafkaTemplate.send(KafkaTopics.PAYMENT_INITIATED, event.transactionId(), json).get();
            log.debug(
                    "Published PaymentInitiated {} partition {} offset {}",
                    event.transactionId(),
                    result.getRecordMetadata().partition(),
                    result.getRecordMetadata().offset()
            );
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Cannot serialize event", e);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new org.springframework.kafka.KafkaException("Interrupted while publishing", e);
        } catch (ExecutionException e) {
            var cause = e.getCause() != null ? e.getCause() : e;
            throw new org.springframework.kafka.KafkaException("Failed to publish PaymentInitiated", cause);
        }
    }
}
