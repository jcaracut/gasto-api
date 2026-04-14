# Gasto ? Expense Tracker API

RESTful API for an offline-first mobile expense tracker. Built with Spring Boot 3.x, PostgreSQL, Redis, and Kafka.

## Tech Stack

| Layer | Technology |
|---|---|
| Runtime | Java 21 |
| Framework | Spring Boot 3.4 |
| Database | PostgreSQL 15+ |
| Migrations | Flyway |
| Cache | Redis 7+ |
| Messaging | Apache Kafka |
| Auth | Stateless JWT (HS256) |
| Build | Maven 3.9+ |

## Prerequisites

- JDK 21
- PostgreSQL running on `localhost:5432`, database `expensetracker`
- Redis running on `localhost:6379`
- Kafka running on `localhost:9092`

## Getting Started

```bash
# 1. Clone and enter the project
git clone <repo-url>
cd Gasto

# 2. Set required environment variables (or edit application.yml)
export DB_USERNAME=postgres
export DB_PASSWORD=yourpassword
export JWT_SECRET=404E635266556A586E3272357538782F413F4428472B4B6250645367566B5970

# 3. Run
mvn spring-boot:run
```

Flyway will auto-run migrations on startup. The server listens on **port 8080**.

## Build & Test

```bash
mvn clean verify        # compile + all tests
mvn test                # tests only
mvn spring-boot:run     # run locally
```

## API Endpoints

All responses use the shape `{ "success": bool, "data": T, "message": string, "timestamp": string }`.

### Authentication ? public
| Method | Path | Description |
|---|---|---|
| `POST` | `/api/v1/auth/register` | Create account, returns JWT |
| `POST` | `/api/v1/auth/login` | Sign in, returns JWT |

### Users ? requires `Authorization: Bearer <token>`
| Method | Path | Description |
|---|---|---|
| `GET` | `/api/v1/users/me` | Get current user profile |
| `PUT` | `/api/v1/users/me` | Update full name |

### Categories ? requires auth
| Method | Path | Description |
|---|---|---|
| `GET` | `/api/v1/categories` | List all predefined categories |

### Expenses ? requires auth
| Method | Path | Description |
|---|---|---|
| `GET` | `/api/v1/expenses` | List expenses (`?from=&to=` date filter) |
| `POST` | `/api/v1/expenses` | Create expense |
| `GET` | `/api/v1/expenses/{id}` | Get single expense |
| `PUT` | `/api/v1/expenses/{id}` | Update expense |
| `DELETE` | `/api/v1/expenses/{id}` | Soft-delete expense |
| `POST` | `/api/v1/expenses/sync` | Bulk upsert (offline sync) |
| `GET` | `/api/v1/expenses/summary` | Monthly totals by category (`?year=&month=`) |

### Quick Example

```bash
# Register
curl -X POST http://localhost:8080/api/v1/auth/register \
  -H "Content-Type: application/json" \
  -d '{"fullName":"Jane Doe","email":"jane@example.com","password":"secret123"}'

# Login and save token
TOKEN=$(curl -s -X POST http://localhost:8080/api/v1/auth/login \
  -H "Content-Type: application/json" \
  -d '{"email":"jane@example.com","password":"secret123"}' | jq -r '.data.accessToken')

# Create expense
curl -X POST http://localhost:8080/api/v1/expenses \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d '{"categoryId":"<category-id>","amount":12.50,"description":"Coffee","expenseDate":"2026-04-14"}'
```

## Architecture

The project follows **Domain-Driven Design** with strict layering:

```
com.gasto/
������ domain/          �� Pure Java: entities, repository interfaces, domain exceptions
������ application/     �� Spring services + DTOs; depends only on domain interfaces
������ infrastructure/  �� JPA, Redis, Kafka, Security implementations
������ presentation/    �� REST controllers + GlobalExceptionHandler
```

Key rules:
- `domain/` has **zero** Spring annotations
- Controllers never hold business logic ? everything delegates to a service
- All writes evict the Redis cache and publish a Kafka event (`expense-events` topic)
- Expenses use **soft delete** (`deleted_at`) to support mobile sync reconciliation

## Database Migrations

Migration files are in `src/main/resources/db/migration/`:

| File | Description |
|---|---|
| `V1__init_schema.sql` | Creates `users`, `categories`, `expenses` tables |
| `V2__seed_categories.sql` | Inserts 10 predefined categories |

To add a new migration, create `V{n+1}__description.sql` ? never edit applied files.

## Configuration

Key settings in `src/main/resources/application.yml` (override via environment variables):

| Variable | Purpose | Default |
|---|---|---|
| `DB_USERNAME` | PostgreSQL username | `postgres` |
| `DB_PASSWORD` | PostgreSQL password | `postgres` |
| `REDIS_HOST` | Redis host | `localhost` |
| `REDIS_PORT` | Redis port | `6379` |
| `KAFKA_BOOTSTRAP_SERVERS` | Kafka brokers | `localhost:9092` |
| `JWT_SECRET` | HS256 signing key (hex-encoded, min 256 bits) | see yml |
| `JWT_EXPIRATION_MS` | Token lifetime in ms | `86400000` (24 h) |

## Copilot Customizations

The `.github/` directory contains AI-assisted development tooling:

| Path | Purpose |
|---|---|
| `.github/copilot-instructions.md` | Always-on workspace rules loaded by Copilot |
| `.github/agents/backend-dev.agent.md` | Agent for implementing new features end-to-end |
| `.github/agents/test-writer.agent.md` | Agent for writing/fixing tests |
| `.github/agents/db-migration.agent.md` | Agent for creating Flyway migrations |
| `.github/prompts/new-feature.prompt.md` | `/new-feature` ? scaffold a feature across all layers |
| `.github/prompts/write-tests.prompt.md` | `/write-tests` ? generate tests for a class or method |
| `.github/prompts/add-migration.prompt.md` | `/add-migration` ? create a numbered migration file |
