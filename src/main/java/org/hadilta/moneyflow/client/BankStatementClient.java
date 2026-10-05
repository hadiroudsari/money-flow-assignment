package org.hadilta.moneyflow.client;

import org.hadilta.moneyflow.model.Transaction;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.time.YearMonth;
import java.util.List;

@Component
public class BankStatementClient {

    private final RestClient restClient;

    public BankStatementClient(@Value("${money-flow.bank-statement-api.base-url}") String baseURL) {
        this.restClient = RestClient.builder()
                .baseUrl(baseURL)
                .build();
    }

    public List<Transaction> getTransactions(YearMonth month) {
        return restClient
                .get()
                .uri("/statements/{month}/transactions", month)
                .retrieve()
                .body(new ParameterizedTypeReference<>() {
                      }
                );
    }
}
