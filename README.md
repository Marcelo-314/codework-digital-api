# cwd-api

Backend API for CodeWork Digital.

Current state: backend foundation with PostgreSQL persistence and a local/sandbox contact submission API. This repository contains a Spring Boot application with Spring MVC, Spring JDBC, Flyway migrations, and Actuator health checks.

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

## Contact submissions API

Endpoint:

```text
POST /api/v1/contact-submissions
Content-Type: application/json
Idempotency-Key: <canonical UUID>
```

Request:

```json
{
  "source": "HOME",
  "locale": "es",
  "name": "Example Name",
  "email": "name@example.test",
  "phone": "+39 123 456",
  "companyOrProject": "Example project",
  "message": "Example message"
}
```

Allowed `source` values: `HOME`, `CONTACT_PAGE`.

Allowed `locale` values: `es`, `en`, `it`.

Required fields: `source`, `locale`, `name`, `email`, `message`.

Optional fields: `phone`, `companyOrProject`.

Successful creation returns HTTP 201:

```json
{
  "submissionId": "00000000-0000-0000-0000-000000000000",
  "status": "RECEIVED",
  "receivedAt": "2026-08-05T00:00:00Z"
}
```

Repeating the same `Idempotency-Key` with the same normalized payload returns HTTP 200 and the original response data. Reusing the same key with a different normalized payload returns HTTP 409 with `application/problem+json` and code `idempotency_conflict`.

Validation and request errors use Problem Details with stable `code` values and do not include submitted personal data.

This endpoint is ready only for local validation and technical sandbox use. It must not be exposed publicly in production yet: Turnstile, CORS, rate limiting, and exposure hardening are not configured.

## Docker

Run `clean verify` before building the image. Testcontainers requires Docker to execute PostgreSQL-backed integration tests; the Docker image build packages the application without running those tests again.

Build the image:

```bash
docker build --tag cwd-api:contact-api .
```

Run the container:

```bash
docker run --rm --publish 18081:18081 --env PORT=18081 --env SPRING_DATASOURCE_URL=jdbc:postgresql://host.docker.internal:5432/cwd_api --env SPRING_DATASOURCE_USERNAME=example_user --env SPRING_DATASOURCE_PASSWORD=example_password cwd-api:contact-api
```

## Not Implemented

This foundation does not include Turnstile, CORS, rate limiting, Render configuration, authentication, frontend integration, administrative APIs, or CI/CD.
