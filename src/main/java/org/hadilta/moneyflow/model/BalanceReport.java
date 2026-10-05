package org.hadilta.moneyflow.model;

import java.math.BigDecimal;
import java.time.YearMonth;

public record BalanceReport(YearMonth month,
                            String currency,
                            BigDecimal income,
                            BigDecimal spending,
                            BigDecimal balance) {
}
