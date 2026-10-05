package org.hadilta.moneyflow.business;

import org.hadilta.moneyflow.model.BalanceReport;
import org.hadilta.moneyflow.model.Transaction;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.YearMonth;
import java.util.List;

@Component
public class BankStatementProcessor {

    private final String accountCurrency;

    public BankStatementProcessor(@Value("${money-flow.account-currency}") String accountCurrency) {
        this.accountCurrency = accountCurrency;
    }

    public BalanceReport processMonthlyStatement(YearMonth month, List<Transaction> transactions) {

        var income = BigDecimal.ZERO;
        var spending = BigDecimal.ZERO;
        for (Transaction t : transactions) {
            validateCurrency(t);
            switch (t.creditDebitIndicator()) {
                case CREDIT -> income = income.add(t.money().amount());
                case DEBIT -> spending = spending.add(t.money().amount());
            }
        }
        return new BalanceReport(month, accountCurrency, income, spending, income.subtract(spending));
    }

    private void validateCurrency(Transaction t) {
        String actual = t.money().currency();
        if (!accountCurrency.equals(actual)) {
            throw new CurrencyMismatchException(t.transactionId(), actual, accountCurrency);
        }
    }
}

