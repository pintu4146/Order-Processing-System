# Design Decisions

## 1. Layered Monolith vs Microservices

**Decision**: Layered monolith (Controller → Service → Repository).

**Why**: For an order processing system of this scope, microservices would add network complexity, deployment overhead, and distributed transaction challenges without meaningful benefit. The layered monolith provides clean separation of concerns that can be extracted into microservices later if needed.

## 2. H2 In-Memory Database

**Decision**: Use H2 for all environments.

**Why**: This is an assignment/interview project. H2 provides zero-setup persistence that works identically across machines. In a real production system, this would be PostgreSQL or MySQL, and the switch would require only a YAML config change and a driver dependency — the code itself wouldn't change thanks to JPA abstraction.

## 3. Flyway Over Hibernate `ddl-auto: create`

**Decision**: Use Flyway for schema management with `ddl-auto: validate`.

**Why**: `ddl-auto: create` silently drops and recreates tables — dangerous in any environment beyond local dev. Flyway provides:
- **Versioned, auditable migrations** (`V1__init_schema.sql`)
- **Reproducible schema** across environments
- **Validation** that the entity model matches the actual DB schema

## 4. DTOs — Never Expose Entities

**Decision**: All public API methods accept/return DTOs, never JPA entities.

**Why**:
- **Security**: Entities may have fields (internal IDs, audit columns) that shouldn't be exposed
- **Decoupling**: API contract is independent of database schema changes
- **Validation**: DTOs carry `@NotBlank`, `@Positive` annotations — entities carry JPA annotations. Mixing them creates confusion.

## 5. Interface-Driven Service Layer

**Decision**: `OrderService` interface + `OrderServiceImpl` implementation.

**Why**:
- **Testability**: Controller tests use `@MockBean` to mock the interface
- **Open/Closed Principle**: New implementations can be swapped without changing the controller
- **Documentation**: The interface serves as a contract with Javadoc describing behavior

## 6. Lombok — Keep It (With Explicit Processor Config)

**Decision**: Keep Lombok with explicit `annotationProcessorPaths` in `maven-compiler-plugin`.

**Why**: Lombok reduces ~60% of boilerplate in DTOs and entities. The alternative (Java Records for DTOs) was considered but deferred because:
- Records can't have `@Builder` (Lombok's biggest value-add)
- JPA entities require mutability — Records are immutable
- The team is already familiar with Lombok
- The annotation processor issue was resolved and documented

**Tradeoff acknowledged**: Lombok adds build complexity (see `docs/LOMBOK_TROUBLESHOOTING.md`). For a greenfield project on Java 21+, Records would be preferred for DTOs.

## 7. `PUT /cancel` vs `PATCH /cancel` vs `POST /cancel`

**Decision**: `PUT /api/orders/{id}/cancel`.

**Why**: This is a state transition, not a partial update. `PUT` communicates idempotency (cancelling an already-cancelled order is a no-op in terms of side effects, though we throw an exception for clarity). `PATCH` would imply partial field updates. `POST` would imply non-idempotency. For an interview project, `PUT` is the most defensible choice.

## 8. Environment Variable Credentials

**Decision**: Use `${DB_USERNAME:sa}` syntax with defaults only in dev.

**Why**:
- **Dev**: Defaults allow zero-config startup (`run.cmd` just works)
- **Stage/Prod**: No defaults — forces explicit environment variable injection, preventing accidental use of dev credentials
- **`.env` file**: Loaded by `run.cmd` for local convenience, git-ignored to prevent credential leaks

## 9. Scheduler — `fixedRate` with Externalized Interval

**Decision**: `@Scheduled(fixedRateString = "${app.scheduler.interval}")`.

**Why**:
- **Externalized**: Interval can be tuned per environment (fast in dev for testing, slow in prod)
- **`fixedRate` over `fixedDelay`**: For this workload (lightweight DB query), overlapping runs are unlikely. `fixedRate` is simpler to reason about.
- **`initialDelay = 10s`**: Prevents the scheduler from firing before the Spring context is fully initialized

## 10. `@ControllerAdvice` for Global Error Handling

**Decision**: Single `GlobalExceptionHandler` with typed exception handlers.

**Why**: Centralizes all error-to-HTTP-status mapping in one place. Without it, every controller method would need try/catch blocks. The standardized `ErrorResponse` DTO ensures clients always receive a predictable error format.

## What Was NOT Built (Intentional Scope Boundaries)

| Feature | Why Not |
|---|---|
| Authentication/Authorization | Out of scope for this assignment |
| Pagination on list endpoint | Would add complexity; H2 dataset is small |
| SHIPPED/DELIVERED transitions | Enum values defined for completeness; transitions not in requirements |
| Event sourcing / audit log | Overkill for this scope; `createdAt`/`updatedAt` provide basic auditing |
| Caching | H2 is in-memory already; caching an in-memory DB has zero benefit |
