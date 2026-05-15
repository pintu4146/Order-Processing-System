# Domain A: Core Logic (Unit & CIT Scenarios)

**Scope**: Entity mappings, DTO validations, Service rules (Open/Closed transitions).
**Execution Method**: Component Integration Testing (CIT) via `mvn clean test`.

## Scenario A1: Database Schema Initialization
- **Given**: The application starts up with an empty database.
- **When**: Flyway migration scripts (`V1__init_schema.sql`, `V2__add_total_amount.sql`) are executed.
- **Then**: The `orders` and `order_items` tables are created successfully with the correct column types (e.g., `totalAmount` as `DECIMAL`).

## Scenario A2: DTO Validation Restricts Invalid Input
- **Given**: A user submits a `CreateOrderRequest`.
- **When**: The request is missing a `customerName` OR contains an item with a negative `price`.
- **Then**: The `GlobalExceptionHandler` intercepts the request and returns a `400 Bad Request` with structured validation errors.

## Scenario A3: Order State Transition Guardrails (OCP)
- **Given**: An order exists in the database with a specific status.
- **When**: A request is made to transition the order to a new status (e.g., `PENDING` -> `CANCELLED` or `PROCESSING` -> `CANCELLED`).
- **Then**: The `OrderStatus.canTransitionTo()` logic evaluates the transition. Legal transitions succeed. Illegal transitions throw an `InvalidOrderStateException`.

## Scenario A4: Total Amount Calculation
- **Given**: An order is created with multiple items.
- **When**: The service persists the order.
- **Then**: The `totalAmount` field is accurately computed as the sum of `(price * quantity)` for all items using `BigDecimal` for financial precision.

## Execution Requirements
These scenarios are fully covered by the automated JUnit and Mockito test suite.
Command to execute: `mvn test`
Expected result: `BUILD SUCCESS` (24/24 tests pass).
