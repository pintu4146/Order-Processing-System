@echo off
REM ============================================
REM  Order Processing System - Run Script
REM ============================================

SET CMD=%1
IF "%CMD%"=="" SET CMD=dev

IF "%CMD%"=="test" GOTO :test
IF "%CMD%"=="build" GOTO :build
IF "%CMD%"=="package" GOTO :package
IF "%CMD%"=="clean" GOTO :clean
IF "%CMD%"=="stop" GOTO :stop
IF "%CMD%"=="docker-build" GOTO :docker-build
IF "%CMD%"=="docker-run" GOTO :docker-run
IF "%CMD%"=="docker-stop" GOTO :docker-stop

REM Default: run the app
GOTO :run

:test
echo [INFO] Running all tests...
cmd /c mvn test
GOTO :end

:build
echo [INFO] Building project (skipping tests)...
cmd /c mvn clean compile -DskipTests
GOTO :end

:package
echo [INFO] Packaging as JAR...
cmd /c mvn clean package -DskipTests
GOTO :end

:clean
echo [INFO] Cleaning build artifacts...
cmd /c mvn clean
GOTO :end

:docker-build
echo [INFO] Building Docker image...
cmd /c docker build -t order-processing-system:latest .
GOTO :end

:docker-run
echo [INFO] Running Docker container...
cmd /c docker run -d --name order-processor -p 8080:8080 order-processing-system:latest
echo [INFO] App running at http://localhost:8080
GOTO :end

:docker-stop
echo [INFO] Stopping Docker container...
cmd /c docker stop order-processor
cmd /c docker rm order-processor
GOTO :end

:stop
echo [INFO] Stopping any process running on port 8080...
powershell -Command "Get-NetTCPConnection -LocalPort 8080 -ErrorAction SilentlyContinue | ForEach-Object { Stop-Process -Id $_.OwningProcess -Force -ErrorAction SilentlyContinue }"
GOTO :end

:run
echo [INFO] Checking if port 8080 is already in use...
powershell -Command "Get-NetTCPConnection -LocalPort 8080 -ErrorAction SilentlyContinue | ForEach-Object { echo '[WARN] Port 8080 is in use. Killing it...'; Stop-Process -Id $_.OwningProcess -Force -ErrorAction SilentlyContinue }"

echo [INFO] Starting Order Processing System with profile: %CMD%
echo [INFO] Swagger UI at http://localhost:8080/swagger-ui.html
echo [INFO] H2 Console at http://localhost:8080/h2-console
echo.
cmd /c mvn spring-boot:run -Dspring-boot.run.profiles=%CMD%
GOTO :end

:end
