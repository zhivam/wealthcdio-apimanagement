# Banking Transaction Processor

A Java 17 / Spring Boot REST microservice implementing the attached coding-kata requirements: accounts with unique IDs and balances; deposits, withdrawals and transfers; validation against invalid amounts and overdrafts; timestamped per-account ledger; and balance/history query APIs.

The design prioritizes simple, readable code and explicit behaviour. It is an in-memory H2-backed local demo, not a production banking platform.

## Requirements and choices

- **Money:** `BigDecimal`, never floating point. Values must be strictly positive and have at most two decimal places.
- **Account identity:** caller-supplied unique account ID. Duplicate creation returns HTTP 409.
- **Transactions:** deposit, withdrawal and transfer operations run inside database transactions. Transfers update both balances and write both ledger entries atomically.
- **Concurrency:** write operations lock affected account rows. Transfers lock accounts in lexicographic ID order to reduce deadlock risk.
- **Ledger:** append-only transaction entries. A transfer creates a debit entry for the source and a credit entry for the destination, linked by the same reference ID.
- **Error contract:** validation errors return HTTP 400; missing accounts return 404; duplicate account IDs and business-rule conflicts (e.g. overdraft) return 409.
- **Scope limitations:** H2 is configured for local development; no authentication/authorization, external database migration tool, idempotency key, reversal workflow, or distributed messaging. Those should be considered before production use.

## Run

Requires JDK 17+ and Maven 3.9+.

```bash
mvn test
mvn spring-boot:run
```

H2 is in-memory, so data resets when the application restarts. H2 console is enabled at `/h2-console` for local development only. JDBC URL: `jdbc:h2:mem:bankdb`, username `sa`, blank password.

## API examples

Create an account:
```bash
curl -X POST http://localhost:8080/api/accounts -H 'Content-Type: application/json' \
  -d '{"id":"A-100","openingBalance":1000.00}'
```

Get balance:
```bash
curl http://localhost:8080/api/accounts/A-100
```

Deposit:
```bash
curl -X POST http://localhost:8080/api/accounts/A-100/deposits -H 'Content-Type: application/json' \
  -d '{"amount":100.00,"description":"Salary"}'
```

Withdraw:
```bash
curl -X POST http://localhost:8080/api/accounts/A-100/withdrawals -H 'Content-Type: application/json' \
  -d '{"amount":25.00,"description":"ATM withdrawal"}'
```

Transfer:
```bash
curl -X POST http://localhost:8080/api/transfers -H 'Content-Type: application/json' \
  -d '{"fromAccountId":"A-100","toAccountId":"A-200","amount":50.00,"description":"Rent"}'
```

Transaction history:
```bash
curl 'http://localhost:8080/api/accounts/A-100/transactions?page=0&size=20'
```

## Endpoint summary

| Method | Endpoint | Purpose |
|---|---|---|
| POST | `/api/accounts` | Create account with optional opening balance |
| GET | `/api/accounts/{id}` | Get account balance |
| POST | `/api/accounts/{id}/deposits` | Deposit |
| POST | `/api/accounts/{id}/withdrawals` | Withdraw |
| POST | `/api/transfers` | Transfer between accounts |
| GET | `/api/accounts/{id}/transactions?page=0&size=20` | Paginated ledger history |

## Tests

The tests document key behaviour: account creation, deposit, withdrawal, transfer ledger entries, overdraft rejection, invalid amounts, and missing accounts. For a production-ready extension, add idempotency and concurrent stress tests, database migrations, API security, audit/observability, and contract tests.
