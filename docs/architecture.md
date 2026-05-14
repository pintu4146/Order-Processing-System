# Architecture

## High-Level Design

The Order Processing System follows a **layered monolith** architecture — the standard pattern for Spring Boot applications that are deployed as a single JAR.

```
┌──────────────────────────────────────────────────────┐
│                    Client (Browser/curl)              │
└──────────────────┬───────────────────────────────────┘
                   │ HTTP
┌──────────────────▼───────────────────────────────────┐
│              Controller Layer                         │
│  OrderController.java (@RestController)               │
│  GlobalExceptionHandler.java (@ControllerAdvice)      │
│  ─ Request validation (@Valid)                        │
│  ─ HTTP status code mapping                          │
│  ─ Swagger/OpenAPI annotations                       │
└──────────────────┬───────────────────────────────────┘
                   │ DTOs (never entities)
┌──────────────────▼───────────────────────────────────┐
│              Service Layer                            │
│  OrderService.java (interface)                        │
│  OrderServiceImpl.java (@Service, @Transactional)     │
│  ─ Business logic                                    │
│  ─ State transition rules                            │
│  ─ Entity ↔ DTO mapping                              │
└──────────────────┬───────────────────────────────────┘
                   │ Entities
┌──────────────────▼───────────────────────────────────┐
│              Repository Layer                         │
│  OrderRepository.java (JpaRepository)                 │
│  ─ CRUD operations                                   │
│  ─ Custom query: findByStatus()                      │
└──────────────────┬───────────────────────────────────┘
                   │ JDBC
┌──────────────────▼───────────────────────────────────┐
│              Database (H2 in-memory)                  │
│  Managed by Flyway migrations                         │
│  Tables: orders, order_items                          │
└──────────────────────────────────────────────────────┘

┌──────────────────────────────────────────────────────┐
│              Scheduler (Background)                   │
│  OrderStatusScheduler.java (@Scheduled)               │
│  ─ Runs every 5 minutes (configurable)               │
│  ─ PENDING → PROCESSING transition                   │
│  ─ Calls OrderService.processPendingOrders()         │
└──────────────────────────────────────────────────────┘
```

## Component Interactions

### Request Flow

1. Client sends HTTP request → **Controller** validates input via `@Valid`
2. Controller calls **Service** with DTO → Service applies business rules
3. Service calls **Repository** to persist/query → Repository talks to DB
4. Service maps entity back to response DTO → Controller wraps in `ResponseEntity`
5. **GlobalExceptionHandler** intercepts any exceptions → returns standardized `ErrorResponse`

### Scheduler Flow

1. Spring `@Scheduled` triggers `OrderStatusScheduler.processPendingOrders()` every 5 minutes
2. Scheduler calls `OrderService.processPendingOrders()`
3. Service queries all `PENDING` orders → sets status to `PROCESSING` → batch saves
4. If zero PENDING orders → no-op (idempotent)

## Order State Machine

```
    ┌─────────┐
    │ PENDING │──────────────────┐
    └────┬────┘                  │
         │ Scheduler             │ cancelOrder()
         ▼                       ▼
    ┌────────────┐         ┌───────────┐
    │ PROCESSING │         │ CANCELLED │
    └─────┬──────┘         └───────────┘
          │ (future)
          ▼
    ┌─────────┐
    │ SHIPPED │
    └────┬────┘
         │ (future)
         ▼
    ┌───────────┐
    │ DELIVERED │
    └───────────┘
```

**Rules:**
- Only `PENDING` orders can be cancelled
- The scheduler only transitions `PENDING → PROCESSING`
- `SHIPPED → DELIVERED` transitions are defined in the enum but not yet implemented (future scope)

## Why Layered Monolith?

For an assignment of this scope, a layered monolith provides:

1. **Simplicity** — single deployable JAR, no network calls between services
2. **Testability** — each layer is independently testable via mocking
3. **Separation of concerns** — controller doesn't know about JPA, service doesn't know about HTTP
4. **Migration path** — if this grows, individual layers can be extracted into microservices
