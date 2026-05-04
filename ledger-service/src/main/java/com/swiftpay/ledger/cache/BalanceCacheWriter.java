package com.swiftpay.ledger.cache;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.Optional;

@Component
public class BalanceCacheWriter {

    private final StringRedisTemplate redis;
    private final Duration ttl;

    public BalanceCacheWriter(
            StringRedisTemplate redis,
            @Value("${swiftpay.balance-cache-ttl-seconds:60}") long ttlSeconds
    ) {
        this.redis = redis;
        this.ttl = Duration.ofSeconds(ttlSeconds);
    }

    public void writeBalance(String userId, long balanceCents) {
        redis.opsForValue().set("balance:" + userId, Long.toString(balanceCents), ttl);
    }

    public void invalidate(String userId) {
        redis.delete("balance:" + userId);
    }

    public Optional<Long> readBalance(String userId) {
        var v = redis.opsForValue().get("balance:" + userId);
        if (v == null) {
            return Optional.empty();
        }
        try {
            return Optional.of(Long.parseLong(v));
        } catch (NumberFormatException e) {
            return Optional.empty();
        }
    }
}
