package org.hadilta.moneyflow.unit.business;

import org.hadilta.moneyflow.business.BankStatementProcessor;
import org.hadilta.moneyflow.business.CurrencyMismatchException;
import org.hadilta.moneyflow.model.BalanceReport;
import org.junit.jupiter.api.Test;

import java.time.YearMonth;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.hadilta.moneyflow.model.CreditDebitIndicator.CREDIT;
import static org.hadilta.moneyflow.model.CreditDebitIndicator.DEBIT;
import static org.hadilta.moneyflow.unit.business.TestTransactions.tx;

class BankStatementProcessorTest {

    private static final YearMonth MONTH = YearMonth.of(2026, 9);

    private final BankStatementProcessor processor = new BankStatementProcessor("EUR");

    @Test
    void sumIncomeWithSpendingAndComputeBalance() {
        var transactions = List.of(
                tx("1", "2500.0", "EUR", CREDIT),
                tx("2", "100.50", "EUR", DEBIT),
                tx("3", "49.50", "EUR", DEBIT),
                tx("4", "10.000", "EUR", CREDIT));

        BalanceReport report = processor.processMonthlyStatement(MONTH, transactions);

        assertThat(report.month()).isEqualTo(MONTH);
        assertThat(report.currency()).isEqualTo("EUR");
        assertThat(report.income()).isEqualByComparingTo("2510.00");
        assertThat(report.spending()).isEqualByComparingTo("150");
        assertThat(report.balance()).isEqualByComparingTo("2360");
    }

    @Test
    void emptyListYieldsToZeroReport() {
        BalanceReport report = processor.processMonthlyStatement(MONTH, List.of());

        assertThat(report.month()).isEqualTo(MONTH);
        assertThat(report.currency()).isEqualTo("EUR");
        assertThat(report.income()).isEqualByComparingTo("0");
        assertThat(report.spending()).isEqualByComparingTo("0");
        assertThat(report.balance()).isEqualByComparingTo("0");
    }

    @Test
    void balanceIsNegativeWhenSpendingExceedsIncome() {
        var transactions = List.of(
                tx("1", "100", "EUR", CREDIT),
                tx("2", "300", "EUR", DEBIT));

        BalanceReport report = processor.processMonthlyStatement(MONTH, transactions);

        assertThat(report.balance()).isEqualByComparingTo("-200");
    }

    @Test
    void rejectsTransactionInOtherCurrency() {
        var transactions = List.of(
                tx("1", "10", "EUR", CREDIT),
                tx("tx-123", "100", "USD", CREDIT));

        assertThatThrownBy(() -> processor.processMonthlyStatement(MONTH, transactions))
                .isInstanceOfSatisfying(CurrencyMismatchException.class, e -> {
                    assertThat(e.getTransactionId()).isEqualTo("tx-123");
                    assertThat(e.getActualCurrency()).isEqualTo("USD");
                    assertThat(e.getExpectedCurrency()).isEqualTo("EUR");
                });
    }

    @Test
    void sumsDecimalAmountsWithoutRoundingErrors() {
        var transactions = List.of(
                tx("1", "0.100", "EUR", CREDIT),
                tx("2", "0.20", "EUR", CREDIT));

        BalanceReport report = processor.processMonthlyStatement(MONTH, transactions);

        assertThat(report.income()).isEqualByComparingTo("0.30");
    }
}