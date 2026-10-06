# Money-flow
Fetches the bank transactions of a month from an external bank statement API,
calculates income, spending and balance, and sends the result as a report to
another external API.

### What problem does this application try to solve?  
A service that needs monthly balance reports (possibly a third party) should not
need access to someone's bank transactions. money-flow sits in the middle: it
fetches the transactions of a month from the bank statement API, calculates
income, spending and balance, and sends only these totals to the report API.
In addition, the report calculation might not belong to the domain the third-party 
service works in. Keeping it there could start bloating a microservice that was created
for a different reason.  

### What kind of architecture was used to achieve the solution?   
A stateless microservice with a layered architecture: `trigger` (REST endpoint)
→ `business` (flow and calculation) → `client` (HTTP clients for the external
APIs).  
I didn't use ports and adapters (hexagonal architecture): with one
implementation per external API, the extra interfaces would add complexity for such a small application.

### How to test your application?  
The application is tested on three levels: unit 
tests for the calculation, the flow and the REST 
endpoint; Pact contract tests for the communication 
with the two external APIs; and a component test that
runs the whole application against WireMock stubs.  
Run all tests:

```bash
./mvnw clean test
```

Run the whole application with stubbed external APIs:

```bash
docker compose up --build
```

This starts the application and two WireMock containers that stand in for the
bank statement API and the report API (stubs in `wiremock/`). The image build
also runs the tests. Then trigger a report:

```bash
curl -i -X POST http://localhost:8080/balance-reports/2026-09
```

### If the application must be deployed to a server in a remote location, how would you do it? 
To deploy it, I would set up a CI/CD pipeline (e.g. CircleCI)
that on every push builds the image, pushes it to a container registry, and
deploys it to the server: a single server with Docker, or a platform like
Kubernetes. The URLs of the external APIs and the account currency are passed as
environment variables, so the same image runs in every environment.
`/actuator/health` is used for health checks.  


### There is no real API to connect to. Think about how you can start implementing your solution before other parties are ready with their implementations?  
I used consumer-driven contracts with Pact. The contract tests 
describe exactly what my service sends to the two external APIs and what it expects back,
and Pact generates contract files from that. The teams building those APIs can run the contracts
against their own implementations to check that they match what I expect,
without waiting for my service or a shared test environment.  
After running the tests, Pact writes the two contract files to `target/pacts/`
(`money-flow-bank-statement-api.json` and `money-flow-balance-report-api.json`).
These can be handed over to the teams that will implement the two APIs.

### Additional notes

- **Smaller image with `jlink`:** instead of a full JRE, the image contains a Java
   runtime built with `jlink` with only the 19 modules the application needs. The
   runtime shrank from 192 MB to 65 MB.
- **A plain loop for the calculation:** a stream version would need two passes
  (one per CREDIT/DEBIT sum) plus a separate currency check. The `for` loop does
  all of it in one pass and is easy to read, so I preferred it rather than this stream version:
  ```java
  var income = sum(transactions, CREDIT);
  var spending = sum(transactions, DEBIT);

  private BigDecimal sum(List<Transaction> transactions, CreditDebitIndicator indicator) {
      return transactions.stream()
              .filter(t -> t.creditDebitIndicator() == indicator)
              .map(t -> t.money().amount())
              .reduce(BigDecimal.ZERO, BigDecimal::add);
  }
  ```
  I could have used modern syntax with Collectors.teeing to calculate both sums in one pass
  ,but it would hurt readability, since most developers would need to look up how teeing works.  
  ```java
  validateCurrencies(transactions);

  return transactions.stream()
          .collect(Collectors.teeing(
                  sumOf(CREDIT),
                  sumOf(DEBIT),
                  (income, spending) -> new BalanceReport(month, accountCurrency,
                          income, spending, income.subtract(spending))));

  private static Collector<Transaction, ?, BigDecimal> sumOf(CreditDebitIndicator indicator) {
      return Collectors.filtering(t -> t.creditDebitIndicator() == indicator,
              Collectors.mapping(t -> t.money().amount(),
                      Collectors.reducing(BigDecimal.ZERO, BigDecimal::add)));
  }
  ```
- **Statement size:** The task gives no size for a statement, so I assumed a normal
  monthly size and fetch it with one GET request. For very large statements, the
  bank API would need pagination, and the transactions would be fetched and
  summed page by page instead of loading the whole month into memory.  
