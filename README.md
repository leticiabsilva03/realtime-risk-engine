# Realtime Risk Engine

A rule-based fraud detection engine for financial transactions. Receives a transaction via REST API, evaluates it against 7 configurable rules, and returns a risk decision with a score from 0 to 100, with full audit trail, Redis caching, circuit breaker, and Prometheus/Grafana observability.

Built to explore how fraud detection systems work in production fintechs: composable rules, observable scoring, zero-redeploy configuration.

![Java](https://img.shields.io/badge/Java-25-orange?style=flat-square)
![Spring Boot](https://img.shields.io/badge/Spring_Boot-4-green?style=flat-square)
![Docker](https://img.shields.io/badge/Docker-ready-blue?style=flat-square)
![Coverage](https://img.shields.io/badge/coverage-80%25_min-brightgreen?style=flat-square)

---

## How it works

Each transaction is scored by 7 independent rules. Scores are summed and mapped to a decision:

| Score   | Decision  |
|---------|-----------|
| 0 – 39  | `APPROVE` |
| 40 – 69 | `REVIEW`  |
| 70 – 100| `BLOCK`   |

Rules are configurable via `rules.yml` — no redeploy needed to adjust thresholds.

---

## Rules

| Rule | What it detects |
|------|----------------|
| `HighAmountRule` | Transaction above R$ 5,000 |
| `VelocityRule` | More than 5 transactions in 10 minutes |
| `DeviceFingerprintRule` | Same device used across multiple users |
| `GeolocationRule` | High-risk country or physically impossible travel |
| `OddHoursRule` | Transaction between 00:00 and 05:00 UTC |
| `NewMerchantRule` | First purchase at a new merchant with high amount |
| `BlacklistRule` | User, device or merchant on blocklist |

---

## Stack

| Layer | Technology |
|-------|-----------|
| API | Java 25 · Spring Boot 4 · REST |
| Persistence | PostgreSQL 15 |
| Cache | Redis 7 + Resilience4j circuit breaker |
| Observability | Micrometer · Prometheus · Grafana |
| Testing | JUnit · Testcontainers · JaCoCo (80% min) |
| Infrastructure | Docker · Docker Compose |

---

## Running locally

**Prerequisites:** Java 25, Docker, Maven

```bash
# Build
./mvnw clean package -DskipTests

# Start all services (API + PostgreSQL + Redis + Prometheus + Grafana)
docker-compose up -d
```

API available at `http://localhost:8080`.

---

## API

```
POST /api/v1/transactions/evaluate   → evaluate and persist to audit log
POST /api/v1/transactions/simulate   → evaluate without persisting
GET  /actuator/health                → health check
GET  /actuator/prometheus            → metrics
```

### Request example

```json
{
  "transactionId": "txn-001",
  "userId": "usr-123",
  "deviceId": "dev-abc",
  "amount": 9500.00,
  "merchantId": "mch-xyz",
  "merchant": "Store XYZ",
  "country": "NG",
  "transactionAt": "2025-02-06T02:30:00Z"
}
```

### Response example

```json
{
  "transactionId": "txn-001",
  "totalScore": 65,
  "decision": "REVIEW",
  "rulesVersion": "1.0.0",
  "simulated": false,
  "rulesEvaluated": [
    { "rule": "HighAmountRule",  "score": 30, "triggered": true,  "reason": "amount 9500.00 exceeds threshold 5000.00" },
    { "rule": "VelocityRule",    "score": 0,  "triggered": false, "reason": null },
    { "rule": "GeolocationRule", "score": 20, "triggered": true,  "reason": "high-risk country: NG" },
    { "rule": "OddHoursRule",    "score": 15, "triggered": true,  "reason": "transaction at 02:00 UTC falls within odd-hours window" }
  ],
  "evaluatedAt": "2025-02-06T02:30:01Z"
}
```

---

## Observability

| Service | URL |
|---------|-----|
| Grafana | http://localhost:3000 (admin/admin) |
| Prometheus | http://localhost:9090 |
| Actuator | http://localhost:8080/actuator/health |

---

## Tests

```bash
# Unit tests only
./mvnw test

# Unit + integration tests (requires Docker)
./mvnw verify
```

Minimum 80% coverage enforced by JaCoCo — build fails if not met.

---

## Project structure

```
src/main/java/com/fraudengine/
├── api/              # Controllers, DTOs, error handling
├── config/           # RulesConfig (@ConfigurationProperties)
├── domain/           # Business rules, models, services
├── engine/           # RuleEngine and the 7 rules
└── infrastructure/
    ├── cache/        # RedisCacheService with circuit breaker
    ├── observability/ # EvaluationLogger
    └── persistence/  # AuditEntity, AuditMapper
```
