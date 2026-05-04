package com.swiftpay.common.kafka;

public final class KafkaTopics {

    public static final String PAYMENT_INITIATED = "payment.initiated";
    public static final String PAYMENT_COMPLETED = "payment.completed";
    public static final String PAYMENT_FAILED = "payment.failed";

    private KafkaTopics() {
    }
}
