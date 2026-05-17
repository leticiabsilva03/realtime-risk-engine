# Realtime Risk Engine

Motor de regras para avaliação de risco em transações financeiras em tempo real.

Recebe uma transação via API REST, executa 7 regras configuráveis e retorna uma decisão com score de 0 a 100.

---

## Decisões

| Score | Decisão |
|-------|---------|
| 0 – 39 | APPROVE |
| 40 – 69 | REVIEW |
| 70 – 100 | BLOCK |

---

## Regras

| Regra | O que detecta |
|-------|--------------|
| HighAmountRule | Valor acima de R$ 5.000 |
| VelocityRule | Mais de 5 transações em 10 minutos |
| DeviceFingerprintRule | Mesmo device em múltiplos usuários |
| GeolocationRule | País de risco ou viagem fisicamente impossível |
| OddHoursRule | Transação entre 00h e 05h UTC |
| NewMerchantRule | Primeira compra em loja nova com valor alto |
| BlacklistRule | Usuário, device ou loja na lista negra |

Todas as regras são configuráveis via `rules.yml`, não sendo necessário redeploy para ajustar thresholds.

---

## Stack

Java 25 · Spring Boot 4 · PostgreSQL 15 · Redis 7 · Resilience4j · Testcontainers · Micrometer · Prometheus · Grafana · Docker

---

## Como rodar

**Pré-requisitos:** Java 25, Docker, Maven

```bash
# Empacota
./mvnw clean package -DskipTests

# Sobe tudo (API + PostgreSQL + Redis + Prometheus + Grafana)
docker-compose up -d
```

A API estará disponível em `http://localhost:8080`.

---

## Endpoints

```
POST /api/v1/transactions/evaluate   → avalia e persiste no audit
POST /api/v1/transactions/simulate   → avalia sem persistir
GET  /actuator/health                → saúde da aplicação
GET  /actuator/prometheus            → métricas
```

### Exemplo de request

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

### Exemplo de response

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

## Monitoramento

| Serviço | URL |
|---------|-----|
| Grafana | http://localhost:3000 (admin/admin) |
| Prometheus | http://localhost:9090 |
| Actuator | http://localhost:8080/actuator/health |

---

## Testes

```bash
# Unitários
./mvnw test

# Unitários + integração (requer Docker)
./mvnw verify
```

Cobertura mínima: 80% (JaCoCo falha o build se não atingir).

---

## Estrutura

```
src/main/java/com/fraudengine/
├── api/          # Controller, DTOs, tratamento de erros
├── config/       # RulesConfig (@ConfigurationProperties)
├── domain/       # Regras de negócio, modelos, serviços
├── engine/       # RuleEngine e as 7 regras
└── infrastructure/
    ├── cache/        # RedisCacheService com Circuit Breaker
    ├── observability/ # EvaluationLogger
    └── persistence/   # AuditEntity, AuditMapper
```

