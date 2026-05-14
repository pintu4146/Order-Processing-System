# Order Processing System

A production-style Spring Boot REST API for managing orders. Supports full order lifecycle — creation, retrieval, filtering, cancellation, and automated status transitions via a background scheduler.

Built as a clean, interview-ready backend project demonstrating Java 17, Spring Boot 3.2, layered architecture, and modern development practices.

---

## Tech Stack

| Layer | Technology |
|---|---|
| Language | Java 17 |
| Framework | Spring Boot 3.2.5 |
| Database | H2 (in-memory) |
| ORM | Hibernate 6.4 / Spring Data JPA |
| Migrations | Flyway |
| Validation | Jakarta Bean Validation |
| API Docs | SpringDoc OpenAPI (Swagger UI) |
| Build | Maven 3.9+ |
| Testing | JUnit 5, Mockito, MockMvc |
| Code Gen | Lombok |

---

## Quick Start

### Prerequisites
- Java 17+
- Maven 3.9+

### Run Locally

```cmd
# Clone
git clone https://github.com/pintu4146/Order-Processing-System.git
cd Order-Processing-System

# Copy environment config
copy .env.example .env

# Start with dev profile (default)
run.cmd

# Or specify a profile
run.cmd dev
run.cmd stage
run.cmd prod
```

### Other Commands

```cmd
run.cmd test         # Run all tests
run.cmd build        # Compile (skip tests)
run.cmd package      # Package as JAR
run.cmd clean        # Clean build artifacts
run.cmd stop         # Kill process on port 8080
```

### Access Points

| Service | URL |
|---|---|
| Swagger UI | http://localhost:8080/swagger-ui.html |
| API Docs (JSON) | http://localhost:8080/api-docs |
| H2 Console (dev only) | http://localhost:8080/h2-console |

---

## API Endpoints

| Method | Endpoint | Description | Status Code |
|---|---|---|---|
| `POST` | `/api/orders` | Create a new order | 201 Created |
| `GET` | `/api/orders/{id}` | Get order by ID | 200 OK / 404 |
| `GET` | `/api/orders?status=PENDING` | List orders (optional filter) | 200 OK |
| `PUT` | `/api/orders/{id}/cancel` | Cancel a PENDING order | 200 OK / 409 |

### Example: Create an Order

```bash
curl -X POST http://localhost:8080/api/orders \
  -H "Content-Type: application/json" \
  -d '{
    "customerName": "John Doe",
    "items": [
      { "productName": "Laptop", "quantity": 1, "price": 1200.00 },
      { "productName": "Mouse", "quantity": 2, "price": 25.99 }
    ]
  }'
```

---

## Project Structure

```
src/main/java/com/orderprocessing/
├── OrderProcessingApplication.java    # Entry point + @EnableScheduling
├── config/
│   └── SwaggerConfig.java             # OpenAPI metadata
├── controller/
│   └── OrderController.java           # REST endpoints (thin controller)
├── dto/
│   ├── CreateOrderRequest.java        # Input DTO with validation
│   ├── OrderItemRequest.java          # Input DTO for items
│   ├── OrderResponse.java             # Output DTO
│   ├── OrderItemResponse.java         # Output DTO for items
│   └── ErrorResponse.java             # Standardized error format
├── exception/
│   ├── GlobalExceptionHandler.java    # @ControllerAdvice
│   ├── OrderNotFoundException.java    # → 404
│   └── InvalidOrderStateException.java # → 409
├── model/
│   ├── Order.java                     # JPA entity
│   ├── OrderItem.java                 # JPA entity
│   └── OrderStatus.java              # Enum: PENDING → PROCESSING → SHIPPED → DELIVERED / CANCELLED
├── repository/
│   └── OrderRepository.java          # Spring Data JPA
├── scheduler/
│   └── OrderStatusScheduler.java     # Background job: PENDING → PROCESSING
└── service/
    ├── OrderService.java             # Interface
    └── OrderServiceImpl.java         # Business logic

src/test/java/com/orderprocessing/
├── controller/
│   └── OrderControllerTest.java      # MockMvc integration tests (9 tests)
├── scheduler/
│   └── OrderStatusSchedulerTest.java # Unit tests (2 tests)
├── service/
│   └── OrderServiceImplTest.java     # Unit tests with Mockito (13 tests)
└── test/
    └── LombokTest.java               # Build diagnostic test
```

---

## Testing

```cmd
run.cmd test
```

**24 tests total — all passing.**

| Test Class | Count | Type |
|---|---|---|
| `OrderServiceImplTest` | 13 | Unit (Mockito) |
| `OrderControllerTest` | 9 | Integration (MockMvc) |
| `OrderStatusSchedulerTest` | 2 | Unit (Mockito) |

See [docs/testing.md](docs/testing.md) for the full test strategy and edge cases covered.

---

## Architecture Decisions

- **Entities never leak** — all public methods return DTOs, never JPA entities.
- **Thin controllers** — zero business logic in the controller layer.
- **Interface-driven service** — `OrderService` interface enables easy mocking and future swapping.
- **Externalized config** — scheduler interval, database credentials all configurable via environment variables.
- **Flyway migrations** — schema managed via versioned SQL, not `ddl-auto: create`.

See [docs/decisions.md](docs/decisions.md) for the full rationale.

---

## Documentation

| Doc | Description |
|---|---|
| [architecture.md](docs/architecture.md) | High-level design, component interactions |
| [api.md](docs/api.md) | Full endpoint reference with examples |
| [testing.md](docs/testing.md) | Test strategy and coverage |
| [decisions.md](docs/decisions.md) | Design tradeoffs and rationale |
| [deployment.md](docs/deployment.md) | Docker and deployment guide |
| [assumptions.md](docs/assumptions.md) | All project assumptions |
| [LOMBOK_TROUBLESHOOTING.md](docs/LOMBOK_TROUBLESHOOTING.md) | Lombok + Maven 3.9 fix |
| [TESTING_TROUBLESHOOTING.md](docs/TESTING_TROUBLESHOOTING.md) | MockBean, .env, scope issues |

---

## Git Workflow

```
main (production) ← stage (staging) ← develop ← feature/*
```

All feature branches merge into `develop` via `--no-ff`. Merges to `stage` and `main` happen after review.