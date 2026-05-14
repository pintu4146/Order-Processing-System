# Assumptions

The following assumptions were made during the development of this project.

---

## Business Logic

1. **Orders start in PENDING status** — there is no draft or validation state before PENDING.
2. **Only PENDING orders can be cancelled** — once an order moves to PROCESSING, it cannot be cancelled.
3. **The scheduler transitions PENDING → PROCESSING** — this is a batch operation that affects all pending orders simultaneously. There is no per-order delay or prioritization.
4. **SHIPPED → DELIVERED transitions are not implemented** — the enum values exist for completeness, but no endpoint or scheduler triggers these transitions. This is intentionally out of scope.
5. **Order items cannot be modified after creation** — once an order is placed, its items are immutable.
6. **No pricing/total calculation** — the system stores item prices but does not compute order totals. This would be added in a real system.
7. **No inventory management** — creating an order does not check or decrement stock levels.
8. **No user/authentication model** — `customerName` is a plain string, not a foreign key to a users table.

## Technical

9. **H2 in-memory database is acceptable** — data is not persisted between application restarts. This is a deliberate choice for an interview project. A real system would use PostgreSQL, MySQL, or similar.
10. **Single-node deployment** — the scheduler uses Spring's built-in `@Scheduled`, which is not cluster-safe. In a multi-instance deployment, a distributed scheduler (e.g., ShedLock, Quartz) would be needed to prevent duplicate processing.
11. **No pagination** — the `GET /api/orders` endpoint returns all orders in a single response. With a large dataset, this would need pagination (`Pageable`).
12. **No rate limiting** — API endpoints have no throttling. A production deployment would add rate limiting at the gateway level.
13. **Flyway manages schema exclusively** — Hibernate's `ddl-auto` is set to `validate`, not `create` or `update`. All schema changes go through versioned SQL migrations.
14. **Default scheduler interval is 5 minutes** — this is configurable via `app.scheduler.interval` in `application.yml`. For testing, it can be reduced.
15. **Credentials are provided via environment variables** — `.env` files are git-ignored. The `application-dev.yml` provides defaults (`sa`/`password`) for zero-config local startup. Stage and prod profiles have no defaults and will fail to start without proper environment configuration.
