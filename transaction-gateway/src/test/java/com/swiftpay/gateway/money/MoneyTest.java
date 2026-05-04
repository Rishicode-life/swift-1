package com.swiftpay.gateway.money;

import com.swiftpay.gateway.web.error.BusinessRuleException;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class MoneyTest {

    @Test
    void convertsMajorUnitsToCents() {
        assertEquals(2550L, Money.toCents(new BigDecimal("25.50")));
        assertEquals(1L, Money.toCents(new BigDecimal("0.01")));
    }

    @Test
    void rejectsSubCentPrecision() {
        assertThrows(BusinessRuleException.class, () -> Money.toCents(new BigDecimal("0.001")));
    }

    @Test
    void convertsWholeDollars() {
        assertEquals(1_000_000L, Money.toCents(new BigDecimal("10000.00")));
    }
}
