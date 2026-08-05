# cwd-api

Backend API for CodeWork Digital.

Current state: backend foundation with PostgreSQL persistence, server-side Turnstile verification, and a local/sandbox contact submission API. This repository contains a Spring Boot application with Spring MVC, Spring JDBC, Flyway migrations, and Actuator health checks.

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
TURNSTILE_SECRET_KEY
TURNSTILE_ALLOWED_HOSTNAMES
```

Optional Turnstile settings:

```text
TURNSTILE_SITEVERIFY_URL
TURNSTILE_HOME_ACTION
TURNSTILE_CONTACT_PAGE_ACTION
TURNSTILE_CONNECT_TIMEOUT
TURNSTILE_REQUEST_TIMEOUT
```

Default Turnstile actions are `contact_home` and `contact_page`. Hostnames are matched exactly; configure every allowed hostname explicitly. Development can use Cloudflare's public test keys, but do not store production secrets in this repository.

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
  "message": "Example message",
  "turnstileToken": "opaque-token"
}
```

Allowed `source` values: `HOME`, `CONTACT_PAGE`.

Allowed `locale` values: `es`, `en`, `it`.

Required fields: `source`, `locale`, `name`, `email`, `message`, `turnstileToken`.

Optional fields: `phone`, `companyOrProject`.

`turnstileToken` is opaque, required, and limited to 2048 characters. Each HTTP request must use a fresh Turnstile token. For retries of the same logical contact submission, keep the same `Idempotency-Key`, request a new Turnstile token, and resend the same normalized contact payload.

Successful creation returns HTTP 201:

```json
{
  "submissionId": "00000000-0000-0000-0000-000000000000",
  "status": "RECEIVED",
  "receivedAt": "2026-08-05T00:00:00Z"
}
```

Repeating the same `Idempotency-Key` with the same normalized payload and a fresh valid Turnstile token returns HTTP 200 and the original response data. Reusing the same key with a different normalized payload returns HTTP 409 with `application/problem+json` and code `idempotency_conflict`.

Turnstile rejection returns HTTP 400 with code `human_verification_failed`. Turnstile provider unavailability returns HTTP 503 with code `human_verification_unavailable`.

Validation and request errors use Problem Details with stable `code` values and do not include submitted personal data.

This endpoint is ready only for local validation and technical sandbox use. It must not be exposed publicly in production yet: CORS, rate limiting, Render configuration, frontend integration, and exposure hardening are not configured.

## Docker

Run `clean verify` before building the image. Testcontainers requires Docker to execute PostgreSQL-backed integration tests; the Docker image build packages the application without running those tests again.

Build the image:

```bash
docker build --tag cwd-api:turnstile .
```

Run the container:

```bash
docker run --rm --publish 18081:18081 --env PORT=18081 --env SPRING_DATASOURCE_URL=jdbc:postgresql://host.docker.internal:5432/cwd_api --env SPRING_DATASOURCE_USERNAME=example_user --env SPRING_DATASOURCE_PASSWORD=example_password --env TURNSTILE_SECRET_KEY=example_test_secret --env TURNSTILE_ALLOWED_HOSTNAMES=localhost cwd-api:turnstile
```

## Not Implemented

This foundation does not include CORS, rate limiting, Render configuration, authentication, frontend integration, administrative APIs, or CI/CD.
