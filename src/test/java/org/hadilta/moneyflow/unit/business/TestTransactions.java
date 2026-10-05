package org.hadilta.moneyflow.unit.business;

import org.hadilta.moneyflow.model.CreditDebitIndicator;
import org.hadilta.moneyflow.model.Money;
import org.hadilta.moneyflow.model.Transaction;
import java.math.BigDecimal;
import java.time.LocalDate;

public final class TestTransactions {

    private TestTransactions() {
    }

    public static Transaction tx(String id, String amount, String currency, CreditDebitIndicator indicator) {
        return new Transaction(id, LocalDate.of(2026, 9, 15),
                new Money(new BigDecimal(amount), currency), indicator, "test");
    }
}