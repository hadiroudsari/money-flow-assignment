package org.hadilta.moneyflow.model;

import java.time.LocalDate;

public record Transaction(
        String transactionId,
        LocalDate bookingDate,
        Money money,
        CreditDebitIndicator creditDebitIndicator,
        String description
) {
}
