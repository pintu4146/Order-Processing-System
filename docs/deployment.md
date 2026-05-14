# Deployment Guide

## Local Development

### Prerequisites
- Java 17+
- Maven 3.9+

### Quick Start
```cmd
copy .env.example .env
run.cmd
```

The app starts on `http://localhost:8080` with the `dev` profile by default.

---

## Docker (Phase 8 — Coming Soon)

### Build Image
```cmd
run.cmd docker-build
```

This runs:
```cmd
docker build -t order-processing-system:latest .
```

### Run Container
```cmd
run.cmd docker-run
```

This runs:
```cmd
docker run -d --name order-processor -p 8080:8080 \
  -e DB_USERNAME=sa \
  -e DB_PASSWORD=password \
  order-processing-system:latest
```

### Stop Container
```cmd
run.cmd docker-stop
```

### Verify
- Swagger UI: http://localhost:8080/swagger-ui.html
- Health: http://localhost:8080/api/orders (should return `[]`)

---

## Environment Configuration

### Profiles

| Profile | Usage | H2 Console | SQL Logging | Flyway Clean |
|---|---|---|---|---|
| `dev` | Local development | ✅ Enabled | ✅ DEBUG | Allowed |
| `stage` | Staging/QA | ❌ Disabled | ❌ Off | Disabled |
| `prod` | Production | ❌ Disabled | ❌ Off | Disabled |

### Environment Variables

| Variable | Required | Default (dev) | Description |
|---|---|---|---|
| `DB_USERNAME` | Stage/Prod | `sa` | Database username |
| `DB_PASSWORD` | Stage/Prod | `password` | Database password |
| `SPRING_PROFILES_ACTIVE` | No | `dev` | Active Spring profile |
| `SERVER_PORT` | No | `8080` | Application port |

### Supplying Variables

**Local (via .env file)**:
```env
DB_USERNAME=sa
DB_PASSWORD=password
```
The `run.cmd` script auto-loads `.env` if it exists.

**Docker**:
```cmd
docker run -e DB_USERNAME=sa -e DB_PASSWORD=secret order-processing-system
```

**Production (AWS/Azure/GCP)**:
Use your cloud provider's secret manager (AWS Secrets Manager, Azure Key Vault, etc.) to inject environment variables at container runtime.

---

## Build Commands Reference

| Command | What It Does |
|---|---|
| `run.cmd` | Start app (dev profile, kills port 8080 first) |
| `run.cmd test` | Run all 24 tests |
| `run.cmd build` | Compile only (skip tests) |
| `run.cmd package` | Build JAR at `target/*.jar` |
| `run.cmd clean` | Delete `target/` directory |
| `run.cmd stop` | Kill any process on port 8080 |
| `run.cmd docker-build` | Build Docker image |
| `run.cmd docker-run` | Run Docker container |
| `run.cmd docker-stop` | Stop + remove Docker container |
