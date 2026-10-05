package org.hadilta.moneyflow.contracts;

import au.com.dius.pact.consumer.MockServer;
import au.com.dius.pact.consumer.dsl.PactDslJsonBody;
import au.com.dius.pact.consumer.dsl.PactDslWithProvider;
import au.com.dius.pact.consumer.junit5.PactConsumerTest;
import au.com.dius.pact.consumer.junit5.PactTestFor;
import au.com.dius.pact.core.model.V4Pact;
import au.com.dius.pact.core.model.annotations.Pact;
import org.hadilta.moneyflow.client.BalanceReportClient;
import org.hadilta.moneyflow.model.BalanceReport;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.YearMonth;

import static org.assertj.core.api.Assertions.assertThatCode;

@PactConsumerTest
@PactTestFor(providerName = "balance-report-api")
class BalanceReportClientPactTest {

    @Pact(consumer = "money-flow")
    V4Pact sendMonthlyReport(PactDslWithProvider builder) {

        var body = new PactDslJsonBody()
                .stringMatcher("month", "\\d{4}-\\d{2}", "2026-09")
                .stringMatcher("currency", "[A-Z]{3}", "EUR")
                .numberType("income", new BigDecimal("2510.00"))
                .numberType("spending", new BigDecimal("150.00"))
                .numberType("balance", new BigDecimal("2360.00"));

        return builder
                .uponReceiving("a monthly balance report for September 2026")
                .path("/reports")
                .method("POST")
                .headers("Content-Type", "application/json")
                .body(body)
                .willRespondWith()
                .status(201)
                .toPact(V4Pact.class);
    }

    @Test
    @PactTestFor(pactMethod = "sendMonthlyReport")
    void shouldSendMonthlyReport(MockServer mockServer) {

        var client = new BalanceReportClient(mockServer.getUrl());
        var report = new BalanceReport(YearMonth.of(2026, 9), "EUR",
                new BigDecimal("2510.00"), new BigDecimal("150.00"), new BigDecimal("2360.00"));

        assertThatCode(() -> client.sendReport(report))
                .doesNotThrowAnyException();
    }
}