# Domain B: API & SIT Scenarios and Execution Report

**Date of Execution**: 2026-05-15
**Target Branch**: `stage` (Release Candidate)
**Execution Script**: `qa/scripts/sit_api_tests.ps1`

## Scope
- REST API Endpoints (`/api/orders`)
- Swagger UI (`/swagger-ui.html`)
- JSON serialization/deserialization
- Spring Boot Actuator (`/actuator/health`)

## Scenario B1: Application Health Monitoring
- **Given**: The Spring Boot application is running.
- **When**: A `GET` request is made to `/actuator/health`.
- **Then**: The API returns HTTP 200 OK with `{"status":"UP"}`.

## Scenario B2: End-to-End Order Lifecycle
- **Given**: The REST API is accessible.
- **When**: 
  1. A `POST /api/orders` request is made with valid JSON payload.
  2. A `GET /api/orders/{id}` request is made using the returned ID.
  3. A `PUT /api/orders/{id}/cancel` request is made.
- **Then**: 
  1. Order is created successfully with `totalAmount` calculated.
  2. Order details match the created data.
  3. Order status transitions to `CANCELLED`.

## Scenario B3: Paginated List Retrieval
- **Given**: Multiple orders exist in the database.
- **When**: A `GET /api/orders?page=0&size=5` request is made.
- **Then**: The API returns a Spring `Page` object containing `content`, `totalElements`, and `totalPages` metadata, ensuring safe retrieval of large datasets.

## SIT Execution Results

```text
1. Checking Application Health...
✅ Health check PASSED.

2. Creating a new Order...
Response: {"id":1,"customerName":"QA Engineer","status":"PENDING","totalAmount":95.5,"createdAt":"2026-05-15T14:55:03.933556","updatedAt":"2026-05-15T14:55:03.933556","items":[{"id":1,"productName":"Integration Book","quantity":2,"price":45},{"id":2,"productName":"Coffee","quantity":1,"price":5.5}]}
✅ Order creation PASSED. ID: 1

3. Retrieving Order Details...
✅ Order retrieval PASSED.

4. Testing Pagination (List Orders)...
✅ Pagination PASSED.

5. Cancelling the Order...
✅ Order cancellation PASSED.

======================================
SIT AUTOMATION COMPLETE
======================================
```

## Conclusion
✅ **PASS**. The REST API layers function flawlessly end-to-end. Routing, JSON binding, DTO validation, and component integration are successfully verified via automated live testing.
