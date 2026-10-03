package org.hadilta.moneyflow.client;

import org.hadilta.moneyflow.model.Transaction;

import org.springframework.core.ParameterizedTypeReference;
import org.springframework.web.client.RestClient;

import java.lang.reflect.Type;
import java.time.YearMonth;
import java.util.List;

public class BankStatementClient {

    private final RestClient restClient;

    public BankStatementClient(String baseURL) {
        this.restClient = RestClient.builder()
                .baseUrl(baseURL)
                .build();
    }

    public List<Transaction> getTransactions(YearMonth month) {
        return restClient
                .get()
                .uri("/statements/{month}/transactions", month)
                .retrieve()
                .body(new ParameterizedTypeReference<>() {}
                );
    }
}
