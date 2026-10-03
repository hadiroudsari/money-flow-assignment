package org.hadilta.moneyflow.model;

import java.math.BigDecimal;

public record Money(
        BigDecimal amount,
        String currency
) {
}
