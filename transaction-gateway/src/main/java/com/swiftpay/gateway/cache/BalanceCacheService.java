package com.swiftpay.gateway.cache;

import com.swiftpay.gateway.account.AccountBalanceReader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.Optional;

@Service
public class BalanceCacheService {

    private static final Logger log = LoggerFactory.getLogger(BalanceCacheService.class);

    private final StringRedisTemplate redis;
    private final AccountBalanceReader accountBalanceReader;
    private final Duration ttl;

    public BalanceCacheService(
            StringRedisTemplate redis,
            AccountBalanceReader accountBalanceReader,
            @Value("${swiftpay.balance-cache-ttl-seconds:60}") long balanceCacheTtlSeconds
    ) {
        this.redis = redis;
        this.accountBalanceReader = accountBalanceReader;
        this.ttl = Duration.ofSeconds(balanceCacheTtlSeconds);
    }

    public Optional<Long> getBalanceCents(String userId) {
        var key = balanceKey(userId);
        var cached = redis.opsForValue().get(key);
        if (cached != null) {
            try {
                return Optional.of(Long.parseLong(cached));
            } catch (NumberFormatException e) {
                log.warn("Invalid cached balance for {}, refreshing", userId);
            }
        }
        return accountBalanceReader.findBalanceCents(userId).map(balance -> {
            redis.opsForValue().set(key, Long.toString(balance), ttl);
            return balance;
        });
    }

    public static String balanceKey(String userId) {
        return "balance:" + userId;
    }
}
