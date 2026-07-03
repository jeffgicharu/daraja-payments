# daraja-payments

A production-grade M-Pesa payments microservice built with **Java 17 / Spring Boot 4**, integrating the [Safaricom Daraja API](https://developer.safaricom.co.ke) (sandbox) for Lipa na M-Pesa Online (STK Push).

## Architecture

- **REST API** — initiate STK Push payments, query transaction status
- **Idempotent callback handling** — Daraja result callbacks deduplicated on `CheckoutRequestID`, with a full raw-payload audit log
- **MySQL 8** — transaction + audit persistence, schema managed by Flyway migrations
- **Kafka** — payment lifecycle events (`payment.initiated`, `payment.completed`, `payment.failed`) via transactional outbox
- **Spring Security + JWT** — secured API endpoints
- **Docker Compose** — full local stack (app, MySQL, Kafka)

## Getting started

```bash
# 1. Start infrastructure
docker compose up -d

# 2. Configure Daraja sandbox credentials
cp .env.example .env   # then fill in keys from developer.safaricom.co.ke

# 3. Run
./mvnw spring-boot:run

# 4. Verify
curl http://localhost:8080/actuator/health
```

## Testing

```bash
./mvnw test
```

Unit tests: JUnit 5 + Mockito. Integration tests: Testcontainers (MySQL, Kafka).

## Status

- [x] Project scaffold, Docker Compose stack, Flyway V1 schema — boot-verified against live MySQL
- [x] Daraja OAuth token client (cached, auto-refresh, thread-safe) — 4 unit tests
- [x] STK Push initiation endpoint — **verified live against the Daraja sandbox** (HTTP 202, real `CheckoutRequestID`)
- [x] Idempotent callback processing + audit — **verified with Daraja's real callback** delivered via tunnel (PENDING → FAILED/1037 for the test MSISDN), and a replayed callback rejected as `DUPLICATE` without state corruption
- [x] Kafka payment events via **transactional outbox** — events written in the same DB transaction as the state change, published by a scheduled relay (at-least-once, ordered per aggregate), consumed by a notifications consumer. **Live-verified**: real Daraja callback produced `payment.initiated` + `payment.failed` on the `payments.events` topic.
- [x] **JWT security** (HS256 resource server + client-credentials token endpoint). Callback webhook and health probes public; everything else requires a Bearer token — verified live (401 without, 202 with).
- [x] **Testcontainers integration test** — real MySQL + Kafka containers, WireMock as Daraja: token → authenticated STK push → callback → COMPLETED → Kafka events consumed and asserted.
- [x] **23 tests green** (JUnit 5, Mockito, MockRestServiceServer, `@WebMvcTest`, Testcontainers, Awaitility, WireMock)
- [ ] OpenAPI docs
- [ ] CI/CD (GitHub Actions), k3d/Kubernetes deploy, AWS
