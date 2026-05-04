# SwiftPay - Step-by-Step Run Guide

This document explains exactly how to run and verify the SwiftPay project on your machine.

## 1) Prerequisites

Make sure these commands work:

```bash
docker --version
docker-compose version
./mvnw -v
```

If `./mvnw` is not executable, run once:

```bash
chmod +x mvnw
```

## 2) Open project folder

```bash
cd /Users/rishikantsingh/Documents/swiftPay
```

## 3) Build + test the code (recommended first)

```bash
./mvnw verify
```

Expected result: build completes with `BUILD SUCCESS`.

## 4) Start the full platform

```bash
docker-compose up --build
```

This starts:
- PostgreSQL
- Redis
- Kafka + Zookeeper
- `ledger-service`
- `transaction-gateway`
- `analytics-worker`

Keep this terminal running.

## 5) Health checks (new terminal)

Open a second terminal and run:

```bash
cd /Users/rishikantsingh/Documents/swiftPay
curl -s http://localhost:8080/health
curl -s http://localhost:8081/health
curl -s http://localhost:8082/health
```

Expected: each returns JSON with a healthy status.

## 6) Open API documentation

- Gateway Swagger: `http://localhost:8080/swagger-ui/index.html`
- Ledger Swagger: `http://localhost:8081/swagger-ui/index.html`

## 7) Create a payment

Run:

```bash
curl -i -X POST http://localhost:8080/v1/payments \
  -H 'Content-Type: application/json' \
  -d '{
    "transactionId": "demo-001",
    "senderId": "user-alice",
    "receiverId": "user-bob",
    "amount": "5.00",
    "currency": "USD"
  }'
```

Expected:
- HTTP status `202 Accepted`
- response JSON containing `transactionId` and status `PENDING`

## 8) Verify ledger history

```bash
curl -s "http://localhost:8081/v1/users/user-alice/transactions"
curl -s "http://localhost:8081/v1/users/user-bob/transactions"
```

Expected:
- Alice shows a `DEBIT` transaction
- Bob shows a `CREDIT` transaction

## 9) Verify idempotency

Send the **same** payload again (same `transactionId` `demo-001`):

```bash
curl -i -X POST http://localhost:8080/v1/payments \
  -H 'Content-Type: application/json' \
  -d '{
    "transactionId": "demo-001",
    "senderId": "user-alice",
    "receiverId": "user-bob",
    "amount": "5.00",
    "currency": "USD"
  }'
```

Expected:
- request is not processed as a brand-new transfer
- response comes from idempotency protection path

## 10) Stop everything

In the terminal running compose, press `Ctrl + C`, then run:

```bash
cd /Users/rishikantsingh/Documents/swiftPay
docker-compose down
```

Optional cleanup (removes volumes too):

```bash
docker-compose down -v
```

## 11) Troubleshooting

### A) `zsh: command not found: docker`
- Install/start Docker tooling and reopen terminal.

### B) `docker compose` fails but `docker-compose` works
- Use `docker-compose` commands on this machine.

### C) Port already in use
- Stop conflicting service or change port mapping in `docker-compose.yml`.

### D) Build fails
- Re-run:
  ```bash
  ./mvnw -q verify
  ```
- Then retry:
  ```bash
  docker-compose up --build
  ```

