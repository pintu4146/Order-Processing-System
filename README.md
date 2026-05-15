<p align="center">
  <h1 align="center">🛒 Order Processing System</h1>
  <p align="center">
    A production-grade, extensible backend for e-commerce order lifecycle management.
    <br />
    Built with Java 17 · Spring Boot 3.2 · SOLID Architecture
    <br />
    <br />
    <a href="https://github.com/pintu4146/Order-Processing-System/issues">Report Bug</a>
    ·
    <a href="https://github.com/pintu4146/Order-Processing-System/issues">Request Feature</a>
  </p>
</p>

---

## 📋 Table of Contents

- [About The Project](#about-the-project)
- [Current Implementation](#current-implementation)
- [Tech Stack](#tech-stack)
- [Getting Started](#getting-started)
- [API Reference](#api-reference)
- [Project Structure](#project-structure)
- [Testing](#testing)
- [Architecture Decisions](#architecture-decisions)
- [Future Scope — Microservices Scalability](#future-scope--microservices-scalability)
- [Contributing](#contributing)
- [License](#license)

---

## About The Project

A clean, enterprise-style Spring Boot REST API that manages the full order lifecycle — creation, retrieval, filtering, cancellation, and automated status transitions via a background scheduler.

This project demonstrates **professional backend engineering** with strict adherence to **SOLID principles**, **DRY/KISS** practices, and **modern Java 17+** conventions.

### Key Highlights

- 🏗️ **Layered Architecture** — Controller → Service → Repository with strict separation of concerns
- 🔒 **Immutable Entities** — JPA entities with controlled mutability and proper `equals`/`hashCode`
- 🔄 **State Machine Enum** — `OrderStatus` encapsulates transition rules (Open/Closed Principle)
- 🧹 **Dedicated Mapper** — `OrderMapper` component for DTO conversions (Single Responsibility)
- ⏰ **Background Scheduler** — Automatic `PENDING → PROCESSING` transitions
- 🩺 **Health Monitoring** — Spring Boot Actuator for production readiness checks
- 📄 **Flyway Migrations** — Versioned, repeatable database schema management
- 🐳 **Docker Ready** — Multi-stage Dockerfile for optimized container images

---

## Current Implementation

### Core Features

| Feature | Description | Status |
|---|---|---|
| Create Order | Place an order with multiple items, auto-calculated `totalAmount` using `BigDecimal` | ✅ Complete |
| Retrieve Order | Fetch order details by ID with full item breakdown | ✅ Complete |
| List Orders | Paginated listing with optional `status` filter | ✅ Complete |
| Cancel Order | Cancel only `PENDING` orders; all other states return `409 Conflict` | ✅ Complete |
| Auto-Process | Background scheduler transitions `PENDING → PROCESSING` at configurable intervals | ✅ Complete |
| Health Endpoint | `/actuator/health` for load balancer and monitoring integration | ✅ Complete |

### Order State Machine

```
PENDING ──────► PROCESSING ──────► SHIPPED ──────► DELIVERED
   │
   └──────────► CANCELLED
```

Transition rules are **encapsulated within the `OrderStatus` enum** itself, not scattered across service methods. Adding a new status (e.g., `REFUNDED`) requires only modifying the enum — zero changes to business services.

### Design Principles Applied

| Principle | How It's Applied |
|---|---|
| **SRP** | `OrderMapper` handles all DTO ↔ Entity conversions; services contain only business logic |
| **OCP** | `OrderStatus.canTransitionTo()` encapsulates state rules; new states don't require service changes |
| **DRY** | `GlobalExceptionHandler` uses a unified `buildErrorResponse()` helper — zero duplicated builder code |
| **KISS** | Modern Java 17 `.toList()` replaces verbose `Collectors.toList()`; `@UpdateTimestamp` replaces manual timestamp management |

---

## Tech Stack

| Layer | Technology |
|---|---|
| Language | Java 17 |
| Framework | Spring Boot 3.2.5 |
| Database | H2 (in-memory, swappable) |
| ORM | Hibernate 6.4 / Spring Data JPA |
| Migrations | Flyway |
| Validation | Jakarta Bean Validation |
| API Docs | SpringDoc OpenAPI (Swagger UI) |
| Monitoring | Spring Boot Actuator |
| Build | Maven 3.9+ |
| Testing | JUnit 5, Mockito, MockMvc |
| Code Gen | Lombok |
| Containerization | Docker (multi-stage build) |

---

## Getting Started

### Prerequisites

- Java 17+
- Maven 3.9+
- Docker (optional, for containerized deployment)

### Installation

```bash
# 1. Clone the repository
git clone https://github.com/pintu4146/Order-Processing-System.git
cd Order-Processing-System

# 2. Copy environment config
copy .env.example .env

# 3. Start with dev profile (default)
run.cmd

# Or specify a profile
run.cmd dev
run.cmd stage
run.cmd prod
```

### Useful Commands

```bash
run.cmd test         # Run all 24 tests
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
| Health Check | http://localhost:8080/actuator/health |
| H2 Console (dev) | http://localhost:8080/h2-console |

---

## API Reference

| Method | Endpoint | Description | Status Code |
|---|---|---|---|
| `POST` | `/api/orders` | Create a new order | `201 Created` |
| `GET` | `/api/orders/{id}` | Get order by ID | `200 OK` / `404` |
| `GET` | `/api/orders?status=PENDING&page=0&size=10` | List orders (paginated, filterable) | `200 OK` |
| `PUT` | `/api/orders/{id}/cancel` | Cancel a PENDING order | `200 OK` / `409` |
| `GET` | `/actuator/health` | Application health check | `200 OK` |

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

For the full API reference with request/response schemas, see [docs/api.md](docs/api.md).

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
│   ├── GlobalExceptionHandler.java    # @ControllerAdvice (DRY helper)
│   ├── OrderNotFoundException.java    # → 404
│   └── InvalidOrderStateException.java # → 409
├── mapper/
│   └── OrderMapper.java               # DTO ↔ Entity conversion (SRP)
├── model/
│   ├── Order.java                     # JPA entity (immutable ID, safe equals/hashCode)
│   ├── OrderItem.java                 # JPA entity
│   └── OrderStatus.java              # State-machine enum (OCP)
├── repository/
│   └── OrderRepository.java          # Spring Data JPA
├── scheduler/
│   └── OrderStatusScheduler.java     # Background job: PENDING → PROCESSING
└── service/
    ├── OrderService.java             # Interface
    └── OrderServiceImpl.java         # Business logic

qa/                                    # QA Automation Suite
├── scenarios/                         # Test scenario documentation (Given/When/Then)
├── scripts/                           # Automated SIT scripts (PowerShell + Bash)
└── reports/                           # Execution logs and pass/fail reports
```

---

## Testing

```bash
run.cmd test
```

**24 tests total — all passing.**

| Test Class | Count | Type |
|---|---|---|
| `OrderServiceImplTest` | 13 | Unit (Mockito) |
| `OrderControllerTest` | 9 | Integration (MockMvc) |
| `OrderStatusSchedulerTest` | 2 | Unit (Mockito) |

### QA Automation

In addition to unit/integration tests, the project includes automated **System Integration Testing (SIT)** scripts in the `qa/` directory that validate the live running application end-to-end:

- `qa/scripts/sit_api_tests.ps1` — Tests health, CRUD, pagination, and cancellation
- `qa/scripts/sit_scheduler_tests.ps1` — Tests automated PENDING → PROCESSING transition

See [docs/testing.md](docs/testing.md) for the full test strategy and edge cases covered.

---

## Architecture Decisions

- **Entities never leak** — all public methods return DTOs, never JPA entities.
- **Thin controllers** — zero business logic in the controller layer.
- **Interface-driven service** — `OrderService` interface enables easy mocking and future swapping.
- **Externalized config** — scheduler interval, database credentials all configurable via environment variables.
- **Flyway migrations** — schema managed via versioned SQL, not `ddl-auto: create`.
- **JPA-safe identity** — `equals()`/`hashCode()` based on DB identity, safe for `Set` collections and detached entities.

See [docs/decisions.md](docs/decisions.md) for the full rationale.

---

## Future Scope — Microservices Scalability

This monolith is intentionally designed to be **decomposition-ready**. Below is the roadmap for scaling into a distributed microservices architecture.

### Phase 1: Service Decomposition

| Microservice | Responsibility | Current Code |
|---|---|---|
| **Order Service** | Order CRUD, state management | `controller/`, `service/`, `model/` |
| **Inventory Service** | Stock validation, product catalog | New service (currently implicit) |
| **Notification Service** | Email/SMS on status change | New service |
| **Payment Service** | Payment processing, refunds | New service |

### Phase 2: Infrastructure

| Component | Technology | Purpose |
|---|---|---|
| **API Gateway** | Spring Cloud Gateway / Kong | Single entry point, rate limiting, auth |
| **Service Discovery** | Eureka / Consul | Dynamic service registration |
| **Config Server** | Spring Cloud Config | Centralized configuration management |
| **Circuit Breaker** | Resilience4j | Fault tolerance between services |

### Phase 3: Event-Driven Architecture

```
┌──────────┐    Event Bus     ┌──────────────────┐
│  Order   │ ──────────────►  │  Notification    │
│ Service  │  (Kafka/RabbitMQ)│  Service         │
└──────────┘                  └──────────────────┘
     │                              │
     ▼                              ▼
┌──────────┐                  ┌──────────────────┐
│ Inventory│                  │  Payment         │
│ Service  │                  │  Service         │
└──────────┘                  └──────────────────┘
```

- Replace synchronous REST calls with **Apache Kafka** or **RabbitMQ** for event-driven communication
- Implement the **Saga Pattern** for distributed transactions (e.g., order → payment → inventory)
- Use **CQRS** (Command Query Responsibility Segregation) to separate read/write models for high throughput

### Phase 4: Observability & Deployment

| Concern | Technology |
|---|---|
| Distributed Tracing | Zipkin / Jaeger |
| Centralized Logging | ELK Stack (Elasticsearch, Logstash, Kibana) |
| Metrics & Monitoring | Prometheus + Grafana |
| Container Orchestration | Kubernetes (K8s) |
| CI/CD Pipeline | GitHub Actions / Jenkins |
| Database per Service | PostgreSQL (Order), MongoDB (Catalog), Redis (Cache) |

### Phase 5: Advanced Patterns

- **Optimistic Locking** — Add `@Version` to entities for concurrent update safety
- **Idempotency Keys** — Prevent duplicate order creation on network retries
- **Rate Limiting** — Protect APIs from abuse using token bucket algorithms
- **Audit Trail** — Event sourcing for complete order history reconstruction
- **Multi-Tenancy** — Support multiple merchants on a single platform

---

## Contributing

We welcome contributions! Please follow the guidelines below to keep the codebase clean and consistent.

### Getting Started

1. **Fork** the repository
2. **Clone** your fork locally
3. Create a **feature branch** from `develop`:
   ```bash
   git checkout develop
   git checkout -b feature/your-feature-name
   ```
4. Make your changes
5. Run the test suite and ensure all 24 tests pass:
   ```bash
   run.cmd test
   ```
6. **Commit** with a conventional commit message:
   ```bash
   git commit -m "feat: add payment validation endpoint"
   ```
7. **Push** to your fork and open a **Pull Request** against `develop`

### Branch Naming Convention

| Branch | Purpose |
|---|---|
| `main` | Production-ready code only |
| `stage` | QA / Pre-production testing |
| `develop` | Active development (default target for PRs) |
| `feature/*` | New features |
| `fix/*` | Bug fixes |
| `refactor/*` | Code improvements without behavior changes |

### Commit Message Convention

Follow [Conventional Commits](https://www.conventionalcommits.org/):

```
feat:     New feature
fix:      Bug fix
refactor: Code change that neither fixes a bug nor adds a feature
test:     Adding or updating tests
docs:     Documentation only changes
chore:    Build process or auxiliary tool changes
```

### Code Style Rules

- **Java 17+** — Use modern language features (`record`, `.toList()`, `switch` expressions)
- **SOLID Principles** — All new code must follow Single Responsibility, Open/Closed, etc.
- **No wildcard imports** — Always use explicit imports
- **DTO boundary** — Never expose JPA entities in controller responses
- **Test coverage** — Every new feature must include corresponding unit tests
- **Lombok usage** — Use `@Builder`, `@Getter` but avoid class-level `@Setter` on entities
- **BigDecimal** — All monetary values must use `BigDecimal`, never `double` or `float`

### Pull Request Checklist

- [ ] Code compiles without warnings
- [ ] All 24+ existing tests pass (`run.cmd test`)
- [ ] New tests added for new functionality
- [ ] No wildcard imports
- [ ] Conventional commit messages used
- [ ] Documentation updated if applicable

### Code of Conduct

Be respectful, constructive, and inclusive. We follow the [Contributor Covenant](https://www.contributor-covenant.org/) code of conduct.

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

---

## Git Workflow

```
main (production) ← stage (QA testing) ← develop ← feature/*
```

All feature branches merge into `develop` via `--no-ff`. Code moves to `stage` for QA verification (automated SIT + manual review), then to `main` for production release.

---

## License

Distributed under the MIT License. See `LICENSE` for more information.

---

<p align="center">
  Built with ❤️ by <a href="https://github.com/pintu4146">Pintu Kumar</a>
</p>