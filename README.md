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
CWD_ALLOWED_ORIGINS
CWD_ADMIN_USERNAME
CWD_ADMIN_PASSWORD
```

Optional Turnstile settings:

```text
TURNSTILE_SITEVERIFY_URL
TURNSTILE_HOME_ACTION
TURNSTILE_CONTACT_PAGE_ACTION
TURNSTILE_CONNECT_TIMEOUT
TURNSTILE_REQUEST_TIMEOUT
CWD_CORS_MAX_AGE
CWD_MAX_CONTACT_REQUEST_BYTES
SERVER_MAX_HTTP_REQUEST_HEADER_SIZE
```

Default Turnstile actions are `contact_home` and `contact_page`. Hostnames are matched exactly; configure every allowed hostname explicitly. Development can use Cloudflare's public test keys, but do not store production secrets in this repository.

`CWD_ALLOWED_ORIGINS` is required and must contain exact comma-separated browser origins, for example `http://localhost:3000`. Wildcards and origin patterns are not accepted. CORS is configured only for `POST /api/v1/contact-submissions`, does not allow credentials, and authorizes only the request headers needed by the contact contract. Requests without an `Origin` header continue to work for local tools and server-to-server validation.

`CWD_ADMIN_USERNAME` and `CWD_ADMIN_PASSWORD` are required external credentials for the read-only administrative API. Do not commit real values or print them in logs.

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

Validation and request errors use Problem Details with stable `code` values and do not include submitted personal data. Unknown JSON properties are rejected with code `invalid_request`. Contact submission request bodies are limited to 65,536 bytes by default; larger bodies return HTTP 413 with code `request_too_large`.

API responses under `/api/` include defensive no-store, nosniff, no-referrer, and deny-all content security policy headers.

This endpoint is ready for local validation and controlled technical sandbox use. It must not be considered fully published in production yet: rate limiting, Render configuration, frontend integration, trusted proxy policy, and operational hardening are still pending.

## Admin contact submissions API

Endpoint:

```text
GET /api/admin/contact-submissions?page=0&size=20
Authorization: Basic <credentials>
```

This read-only endpoint requires `CWD_ADMIN_USERNAME` and `CWD_ADMIN_PASSWORD`. Responses are paginated, ordered by `createdAt` descending, and intentionally exclude payload hashes, idempotency keys, Turnstile internals, and secrets.

## Render sandbox

This repository includes `render.yaml` as the Render Blueprint contract for the internal sandbox. It explicitly defines project `cwd-contact-sandbox`, environment `Sandbox`, a Docker Web Service, and PostgreSQL in Frankfurt. The Blueprint is the canonical source for that structure, deploys branch `develop`, and uses `/actuator/health` for health checks.

The Blueprint does not contain secrets and does not create resources by itself in this commit. Database credentials are referenced from Render PostgreSQL with `fromDatabase`. `RENDER_DATABASE_URL` receives Render's internal `postgresql://user:password@host:port/database` connection string, and the Docker entrypoint derives the JDBC URL required by pgJDBC without printing credentials. Direct `SPRING_DATASOURCE_URL=jdbc:postgresql://...` remains supported for local and non-Render runs.

The sandbox resources are Blueprint-managed. Changes to `render.yaml` should be reviewed and merged before a manual Blueprint sync is performed from Render. `autoDeployTrigger: off` controls the Web Service's Git autodeploys; Blueprint Auto Sync is separate and should remain set to No so infrastructure changes require Manual Sync.

See [docs/render-sandbox.md](docs/render-sandbox.md) for provisioning, smoke, rollback, and teardown guidance.

## Docker

Run `clean verify` before building the image. Testcontainers requires Docker to execute PostgreSQL-backed integration tests; the Docker image build packages the application without running those tests again.

Build the image:

```bash
docker build --tag cwd-api:http-hardening .
```

Run the container:

```bash
docker run --rm --publish 18081:18081 --env PORT=18081 --env SPRING_DATASOURCE_URL=jdbc:postgresql://host.docker.internal:5432/cwd_api --env SPRING_DATASOURCE_USERNAME=local_user_placeholder --env SPRING_DATASOURCE_PASSWORD=local_password_placeholder --env TURNSTILE_SECRET_KEY=local_turnstile_secret_placeholder --env TURNSTILE_ALLOWED_HOSTNAMES=localhost --env CWD_ALLOWED_ORIGINS=http://localhost:3000 --env CWD_ADMIN_USERNAME=local_admin_placeholder --env CWD_ADMIN_PASSWORD=local_admin_password_placeholder cwd-api:http-hardening
```

## Not Implemented

This foundation does not include rate limiting, sandbox provisioning, remote smoke validation, DNS or custom domain setup, production Render configuration, authentication, frontend integration, administrative APIs, or CI/CD.
