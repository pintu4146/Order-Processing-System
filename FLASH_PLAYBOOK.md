# Flash Playbook — Order Processing System

> **Your role**: Primary code generator. Follow this playbook precisely.
> **Opus role**: Architect & reviewer. Opus has already designed the architecture — do NOT deviate from it.

---

## 🔴 MANDATORY RULES — Read First

1. **Always use `cmd /c`** before any shell command. Example: `cmd /c "mvn clean compile"`
2. **Always read `.agents.md`** at the start of every conversation for project rules.
3. **Never expose JPA entities in API responses.** Always map to DTOs.
4. **Never put business logic in controllers.** Controllers only validate + delegate.
5. **Use profile-based YAML config**, not `application.properties`.
6. **Follow the folder structure exactly** as defined in the implementation plan.
7. **Do not add dependencies** beyond what's in the plan unless absolutely necessary.
8. **Do not refactor the architecture.** If you think the structure should change, STOP and ask for Opus review.
9. **Follow the git branching strategy** defined in `.agents.md`. Never commit to `main`, `stage`, or `develop` directly.
10. **Schema changes go through Flyway only.** Never use JPA `ddl-auto: create` or `update`. Always create a `V{N}__{description}.sql` migration.

---

## 🔀 GIT WORKFLOW — Execute This Every Phase

```bash
# === START OF PHASE ===
# 1. Make sure you're on develop and it's up to date
cmd /c "git checkout develop"

# 2. Create feature branch for this phase
cmd /c "git checkout -b feature/phase-N-description"

# 3. ... do all your work ...

# 4. Stage and commit
cmd /c "git add ."
cmd /c "git commit -m \"feat: phase N - description of what was done\""

# 5. Merge back to develop
cmd /c "git checkout develop"
cmd /c "git merge --no-ff feature/phase-N-description -m \"Merge feature/phase-N-description into develop\""

# 6. Clean up feature branch
cmd /c "git branch -d feature/phase-N-description"

# === END OF PHASE — Report to user ===
```

> ⚠️ **If you forget to branch or accidentally commit to develop/main, STOP and tell the user immediately.**

---

## ⚠️ CAUTION ZONES — Where You're Likely to Make Mistakes

### 1. State Transition Logic (Phase 3)
**Risk**: Allowing invalid state transitions (e.g., cancelling a SHIPPED order).

**What to do**:
```java
// In OrderServiceImpl.cancelOrder():
// ONLY allow cancel if status == PENDING
// Throw InvalidOrderStateException for ALL other statuses
// Including: PROCESSING, SHIPPED, DELIVERED, CANCELLED
```

**DO NOT** use a generic status update endpoint. The only transitions are:
- `PENDING → CANCELLED` (via cancel endpoint)
- `PENDING → PROCESSING` (via scheduler)
- `SHIPPED`, `DELIVERED` are future scope — no endpoints for them

### 2. Entity ↔ DTO Mapping (Phase 3)
**Risk**: Returning the `Order` entity directly from the controller.

**What to do**:
- Create private mapping methods in `OrderServiceImpl`
- `mapToResponse(Order order)` → `OrderResponse`
- `mapToItemResponse(OrderItem item)` → `OrderItemResponse`
- Service methods return `OrderResponse`, NEVER `Order`

### 3. Bean Validation (Phase 2)
**Risk**: Missing validation annotations or wrong error responses.

**Checklist**:
```java
// CreateOrderRequest:
//   customerName → @NotBlank
//   items → @NotEmpty + @Valid (to cascade into OrderItemRequest)

// OrderItemRequest:
//   productName → @NotBlank
//   quantity → @NotNull + @Positive
//   price → @NotNull + @Positive
```

### 4. GlobalExceptionHandler (Phase 2)
**Risk**: Returning raw exception stack traces to the client.

**What to do**:
- Always return `ErrorResponse` DTO
- Never expose internal details
- Handle these specifically:
  - `OrderNotFoundException` → 404
  - `InvalidOrderStateException` → 409 Conflict
  - `MethodArgumentNotValidException` → 400 (extract field errors)
  - `Exception` → 500 (generic fallback, log the error)

### 5. YAML Config (Phase 1)
**Risk**: Wrong profile activation or missing properties.

**Structure**:
```
application.yml          → common settings (server port, app name)
application-dev.yml      → H2 config, console enabled, SQL logging
application-stage.yml    → H2 config, console disabled, less logging
application-prod.yml     → H2 config, console disabled, minimal logging
```

**DO NOT** put everything in one file. Split by profile.

### 6. Scheduler (Phase 5)
**Risk**: Not being idempotent, or processing orders that shouldn't be processed.

**What to do**:
```java
// OrderStatusScheduler should:
// 1. Call orderService.processPendingOrders()
// 2. Log how many orders were transitioned
// 3. Do NOTHING else — all logic is in the service

// OrderServiceImpl.processPendingOrders() should:
// 1. Find all orders with status == PENDING
// 2. Set status = PROCESSING and updatedAt = now
// 3. Save all
// 4. Return count of updated orders
// 5. If no PENDING orders found → return 0, do nothing (idempotent)
```

### 7. Test Quality (Phase 6)
**Risk**: Writing shallow tests that only test happy paths.

**Must-test negative scenarios**:
- Empty item list → rejected
- Null customer name → rejected
- Non-existent order ID → 404
- Cancel PROCESSING order → 409
- Cancel SHIPPED order → 409
- Cancel DELIVERED order → 409
- Cancel already CANCELLED order → 409

---

## 🟢 SAFE ZONES — Where You Can Move Fast

These are straightforward and well within your capability:
- `pom.xml` creation
- Entity classes (`Order`, `OrderItem`)
- Enum (`OrderStatus`)
- DTO classes
- `OrderRepository` interface
- `SwaggerConfig`
- `Dockerfile` and `.dockerignore`
- All documentation files (Phase 7)
- `run.cmd` updates

---

## 📋 Phase Execution Checklist

Before starting each phase:
1. ☐ Read `.agents.md` for rules
2. ☐ Read `task.md` for the specific tasks
3. ☐ Check caution zones above for the current phase

After completing each phase:
1. ☐ Run `cmd /c mvn clean compile` — must pass
2. ☐ Check that no entities are exposed in API responses
3. ☐ Check that no business logic is in controllers
4. ☐ Mark tasks complete in `task.md`
5. ☐ Report what was done to the user

---

## 🆘 WHEN TO STOP AND ASK FOR HELP

**Escalate to Opus (or the user) if:**

1. You're unsure about a state transition rule
2. You want to change the folder structure or architecture
3. You want to add a new dependency
4. A test is failing and you can't figure out why after 2 attempts
5. You're unsure whether something is a controller concern or service concern
6. The build is broken and the error is not a simple syntax/import issue
7. You're considering adding a feature not in the plan

**How to escalate**: Simply tell the user: *"⚠️ This needs Opus review before I proceed: [describe the issue]"*

---

## 📐 Key Class Signatures (Reference)

```java
// === Enum ===
public enum OrderStatus {
    PENDING, PROCESSING, SHIPPED, DELIVERED, CANCELLED
}

// === Service Interface ===
public interface OrderService {
    OrderResponse createOrder(CreateOrderRequest request);
    OrderResponse getOrderById(Long id);
    List<OrderResponse> getAllOrders(OrderStatus status); // status is nullable
    OrderResponse cancelOrder(Long id);
    int processPendingOrders(); // returns count of transitioned orders
}

// === Controller Endpoints ===
// POST   /api/orders          → 201 + OrderResponse
// GET    /api/orders/{id}     → 200 + OrderResponse
// GET    /api/orders          → 200 + List<OrderResponse>  (optional ?status=)
// PUT    /api/orders/{id}/cancel → 200 + OrderResponse

// === Repository ===
public interface OrderRepository extends JpaRepository<Order, Long> {
    List<Order> findByStatus(OrderStatus status);
}
```

---

## 🏁 Success Criteria

When all phases are complete, the project must:
- ✅ Compile without errors
- ✅ All tests pass (`cmd /c mvn test`)
- ✅ Swagger UI loads and all endpoints work
- ✅ Docker image builds and runs
- ✅ All 7 documentation files are written
- ✅ Code is clean, readable, and interview-ready
