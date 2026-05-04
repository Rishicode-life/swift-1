package com.swiftpay.gateway.account;

import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import java.util.Optional;

@Component
public class AccountBalanceReader {

    private final JdbcTemplate jdbcTemplate;

    public AccountBalanceReader(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public Optional<Long> findBalanceCents(String userId) {
        var sql = "SELECT balance_cents FROM accounts WHERE user_id = ?";
        try {
            return Optional.ofNullable(jdbcTemplate.queryForObject(sql, Long.class, userId));
        } catch (EmptyResultDataAccessException e) {
            return Optional.empty();
        }
    }
}
