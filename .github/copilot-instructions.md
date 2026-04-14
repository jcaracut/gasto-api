# Gasto ? Expense Tracker API: Copilot Instructions

## Project Overview

Spring Boot 3.x RESTful API for an offline-first mobile expense tracker.
- **Language**: Java 21
- **Build**: Maven (`pom.xml` at root)
- **Database**: PostgreSQL + Flyway migrations (`src/main/resources/db/migration/`)
- **Cache**: Redis (manual via `ExpenseCacheService`, keys pattern `expenses::{userId}::*`)
- **Events**: Kafka (fire-and-forget via `ExpenseEventProducer`, topic `expense-events`)
- **Auth**: Stateless JWT (HS256 via `JwtUtil`)

## Architecture: DDD Layering

```
domain/          �� Pure Java. No Spring. Entities, repository interfaces, domain exceptions.
application/     �� Business logic. Services, DTOs. Depends only on domain interfaces.
infrastructure/  �� Spring beans. JPA adapters, security, Kafka, Redis implementations.
presentation/    �� REST controllers, GlobalExceptionHandler, ApiResponse<T> wrapper.
```

**Rules (enforce when editing):**
- `domain/` classes must have ZERO Spring annotations (`@Service`, `@Repository`, etc.)
- `application/` services depend on domain interfaces ? never on JPA repositories directly
- `infrastructure/persistence/adapter/` contains the only implementations of domain repository interfaces
- Controllers must not contain business logic ? delegate everything to a service

## Key Conventions

### Repository Pattern
Domain repository interfaces live in `domain/{aggregate}/`. JPA Spring Data interfaces live in `infrastructure/persistence/jpa/Jpa*Repository.java`. Adapter classes in `infrastructure/persistence/adapter/*RepositoryAdapter.java` bridge them.

### API Response Shape
All controllers return `ApiResponse<T>` from `presentation/common/ApiResponse.java`:
```json
{ "success": true, "data": {}, "message": "...", "timestamp": "..." }
```
Never return raw domain objects or JPA entities from controllers ? always convert to a DTO/response record.

### Expense Soft Delete
`Expense` uses soft delete via `deletedAt` (TIMESTAMPTZ). Always filter `e.isDeleted()` when reading.
The `syncExpenses` endpoint performs an upsert using the client-provided UUID for idempotency.

### Validation
Use Bean Validation annotations on request records (`@NotNull`, `@Size`, `@DecimalMin`, `@PastOrPresent`).
Validation errors are handled centrally in `GlobalExceptionHandler` �� returns HTTP 400 with field-level map.

### Cache Eviction
After any write (create, update, delete, sync) on expenses, call `cacheService.evictUserCache(userId)`.
Cache keys use pattern: `expenses::list::{userId}::{from}::{to}` and `expenses::summary::{userId}::{year}-{month}`.

### Kafka
`ExpenseEventProducer` methods are `@Async` fire-and-forget. Never `await` or block on Kafka in a service.
`ExpenseEvent` record fields: `eventType`, `expenseId`, `userId`, `categoryId`, `categoryName`, `amount`, `expenseDate`, `occurredAt`.

### Security
JWT subject is the user UUID string. `UserDetailsServiceImpl.loadUserByUsername()` accepts UUID string.
In controllers, extract the current user ID with: `UUID.fromString(principal.getUsername())`.
Public endpoints: only `/api/v1/auth/**`. All others require a valid JWT Bearer token.

## Package Root
`com.gasto`

## Build & Test Commands
```bash
mvn clean verify          # build + all tests
mvn test                  # tests only
mvn spring-boot:run       # run locally (requires PostgreSQL, Redis, Kafka)
```

## Flyway Migrations
- Files: `src/main/resources/db/migration/V{n}__{description}.sql`
- Naming: `V3__add_column_x.sql` ? version must be strictly greater than previous
- Never edit an already-applied migration ? always add a new version

## Test Conventions
- Unit tests: `@ExtendWith(MockitoExtension.class)` ? mock all dependencies
- Web slice tests: `@WebMvcTest` + `@Import(SecurityConfig.class, JwtAuthFilter.class)` + `@MockitoBean` for `JwtUtil` and `UserDetailsServiceImpl`
- No Spring context for service/domain tests
- Use `@WithMockUser(username = "{uuid}")` where the username is the user UUID string
