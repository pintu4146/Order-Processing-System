# Domain D: Infrastructure & Deployment Scenarios

**Scope**: Containerization, Environment Variables, Profile Management.
**Execution Method**: Docker Build & Run.

## Scenario D1: Docker Multi-Stage Build Efficiency
- **Given**: A machine with Docker installed.
- **When**: The command `docker build -t order-processing-system .` is executed.
- **Then**: 
  1. The build executes in two stages (Maven build -> JRE runtime).
  2. The resulting image contains only the compiled `.jar` and the JRE, excluding the JDK and Maven source code.
  3. The image size is heavily optimized (typically < 300MB).

## Scenario D2: Environment Variable Injection
- **Given**: The Docker image is successfully built.
- **When**: The container is started using:
  `docker run -e SPRING_PROFILES_ACTIVE=prod -e SPRING_DATASOURCE_USERNAME=qa_user -p 8080:8080 order-processing-system`
- **Then**: 
  1. The application boots up using the `application-prod.yml` profile.
  2. The `SPRING_DATASOURCE_USERNAME` correctly overrides the default YAML configuration, demonstrating secure credentials management without hardcoding.
  3. The `ENTRYPOINT` in the Dockerfile correctly processes the environment variables using the `exec` form (`["java", "-jar", "app.jar"]`).
