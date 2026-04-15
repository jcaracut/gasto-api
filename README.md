# Gasto — Expense Tracker API

RESTful API for an offline-first mobile expense tracker. Built with Spring Boot 3.x, PostgreSQL, Redis, and Kafka.

## Tech Stack

| Layer | Technology |
|---|---|
| Runtime | Java 21 |
| Framework | Spring Boot 3.4 |
| Database | PostgreSQL 16 |
| Migrations | Flyway |
| Cache | Redis 7 |
| Messaging | Apache Kafka (Confluent 7.7) |
| Tracing | Micrometer + Brave + Zipkin |
| Auth | Stateless JWT (HS256) |
| Build | Maven 3.9+ (wrapper included) |
| Containers | Docker + Docker Compose |

---

## Running with Docker (recommended)

This is the fastest way to get everything running. No local JDK, PostgreSQL, Redis, or Kafka required.

### 1. Configure secrets

```bash
cp .env.example .env
```

Open `.env` and replace the default `JWT_SECRET` with a strong random value:

```bash
# Generate a secure secret
openssl rand -hex 32
```

### 2. Start all services

```bash
docker compose up --build
```

This starts six containers in dependency order:

| Container | Image | Port | Purpose |
|---|---|---|---|
| `gasto-postgres` | `postgres:16-alpine` | `5432` | Primary database |
| `gasto-redis` | `redis:7-alpine` | `6379` | Expense list & summary cache |
| `gasto-zookeeper` | `cp-zookeeper:7.7.0` | — | Kafka coordination |
| `gasto-kafka` | `cp-kafka:7.7.0` | `9092` | Expense event streaming |
| `gasto-zipkin` | `openzipkin/zipkin:3` | `9411` | Distributed trace UI |
| `gasto-api` | built from `Dockerfile` | `8080` | Spring Boot API |

The API waits for PostgreSQL, Redis, and Kafka health checks before starting. Flyway migrations run automatically on startup.

### 3. Verify

```
API:    http://localhost:8080/actuator/health
Zipkin: http://localhost:9411
```

### Useful Docker commands

```bash
# Start only infrastructure (run the API locally with mvn spring-boot:run)
docker compose up postgres redis kafka zipkin

# Tail API logs
docker compose logs -f api

# Stop and remove containers (keeps volumes)
docker compose down

# Stop and wipe all data volumes
docker compose down -v

# Rebuild the API image after code change
docker compose up --build api
```

---

## Running locally (without Docker)

Requires: JDK 21, PostgreSQL 16, Redis 7, Kafka on `localhost:9092`.

```bash
# 1. Clone
git clone <repo-url>
cd Gasto

# 2. Set environment variables
export DB_USERNAME=postgres
export DB_PASSWORD=yourpassword
export JWT_SECRET=$(openssl rand -hex 32)

# 3. Run
./mvnw spring-boot:run
```

---

## Build & Test

```bash
./mvnw clean verify     # compile + all tests
./mvnw test             # tests only
./mvnw package          # build fat jar → target/gasto-api-*.jar
```

---

## API Endpoints

All responses wrap data in:
```json
{ "success": true, "data": {}, "message": "...", "timestamp": "..." }
```
Error responses also include `"traceId"` for correlating with Zipkin traces.

### Authentication — public

| Method | Path | Description |
|---|---|---|
| `POST` | `/api/v1/auth/register` | Create account, returns JWT |
| `POST` | `/api/v1/auth/login` | Sign in, returns JWT |

### Users — requires `Authorization: Bearer <token>`

| Method | Path | Description |
|---|---|---|
| `GET` | `/api/v1/users/me` | Get current user profile |
| `PUT` | `/api/v1/users/me` | Update full name |

### Categories — requires auth

| Method | Path | Description |
|---|---|---|
| `GET` | `/api/v1/categories` | List all predefined categories |

### Expenses — requires auth

| Method | Path | Description |
|---|---|---|
| `GET` | `/api/v1/expenses` | List expenses (`?from=YYYY-MM-DD&to=YYYY-MM-DD`) |
| `POST` | `/api/v1/expenses` | Create expense |
| `GET` | `/api/v1/expenses/{id}` | Get single expense |
| `PUT` | `/api/v1/expenses/{id}` | Update expense |
| `DELETE` | `/api/v1/expenses/{id}` | Soft-delete expense |
| `POST` | `/api/v1/expenses/sync` | Bulk upsert for offline sync |
| `GET` | `/api/v1/expenses/summary` | Monthly totals by category (`?year=&month=`) |

### Actuator — public

| Path | Description |
|---|---|
| `/actuator/health` | Liveness / readiness |
| `/actuator/metrics` | Micrometer metrics |
| `/actuator/prometheus` | Prometheus scrape endpoint |

### Quick Example

```bash
# Register
curl -X POST http://localhost:8080/api/v1/auth/register \
  -H "Content-Type: application/json" \
  -d '{"fullName":"Jane Doe","email":"jane@example.com","password":"secret123"}'

# Login — save the token
TOKEN=$(curl -s -X POST http://localhost:8080/api/v1/auth/login \
  -H "Content-Type: application/json" \
  -d '{"email":"jane@example.com","password":"secret123"}' \
  | jq -r '.data.accessToken')

# List categories to get a categoryId
curl -H "Authorization: Bearer $TOKEN" http://localhost:8080/api/v1/categories

# Create an expense
curl -X POST http://localhost:8080/api/v1/expenses \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d '{"categoryId":"<category-id>","amount":12.50,"description":"Coffee","expenseDate":"2026-04-15"}'

# Sync offline expenses (bulk upsert — provide client-side UUIDs for idempotency)
curl -X POST http://localhost:8080/api/v1/expenses/sync \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "expenses": [
      {"id":"550e8400-e29b-41d4-a716-446655440000","categoryId":"<id>","amount":5.00,"description":"Bus","expenseDate":"2026-04-10"},
      {"id":"550e8400-e29b-41d4-a716-446655440001","categoryId":"<id>","amount":8.50,"description":"Lunch","expenseDate":"2026-04-11"}
    ]
  }'

# Monthly summary
curl -H "Authorization: Bearer $TOKEN" \
  "http://localhost:8080/api/v1/expenses/summary?year=2026&month=4"
```

---

## Redis — Caching

Redis caches two query shapes to avoid repeated DB hits:

| Cache key pattern | TTL | Evicted when |
|---|---|---|
| `expenses::list::{userId}::{from}::{to}` | 10 min | Any write (create / update / delete / sync) |
| `expenses::summary::{userId}::{year}-{month}` | 30 min | Any write |

Configuration in `application.yml`:
```yaml
app:
  cache:
    expense-list-ttl-minutes: 10
    expense-summary-ttl-minutes: 30
```

All Redis operations are wrapped in try/catch — a Redis outage degrades gracefully (falls back to DB) without failing requests.

**Inspect cache at runtime:**
```bash
# Connect to the Redis container
docker compose exec redis redis-cli

# List all expense cache keys
KEYS "expenses::*"

# Check TTL on a key
TTL "expenses::list::some-uuid::2026-04-01::2026-04-30"

# Flush all (dev only)
FLUSHALL
```

---

## Kafka — Event Streaming

Every create and update on an expense publishes a JSON message to the `expense-events` topic. Publishing is `@Async` and fire-and-forget — a Kafka outage does not fail the HTTP request.

### Event schema

```json
{
  "eventType": "EXPENSE_CREATED",
  "expenseId": "uuid",
  "userId": "uuid",
  "categoryId": "uuid",
  "categoryName": "Food & Dining",
  "amount": 12.50,
  "expenseDate": "2026-04-15",
  "occurredAt": "2026-04-15T10:30:00Z"
}
```

`eventType` is either `EXPENSE_CREATED` or `EXPENSE_UPDATED`.

### Topic configuration

| Property | Value |
|---|---|
| Topic name | `expense-events` |
| Partitions | 3 |
| Replication | 1 (increase for production) |
| Retention | 168 hours (7 days) |

**Inspect messages at runtime:**
```bash
# List topics
docker compose exec kafka kafka-topics \
  --bootstrap-server localhost:9092 --list

# Consume messages from the beginning
docker compose exec kafka kafka-console-consumer \
  --bootstrap-server localhost:9092 \
  --topic expense-events \
  --from-beginning \
  --property print.key=true

# Describe the topic
docker compose exec kafka kafka-topics \
  --bootstrap-server localhost:9092 \
  --describe --topic expense-events
```

---

## Distributed Tracing

Every request is automatically traced with Micrometer + Brave. Spans are exported to Zipkin.

- **Trace ID** is included in every error response body under the `traceId` key
- **Log lines** print `[traceId=... spanId=...]` for easy grep correlation
- **Sampling**: 100% in dev (`management.tracing.sampling.probability: 1.0`) — lower to `0.1` in production

Open the Zipkin UI: **http://localhost:9411**

To trace a specific failed request, copy the `traceId` from the error response and search for it in Zipkin.

---

## Architecture

The project follows **Domain-Driven Design** with strict layering:

```
com.gasto/
├── domain/          ← Pure Java: entities, repository interfaces, domain exceptions
├── application/     ← Spring services + DTOs; depends only on domain interfaces
├── infrastructure/  ← JPA, Redis, Kafka, Security implementations
└── presentation/    ← REST controllers + GlobalExceptionHandler
```

Key rules:
- `domain/` has **zero** Spring annotations
- Controllers never hold business logic — everything delegates to a service
- All writes evict the Redis cache and publish a Kafka event (`expense-events` topic)
- Expenses use **soft delete** (`deleted_at`) to support mobile sync reconciliation

---

## Database Migrations

Migration files are in `src/main/resources/db/migration/`:

| File | Description |
|---|---|
| `V1__init_schema.sql` | Creates `users`, `categories`, `expenses` tables |
| `V2__seed_categories.sql` | Inserts 10 predefined categories |

To add a new migration, create `V{n+1}__description.sql` — never edit applied files.

---

## Configuration Reference

All settings in `src/main/resources/application.yml`. Override via environment variables:

| Variable | Purpose | Default |
|---|---|---|
| `DB_USERNAME` | PostgreSQL username | `postgres` |
| `DB_PASSWORD` | PostgreSQL password | `postgres` |
| `REDIS_HOST` | Redis host | `localhost` |
| `REDIS_PORT` | Redis port | `6379` |
| `REDIS_PASSWORD` | Redis password (leave blank if none) | `` |
| `KAFKA_BOOTSTRAP_SERVERS` | Kafka broker address | `localhost:9092` |
| `ZIPKIN_URL` | Zipkin span collector endpoint | `http://localhost:9411/api/v2/spans` |
| `JWT_SECRET` | HS256 signing key (hex-encoded, min 256 bits) | see `.env.example` |
| `JWT_EXPIRATION_MS` | Token lifetime in milliseconds | `86400000` (24 h) |

---

## Copilot Customizations

The `.github/` directory contains AI-assisted development tooling:

| Path | Purpose |
|---|---|
| `.github/copilot-instructions.md` | Always-on workspace rules loaded by Copilot |
| `.github/agents/backend-dev.agent.md` | Agent for implementing new features end-to-end |
| `.github/agents/test-writer.agent.md` | Agent for writing/fixing tests |
| `.github/agents/db-migration.agent.md` | Agent for creating Flyway migrations |
| `.github/prompts/new-feature.prompt.md` | `/new-feature` — scaffold a feature across all layers |
| `.github/prompts/write-tests.prompt.md` | `/write-tests` — generate tests for a class or method |
| `.github/prompts/add-migration.prompt.md` | `/add-migration` — create a numbered migration file |
