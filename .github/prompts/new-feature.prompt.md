---
description: "Scaffold a complete new feature (endpoint + service + domain changes) in the Expense Tracker API"
argument-hint: "Describe the feature, e.g. 'Add a recurring expense flag to the Expense entity with a GET /expenses/recurring endpoint'"
agent: "agent"
---
You are implementing a new feature in the Gasto Expense Tracker API. Follow the project's strict DDD layering rules throughout.

## Feature to Implement
$input

## Required Steps ? Complete All in Order

### 1. Explore First
- Read the relevant domain entities and repository interfaces
- Read the related service and controller
- Read an existing migration to confirm the next version number

### 2. Domain Layer (`domain/`)
- Add or modify the entity (no Spring annotations)
- Add or extend the repository interface if new queries are needed

### 3. Database Migration (if schema changes)
- Create `src/main/resources/db/migration/V{n}__description.sql`
- Use TIMESTAMPTZ, UUID, NUMERIC(15,2) per project conventions

### 4. Application Layer (`application/`)
- Add request/response DTOs (Java records with Bean Validation)
- Add the service method (depends only on domain interfaces)
- Evict cache and publish Kafka event on any write

### 5. Infrastructure Layer (`infrastructure/`)
- Add JPQL query to `Jpa*Repository` if needed
- Extend the `*RepositoryAdapter` to implement new domain interface methods

### 6. Presentation Layer (`presentation/`)
- Add the endpoint to the controller
- Return `ApiResponse<T>` ? never raw entities
- Extract current user with `UUID.fromString(principal.getUsername())`

### 7. Tests
- Unit test for the service method (`@ExtendWith(MockitoExtension.class)`)
- `@WebMvcTest` slice test for the new endpoint (including 400 and 404 cases)

## Constraints
- Zero Spring annotations in `domain/`
- Services never call JPA repositories directly ? always go through the adapter
- Never return JPA entities from controllers
