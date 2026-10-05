package org.hadilta.moneyflow.business;

public class CurrencyMismatchException extends RuntimeException {
    private final String transactionId;
    private final String actualCurrency;
    private final String expectedCurrency;

    public CurrencyMismatchException(String transactionId, String actualCurrency, String expectedCurrency) {
        super("Transaction %s has currency %s, expected account currency %s"
                .formatted(transactionId, actualCurrency, expectedCurrency));
        this.transactionId = transactionId;
        this.actualCurrency = actualCurrency;
        this.expectedCurrency = expectedCurrency;
    }

    public String getTransactionId() {
        return transactionId;
    }

    public String getActualCurrency() {
        return actualCurrency;
    }

    public String getExpectedCurrency() {
        return expectedCurrency;
    }
}

