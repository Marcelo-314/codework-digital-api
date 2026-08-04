# cwd-api

Backend API for CodeWork Digital.

Current state: backend foundation with PostgreSQL persistence. This repository contains a Spring Boot application with Spring MVC, Spring JDBC, Flyway migrations, and Actuator health checks.

## Requirements

- Java 21
- Docker
- PostgreSQL for local application runs

## Tests

Integration tests use Testcontainers with PostgreSQL. Docker must be available before running the verification command.

Windows PowerShell:

```powershell
.\mvnw.cmd -B -ntp clean verify
```

Unix:

```bash
./mvnw -B -ntp clean verify
```

## Local run

Flyway is the canonical source of the database schema. Start the application with a PostgreSQL datasource:

```text
SPRING_DATASOURCE_URL
SPRING_DATASOURCE_USERNAME
SPRING_DATASOURCE_PASSWORD
```

Windows PowerShell:

```powershell
.\mvnw.cmd -B -ntp package
$env:PORT = "18080"
java -jar target/cwd-api-0.0.1-SNAPSHOT.jar
```

Unix:

```bash
./mvnw -B -ntp package
PORT=18080 java -jar target/cwd-api-0.0.1-SNAPSHOT.jar
```

Health endpoint:

```text
GET /actuator/health
```

A healthy application returns HTTP 200 with status `UP`.

## Docker

Run `clean verify` before building the image. Testcontainers requires Docker to execute PostgreSQL-backed integration tests; the Docker image build packages the application without running those tests again.

Build the image:

```bash
docker build --tag cwd-api:persistence .
```

Run the container:

```bash
docker run --rm --publish 18081:18081 --env PORT=18081 --env SPRING_DATASOURCE_URL=jdbc:postgresql://host.docker.internal:5432/cwd_api --env SPRING_DATASOURCE_USERNAME=example_user --env SPRING_DATASOURCE_PASSWORD=example_password cwd-api:persistence
```

## Not Implemented

This foundation does not include a public contact API, Turnstile, Render configuration, authentication, business endpoints, or CI/CD.
