package com.swiftpay.gateway.idempotency;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.Optional;

@Service
public class IdempotencyService {

    public static final String IN_PROGRESS_MARKER = "IN_PROGRESS";

    private final StringRedisTemplate redis;
    private final Duration ttl;

    public IdempotencyService(
            StringRedisTemplate redis,
            @Value("${swiftpay.idempotency-ttl-hours:24}") long idempotencyTtlHours
    ) {
        this.redis = redis;
        this.ttl = Duration.ofHours(Math.max(1, idempotencyTtlHours));
    }

    public Optional<String> getRaw(String transactionId) {
        return Optional.ofNullable(redis.opsForValue().get(idempotencyKey(transactionId)));
    }

    /**
     * Reserves the idempotency key for this transaction. Call {@link #complete(String, String)} or
     * {@link #clear(String)} when finished.
     */
    public boolean markInProgress(String transactionId) {
        return Boolean.TRUE.equals(
                redis.opsForValue().setIfAbsent(idempotencyKey(transactionId), IN_PROGRESS_MARKER, ttl)
        );
    }

    public void complete(String transactionId, String responseJson) {
        redis.opsForValue().set(idempotencyKey(transactionId), responseJson, ttl);
    }

    public void clear(String transactionId) {
        redis.delete(idempotencyKey(transactionId));
    }

    static String idempotencyKey(String transactionId) {
        return "idempotency:payment:" + transactionId;
    }
}
