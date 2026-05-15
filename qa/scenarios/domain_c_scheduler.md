# Domain C: Background Scheduler Scenarios

**Scope**: Automated transition of orders from `PENDING` to `PROCESSING`.
**Execution Method**: Manual Verification against Live System.

## Scenario C1: Automated Status Transition
- **Given**: The Spring Boot application is running with scheduling enabled (`@EnableScheduling`).
- **When**: A new order is created via the API. Its initial status is explicitly set to `PENDING`.
- **And**: 10 seconds elapse (as configured by `app.scheduler.interval` in `application.yml`).
- **Then**: The `OrderStatusScheduler` executes `processPendingOrders()`.
- **And**: A subsequent `GET /api/orders/{id}` request reveals the order's status has been transitioned to `PROCESSING`.

## Scenario C2: Scheduler Idempotency
- **Given**: The application is running, but there are exactly **zero** `PENDING` orders in the database.
- **When**: The scheduled interval triggers.
- **Then**: The scheduler executes, queries the database, finds no eligible orders, logs "No pending orders to process", and safely completes without throwing errors or modifying existing `PROCESSING`, `SHIPPED`, or `CANCELLED` orders.

## Scenario C3: Bulk Processing
- **Given**: 5 orders are rapidly created via the API and all sit in `PENDING` state.
- **When**: The scheduled interval triggers.
- **Then**: All 5 orders are queried, updated, and persisted in a single transactional batch operation (`saveAll`), rather than looping individual database commits.
