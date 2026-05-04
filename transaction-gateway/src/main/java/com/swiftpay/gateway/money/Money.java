package com.swiftpay.gateway.money;

import com.swiftpay.gateway.web.error.BusinessRuleException;
import org.springframework.http.HttpStatus;

import java.math.BigDecimal;
import java.math.RoundingMode;

public final class Money {

    private Money() {
    }

    public static long toCents(BigDecimal majorUnits) {
        if (majorUnits == null) {
            throw new BusinessRuleException(HttpStatus.BAD_REQUEST, "INVALID_AMOUNT", "amount is required");
        }
        if (majorUnits.signum() <= 0) {
            throw new BusinessRuleException(HttpStatus.BAD_REQUEST, "INVALID_AMOUNT", "Amount must be positive");
        }
        if (majorUnits.scale() > 2) {
            throw new BusinessRuleException(
                    HttpStatus.BAD_REQUEST,
                    "INVALID_AMOUNT",
                    "Amount may have at most two decimal places"
            );
        }
        try {
            return majorUnits.movePointRight(2).setScale(0, RoundingMode.HALF_UP).longValueExact();
        } catch (ArithmeticException e) {
            throw new BusinessRuleException(
                    HttpStatus.BAD_REQUEST,
                    "INVALID_AMOUNT",
                    "Amount is out of supported range",
                    e
            );
        }
    }
}
