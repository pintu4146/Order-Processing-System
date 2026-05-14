# Testing Strategy

## Overview

The project uses a **two-tier testing strategy**:

1. **Unit Tests** (Mockito) — isolate business logic from infrastructure
2. **Integration Tests** (MockMvc) — validate the HTTP layer end-to-end (controller → exception handler → serialization)

**Total: 24 tests — all passing.**

```cmd
run.cmd test
```

---

## Test Suite Breakdown

### OrderServiceImplTest — 13 Unit Tests

Tests the service layer in isolation using `@ExtendWith(MockitoExtension.class)` with a mocked `OrderRepository`.

| Test | Category | What It Validates |
|---|---|---|
| Create order with valid items → PENDING | Happy path | Order creation, status initialization, repository save |
| Get order by existing ID | Happy path | Fetch and DTO mapping |
| Get order by non-existent ID → exception | Edge case | `OrderNotFoundException` thrown |
| List all orders (no filter) | Happy path | `findAll()` called, not `findByStatus()` |
| List orders filtered by status | Happy path | `findByStatus()` called, not `findAll()` |
| Cancel PENDING order → CANCELLED | Happy path | Status change, repository save |
| Cancel PROCESSING order → exception | State guard | `InvalidOrderStateException` thrown, no save |
| Cancel SHIPPED order → exception | State guard | `InvalidOrderStateException` thrown, no save |
| Cancel DELIVERED order → exception | State guard | `InvalidOrderStateException` thrown, no save |
| Cancel CANCELLED order → exception | State guard | `InvalidOrderStateException` thrown, no save |
| Cancel non-existent order → exception | Edge case | `OrderNotFoundException` thrown |
| Process pending orders → PROCESSING | Scheduler | Bulk status transition, `saveAll()` called |
| Process with no pending → no-op | Idempotency | Returns 0, `saveAll()` never called |

### OrderControllerTest — 9 Integration Tests

Tests the full HTTP stack using `@WebMvcTest` with `MockMvc` and a `@MockBean` service.

| Test | HTTP | What It Validates |
|---|---|---|
| POST valid order → 201 | `POST /api/orders` | Request deserialization, 201 status, JSON response |
| POST empty items → 400 | `POST /api/orders` | Bean Validation fires, `GlobalExceptionHandler` returns error DTO |
| POST missing customer name → 400 | `POST /api/orders` | Bean Validation fires |
| GET existing order → 200 | `GET /api/orders/1` | Path variable binding, JSON response |
| GET non-existent order → 404 | `GET /api/orders/99` | `OrderNotFoundException` → 404 via exception handler |
| GET all orders → 200 | `GET /api/orders` | List serialization |
| GET filtered orders → 200 | `GET /api/orders?status=PENDING` | Query param binding to enum |
| PUT cancel PENDING → 200 | `PUT /api/orders/1/cancel` | Cancel flow, CANCELLED status in response |
| PUT cancel non-PENDING → 409 | `PUT /api/orders/1/cancel` | `InvalidOrderStateException` → 409 via exception handler |

### OrderStatusSchedulerTest — 2 Unit Tests

| Test | What It Validates |
|---|---|
| Scheduler calls service | `processPendingOrders()` is delegated to `OrderService` |
| Exception doesn't crash scheduler | `try/catch` in scheduler prevents RuntimeException from killing the thread |

### LombokTest — Diagnostic

Located in `src/test/java/com/orderprocessing/test/`. Not a functional test — it verifies that the Lombok annotation processor is working correctly in the build environment. Retained as a permanent diagnostic tool.

---

## Testing Patterns Used

| Pattern | Where | Why |
|---|---|---|
| `@ExtendWith(MockitoExtension.class)` | Service tests | Lightweight — no Spring context needed |
| `@WebMvcTest(OrderController.class)` | Controller tests | Loads only the web slice — fast startup |
| `@MockBean` | Controller tests | Replaces the real service bean with a Mockito mock |
| `@Nested` + `@DisplayName` | All tests | Organized by operation, readable test report |
| AssertJ (`assertThat`) | All tests | Fluent, readable assertions |
| `verify(..., never())` | Cancel tests | Proves that `save()` is NOT called on invalid states |

---

## Known Troubleshooting

See [TESTING_TROUBLESHOOTING.md](TESTING_TROUBLESHOOTING.md) for:
- `@MockBean` import location (differs per Spring Boot version)
- Lombok annotation processor configuration
