# cwd-api

Backend API for CodeWork Digital.

Current state: foundation only. This repository contains a minimal Spring Boot application with Spring MVC and Actuator health checks, prepared for later container execution on Render.

## Requirements

- Java 21
- Docker optional

## Tests

```bash
./mvnw -B -ntp clean verify
```

## Local run

```bash
./mvnw -B -ntp package
java -jar target/cwd-api-0.0.1-SNAPSHOT.jar
```

Configure the HTTP port with `PORT`:

```bash
PORT=18080 java -jar target/cwd-api-0.0.1-SNAPSHOT.jar
```

Health endpoint:

```text
GET /actuator/health
```

A healthy application returns HTTP 200 with status `UP`.

## Docker

Build the image:

```bash
docker build --tag cwd-api:foundation .
```

Run the container:

```bash
docker run --rm --publish 18081:18081 --env PORT=18081 cwd-api:foundation
```

## Not Implemented

This foundation does not include a contact API, persistence, PostgreSQL, Flyway, Turnstile, authentication, Render deployment, business endpoints, or CI/CD.
