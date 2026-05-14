# API Reference

**Base URL**: `http://localhost:8080`

---

## POST /api/orders

Create a new order. The order starts in `PENDING` status.

### Request

```json
{
  "customerName": "John Doe",
  "items": [
    { "productName": "Laptop", "quantity": 1, "price": 1200.00 },
    { "productName": "Mouse", "quantity": 2, "price": 25.99 }
  ]
}
```

### Validation Rules

| Field | Rule | Error |
|---|---|---|
| `customerName` | `@NotBlank` | 400 — must not be blank |
| `items` | `@NotEmpty` | 400 — must not be empty |
| `items[].productName` | `@NotBlank` | 400 — must not be blank |
| `items[].quantity` | `@NotNull`, `@Positive` | 400 — must be > 0 |
| `items[].price` | `@NotNull`, `@Positive` | 400 — must be > 0 |

### Response — 201 Created

```json
{
  "id": 1,
  "customerName": "John Doe",
  "status": "PENDING",
  "createdAt": "2026-05-15T03:30:00",
  "updatedAt": "2026-05-15T03:30:00",
  "items": [
    { "id": 1, "productName": "Laptop", "quantity": 1, "price": 1200.00 },
    { "id": 2, "productName": "Mouse", "quantity": 2, "price": 25.99 }
  ]
}
```

### Error — 400 Bad Request

```json
{
  "timestamp": "2026-05-15T03:30:00",
  "status": 400,
  "message": "Validation failed",
  "details": "One or more fields are invalid",
  "validationErrors": {
    "customerName": "must not be blank",
    "items": "must not be empty"
  }
}
```

---

## GET /api/orders/{id}

Retrieve a specific order by its ID.

### Response — 200 OK

```json
{
  "id": 1,
  "customerName": "John Doe",
  "status": "PENDING",
  "createdAt": "2026-05-15T03:30:00",
  "updatedAt": "2026-05-15T03:30:00",
  "items": [...]
}
```

### Error — 404 Not Found

```json
{
  "timestamp": "2026-05-15T03:30:00",
  "status": 404,
  "message": "Order not found with id: 99",
  "details": "Resource not found"
}
```

---

## GET /api/orders

List all orders. Optionally filter by status.

### Query Parameters

| Param | Type | Required | Values |
|---|---|---|---|
| `status` | String | No | `PENDING`, `PROCESSING`, `SHIPPED`, `DELIVERED`, `CANCELLED` |

### Examples

```
GET /api/orders                    → All orders
GET /api/orders?status=PENDING     → Only PENDING orders
GET /api/orders?status=PROCESSING  → Only PROCESSING orders
```

### Response — 200 OK

```json
[
  { "id": 1, "customerName": "John Doe", "status": "PENDING", ... },
  { "id": 2, "customerName": "Jane Smith", "status": "PROCESSING", ... }
]
```

---

## PUT /api/orders/{id}/cancel

Cancel an order. Only orders in `PENDING` status can be cancelled.

### Response — 200 OK

```json
{
  "id": 1,
  "customerName": "John Doe",
  "status": "CANCELLED",
  ...
}
```

### Error — 409 Conflict

```json
{
  "timestamp": "2026-05-15T03:30:00",
  "status": 409,
  "message": "Cannot cancel order 1. Current status is PROCESSING. Only PENDING orders can be cancelled.",
  "details": "Illegal state transition"
}
```

---

## Error Response Format

All errors follow a consistent structure:

```json
{
  "timestamp": "ISO-8601 datetime",
  "status": 400,
  "message": "Human-readable error message",
  "details": "Additional context",
  "validationErrors": { }
}
```

| HTTP Code | When |
|---|---|
| 400 | Validation failure (missing/invalid fields) |
| 404 | Order not found |
| 409 | Invalid state transition (e.g., cancelling a SHIPPED order) |
| 500 | Unexpected server error |
