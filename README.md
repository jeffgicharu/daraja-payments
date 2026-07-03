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
- [ ] Daraja OAuth token client
- [ ] STK Push initiation endpoint
- [ ] Idempotent callback processing + audit
- [ ] Kafka payment events (outbox pattern)
- [ ] JWT-secured endpoints, OpenAPI docs
- [ ] CI/CD (GitHub Actions), k3d/Kubernetes deploy, AWS
