@echo off
REM ============================================
REM  Order Processing System - Run Script
REM ============================================
REM  Usage:
REM    run.cmd              - Run with dev profile (default)
REM    run.cmd dev          - Run with dev profile
REM    run.cmd stage        - Run with stage profile
REM    run.cmd prod         - Run with prod profile
REM    run.cmd test         - Run all tests
REM    run.cmd build        - Build the project (skip tests)
REM    run.cmd package      - Package as JAR
REM    run.cmd clean        - Clean build artifacts
REM    run.cmd docker-build - Build Docker image
REM    run.cmd docker-run   - Run Docker container
REM    run.cmd docker-stop  - Stop Docker container
REM ============================================

SET PROFILE=%1
IF "%PROFILE%"=="" SET PROFILE=dev

IF "%PROFILE%"=="test" (
    echo [INFO] Running all tests...
    cmd /c mvn test
    goto :end
)

IF "%PROFILE%"=="build" (
    echo [INFO] Building project (skipping tests)...
    cmd /c mvn clean compile -DskipTests
    goto :end
)

IF "%PROFILE%"=="package" (
    echo [INFO] Packaging as JAR...
    cmd /c mvn clean package -DskipTests
    goto :end
)

IF "%PROFILE%"=="clean" (
    echo [INFO] Cleaning build artifacts...
    cmd /c mvn clean
    goto :end
)

IF "%PROFILE%"=="docker-build" (
    echo [INFO] Building Docker image...
    cmd /c docker build -t order-processing-system:latest .
    goto :end
)

IF "%PROFILE%"=="docker-run" (
    echo [INFO] Running Docker container...
    cmd /c docker run -d --name order-processor -p 8080:8080 order-processing-system:latest
    echo [INFO] App running at http://localhost:8080
    echo [INFO] Swagger UI at http://localhost:8080/swagger-ui.html
    goto :end
)

IF "%PROFILE%"=="docker-stop" (
    echo [INFO] Stopping Docker container...
    cmd /c docker stop order-processor
    cmd /c docker rm order-processor
    goto :end
)

echo [INFO] Starting Order Processing System with profile: %PROFILE%
echo [INFO] Swagger UI will be at http://localhost:8080/swagger-ui.html
echo [INFO] H2 Console will be at http://localhost:8080/h2-console
echo.
cmd /c mvn spring-boot:run -Dspring-boot.run.profiles=%PROFILE%

:end
