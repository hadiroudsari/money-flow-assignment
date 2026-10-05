package org.hadilta.moneyflow.client;

import org.hadilta.moneyflow.model.BalanceReport;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

@Component
public class BalanceReportClient {

    private final RestClient restClient;

    public BalanceReportClient(@Value("${money-flow.balance-report-api.base-url}") String baseURL) {
        this.restClient = RestClient.builder()
                .baseUrl(baseURL)
                .build();
    }

    public void sendReport(BalanceReport report) {
        restClient
                .post()
                .uri("/reports")
                .contentType(MediaType.APPLICATION_JSON)
                .body(report)
                .retrieve()
                .toBodilessEntity();
    }
}