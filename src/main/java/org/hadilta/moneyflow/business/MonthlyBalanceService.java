package org.hadilta.moneyflow.business;

import org.hadilta.moneyflow.client.BalanceReportClient;
import org.hadilta.moneyflow.client.BankStatementClient;
import org.hadilta.moneyflow.model.BalanceReport;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import java.time.YearMonth;

@Service
public class MonthlyBalanceService {

    private static final Logger log = LoggerFactory.getLogger(MonthlyBalanceService.class);

    private final BankStatementClient bankStatementClient;
    private final BankStatementProcessor processor;
    private final BalanceReportClient balanceReportClient;

    public MonthlyBalanceService(BankStatementClient bankStatementClient,
                                 BankStatementProcessor processor,
                                 BalanceReportClient balanceReportClient) {
        this.bankStatementClient = bankStatementClient;
        this.processor = processor;
        this.balanceReportClient = balanceReportClient;
    }

    public BalanceReport generateMonthlyReport(YearMonth month) {
        var transactions = bankStatementClient.getTransactions(month);
        try {
            var report = processor.processMonthlyStatement(month, transactions);
            balanceReportClient.sendReport(report);
            log.info("Balance report sent for {}", month);
            return report;
        } catch (CurrencyMismatchException e) {
            log.error("Balance report not sent for {}: {}", month, e.getMessage());
            throw e;
        }
    }
}
