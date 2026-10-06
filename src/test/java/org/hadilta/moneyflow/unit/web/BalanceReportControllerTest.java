package org.hadilta.moneyflow.unit.web;

import org.hadilta.moneyflow.business.CurrencyMismatchException;
import org.hadilta.moneyflow.business.MonthlyBalanceService;
import org.hadilta.moneyflow.model.BalanceReport;
import org.hadilta.moneyflow.web.BalanceReportController;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.YearMonth;

import static org.hamcrest.Matchers.containsString;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(BalanceReportController.class)
class BalanceReportControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private MonthlyBalanceService service;

    @Test
    void returnsGeneratedReport() throws Exception {
        var report = new BalanceReport(YearMonth.of(2026, 9), "EUR",
                new BigDecimal("1000"), new BigDecimal("300"), new BigDecimal("700"));
        when(service.generateMonthlyReport(YearMonth.of(2026, 9))).thenReturn(report);

        mockMvc.perform(post("/balance-reports/2026-09"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.month").value("2026-09"))
                .andExpect(jsonPath("$.balance").value(700));
    }

    @Test
    void returns422OnCurrencyMismatch() throws Exception {
        when(service.generateMonthlyReport(any()))
                .thenThrow(new CurrencyMismatchException("tx-usd", "USD", "EUR"));

        mockMvc.perform(post("/balance-reports/2026-09"))
                .andExpect(status().isUnprocessableContent())
                .andExpect(jsonPath("$.detail").value(containsString("tx-usd")));
    }

    @Test
    void returns400ForInvalidMonth() throws Exception {
        mockMvc.perform(post("/balance-reports/2026-13"))
                .andExpect(status().isBadRequest());

        verifyNoInteractions(service);
    }
}