package org.hadilta.moneyflow.unit.business;

import org.hadilta.moneyflow.business.BankStatementProcessor;
import org.hadilta.moneyflow.business.CurrencyMismatchException;
import org.hadilta.moneyflow.business.MonthlyBalanceService;
import org.hadilta.moneyflow.client.BalanceReportClient;
import org.hadilta.moneyflow.client.BankStatementClient;
import org.hadilta.moneyflow.model.BalanceReport;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;

import java.math.BigDecimal;
import java.time.YearMonth;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.hadilta.moneyflow.model.CreditDebitIndicator.CREDIT;
import static org.hadilta.moneyflow.model.CreditDebitIndicator.DEBIT;
import static org.hadilta.moneyflow.unit.business.TestTransactions.tx;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith({MockitoExtension.class, OutputCaptureExtension.class})
class MonthlyBalanceServiceTest {

    private static final YearMonth MONTH = YearMonth.of(2026, 9);

    @Mock
    private BankStatementClient bankStatementClient;

    @Mock
    private BalanceReportClient balanceReportClient;

    private MonthlyBalanceService service;

    @BeforeEach
    void setUp() {
        service = new MonthlyBalanceService(bankStatementClient, new BankStatementProcessor("EUR"), balanceReportClient);
    }

    @Test
    void fetchesTransactionsAndSendsComputedReport() {
        when(bankStatementClient.getTransactions(MONTH)).thenReturn(List.of(
                tx("1", "1000", "EUR", CREDIT),
                tx("2", "300", "EUR", DEBIT)));

        BalanceReport report = service.generateMonthlyReport(MONTH);

        var expected = new BalanceReport(MONTH, "EUR",
                new BigDecimal("1000"), new BigDecimal("300"), new BigDecimal("700"));
        assertThat(report).isEqualTo(expected);
        verify(balanceReportClient).sendReport(expected);
    }

    @Test
    void doesNotSendReportWhenCurrencyMismatches(CapturedOutput output) {
        when(bankStatementClient.getTransactions(MONTH)).thenReturn(List.of(
                tx("tx-usd", "10", "USD", CREDIT)));

        assertThatThrownBy(() -> service.generateMonthlyReport(MONTH))
                .isInstanceOf(CurrencyMismatchException.class);

        verify(balanceReportClient, never()).sendReport(any());
        assertThat(output)
                .contains("ERROR")
                .contains("not sent for 2026-09")
                .contains("tx-usd");
    }

    @Test
    void doesNotSendReportWhenFetchingTransactionsFails() {
        when(bankStatementClient.getTransactions(MONTH)).thenThrow(new RuntimeException("bank API down"));

        assertThatThrownBy(() -> service.generateMonthlyReport(MONTH))
                .hasMessage("bank API down");

        verify(balanceReportClient, never()).sendReport(any());
    }

    @Test
    void propagatesFailureWhenSendingReportFails() {
        when(bankStatementClient.getTransactions(MONTH)).thenReturn(List.of(
                tx("1", "100", "EUR", CREDIT)));
        doThrow(new RuntimeException("report API down"))
                .when(balanceReportClient).sendReport(any());

        assertThatThrownBy(() -> service.generateMonthlyReport(MONTH))
                .hasMessage("report API down");
    }
}