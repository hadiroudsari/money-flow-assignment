package org.hadilta.moneyflow.contracts;

import au.com.dius.pact.consumer.MockServer;
import au.com.dius.pact.consumer.dsl.PactDslJsonArray;
import au.com.dius.pact.consumer.dsl.PactDslJsonBody;
import au.com.dius.pact.consumer.dsl.PactDslWithProvider;
import au.com.dius.pact.consumer.junit5.PactConsumerTest;
import au.com.dius.pact.consumer.junit5.PactTestFor;
import au.com.dius.pact.core.model.V4Pact;
import au.com.dius.pact.core.model.annotations.Pact;
import org.hadilta.moneyflow.client.BankStatementClient;
import org.hadilta.moneyflow.model.CreditDebitIndicator;
import org.junit.jupiter.api.Test;

import java.time.YearMonth;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;


@PactConsumerTest
@PactTestFor(providerName = "bank-statement-api")
class BankStatementClientPactTest {

    @Pact(consumer = "money-flow")
    V4Pact monthlyTransactions(PactDslWithProvider builder) {

        var money = new PactDslJsonBody()
                .stringType("amount", "3500.00")
                .stringType("currency", "EUR");

        var body = PactDslJsonArray
                .arrayEachLike()
                .stringType("transactionId", "TX-12345")
                .date("bookingDate", "yyyy-MM-dd")
                .object("money", money)
                .stringMatcher("creditDebitIndicator", "CREDIT|DEBIT", "CREDIT")
                .stringType("description", "salary of September")
                .closeObject();

        return builder
                .given("transaction exist for September 2026")
                .uponReceiving("a request for September 2026 transactions")
                .path("/statements/2026-09/transactions")
                .method("GET")
                .willRespondWith()
                .status(200)
                .headers(Map.of("Content-type", "application/json"))
                .body(body)
                .toPact(V4Pact.class);
    }

    @Test
    @PactTestFor(pactMethod ="monthlyTransactions")
    void shouldFetchMonthlyTransaction(MockServer mockServer){

        var client= new BankStatementClient(mockServer.getUrl());
        var transactions= client.getTransactions(YearMonth.of(2026,9));

        assertThat(transactions).hasSize(1);

        var transaction=transactions.getFirst();

        assertThat(transaction.transactionId())
                .isEqualTo("TX-12345");

        assertThat(transaction.creditDebitIndicator())
                .isEqualTo(CreditDebitIndicator.CREDIT);

        assertThat(transaction.money().amount())
                .isEqualByComparingTo("3500.00");

    }
}