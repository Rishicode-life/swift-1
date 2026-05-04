package com.swiftpay.gateway.web.error;

public class IdempotencyInProgressException extends RuntimeException {

    public IdempotencyInProgressException(String message) {
        super(message);
    }
}
