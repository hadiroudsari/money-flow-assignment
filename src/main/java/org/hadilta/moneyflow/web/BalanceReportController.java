package org.hadilta.moneyflow.trigger;

import org.hadilta.moneyflow.business.MonthlyBalanceService;
import org.hadilta.moneyflow.model.BalanceReport;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.YearMonth;

@RestController
public class BalanceReportController {

    private final MonthlyBalanceService service;

    public BalanceReportController(MonthlyBalanceService service) {
        this.service = service;
    }

    @PostMapping("/balance-reports/{month}")
    public BalanceReport generateReport(@PathVariable YearMonth month) {
        return service.generateMonthlyReport(month);
    }
}
