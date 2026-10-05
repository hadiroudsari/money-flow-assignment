package org.hadilta.moneyflow.component;

import com.github.tomakehurst.wiremock.junit5.WireMockExtension;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;

import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.wireMockConfig;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class MonthlyBalanceComponentTest {

    @RegisterExtension
    static WireMockExtension bankApi = WireMockExtension.newInstance()
            .options(wireMockConfig().dynamicPort()).build();

    @RegisterExtension
    static WireMockExtension reportApi = WireMockExtension.newInstance()
            .options(wireMockConfig().dynamicPort()).build();

    @DynamicPropertySource
    static void apiUrls(DynamicPropertyRegistry registry) {
        registry.add("money-flow.bank-statement-api.base-url", bankApi::baseUrl);
        registry.add("money-flow.balance-report-api.base-url", reportApi::baseUrl);
    }

    @Autowired
    private MockMvc mockMvc;

    @Test
    void generatesAndSendsMonthlyReport() throws Exception {
        bankApi.stubFor(get("/statements/2026-09/transactions").willReturn(okJson("""
                [
                  {"transactionId": "TX-1", "bookingDate": "2026-09-01",
                   "money": {"amount": "1000.00", "currency": "EUR"},
                   "creditDebitIndicator": "CREDIT", "description": "salary"},
                  {"transactionId": "TX-2", "bookingDate": "2026-09-05",
                   "money": {"amount": "300.00", "currency": "EUR"},
                   "creditDebitIndicator": "DEBIT", "description": "rent"}
                ]
                """)));
        reportApi.stubFor(post("/reports").willReturn(created()));

        mockMvc.perform(MockMvcRequestBuilders.post("/balance-reports/2026-09"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.balance").value(700.00));

        reportApi.verify(postRequestedFor(urlEqualTo("/reports"))
                .withRequestBody(equalToJson("""
                        {"month": "2026-09", "currency": "EUR",
                         "income": 1000.00, "spending": 300.00, "balance": 700.00}
                        """)));
    }

    @Test
    void returns502WhenBankApiFails() throws Exception {
        bankApi.stubFor(get("/statements/2026-09/transactions").willReturn(serverError()));

        mockMvc.perform(MockMvcRequestBuilders.post("/balance-reports/2026-09"))
                .andExpect(status().isBadGateway());

        reportApi.verify(0, postRequestedFor(urlEqualTo("/reports")));
    }
}
