# Render Sandbox

## Purpose

This document describes the internal Render sandbox declared by `render.yaml`. The Blueprint is the deployment contract for the existing sandbox resources managed from branch `develop`.

Repository changes do not perform a Render sync, deploy, API call, DNS change, custom domain setup, frontend integration, or production configuration. Apply Blueprint changes manually from Render only after review and merge.

## Defined Resources

- Project: `cwd-contact-sandbox`
- Environment: `Sandbox`
- Web Service: `cwd-api-sandbox`
- PostgreSQL: `cwd-api-sandbox-db`

The Blueprint is the canonical source for this project/environment structure. The Web Service builds this repository with `runtime: docker` and the existing `Dockerfile`. PostgreSQL is attached through Render Blueprint references, not through copied credentials.

## Region

Both resources must remain in `frankfurt`. Keeping the Web Service and PostgreSQL in the same region is required so the service uses Render's internal database connection.

## Plans And Limitations

- Web Service plan: `starter`
- PostgreSQL plan: `basic-256mb`
- PostgreSQL major version: `17`
- PostgreSQL database name: `cwd_api`
- PostgreSQL user: `cwd_api`
- Web instances: `1`
- Web Service auto deploy: disabled with `autoDeployTrigger: off`
- Preview environments: disabled with `previews.generation: off`

The web filesystem is ephemeral. The paid PostgreSQL instance removes the Free-tier expiration, while storage remains intentionally undeclared in the Blueprint unless a separate storage decision is reviewed. These resources are internal sandbox infrastructure, not production.

## Existing Render Resources

The Render dashboard currently has Blueprint-managed sandbox resources:

- Workspace: `Marcelo's workspace`
- Project: `cwd-contact-sandbox`
- Environment: `Sandbox`
- Web Service: `cwd-api-sandbox`
- PostgreSQL: `cwd-api-sandbox-db`

Do not change these resources manually before updating the Blueprint. Review and merge repository changes first, then use Manual Sync from the Blueprint so the dashboard stays aligned with version control.

## Required Variables

The Blueprint obtains database values from `cwd-api-sandbox-db`:

- `RENDER_DATABASE_URL`: `connectionString`
- `SPRING_DATASOURCE_USERNAME`: `user`
- `SPRING_DATASOURCE_PASSWORD`: `password`

These values are prompted or synchronized manually and must not be committed:

- `TURNSTILE_SECRET_KEY`
- `TURNSTILE_ALLOWED_HOSTNAMES`
- `TURNSTILE_HOME_ACTION`
- `TURNSTILE_CONTACT_PAGE_ACTION`
- `CWD_ALLOWED_ORIGINS`
- `CWD_ADMIN_USERNAME`
- `CWD_ADMIN_PASSWORD`

These explicit non-secret values are declared in the Blueprint:

- `CWD_CORS_MAX_AGE=1h`
- `CWD_MAX_CONTACT_REQUEST_BYTES=65536`
- `SERVER_MAX_HTTP_REQUEST_HEADER_SIZE=8KB`

## PostgreSQL Integration

Render's internal PostgreSQL `connectionString` has this shape:

```text
postgresql://user:password@host:port/database
```

The PostgreSQL JDBC driver expects:

```text
jdbc:postgresql://host:port/database
```

The Docker entrypoint derives `SPRING_DATASOURCE_URL` from `RENDER_DATABASE_URL` only when `SPRING_DATASOURCE_URL` is absent. It strips the `postgresql://` scheme and userinfo before the first `@`, then prefixes the host/database portion with `jdbc:postgresql://`. It does not print the URL, username, password, or original Render value.

PostgreSQL public access is blocked with `ipAllowList: []`. Do not use the external database URL for this sandbox.

## Turnstile Sandbox

Recommended approach:

- Create a Turnstile widget dedicated to sandbox validation.
- Use a sandbox-only secret.
- Configure only the expected frontend sandbox hostname or `localhost`.
- Keep the actions `contact_home` and `contact_page`.

Short controlled smoke alternative:

- Use Cloudflare's official dummy credentials only for a brief smoke.
- Configure the expected hostname as `localhost`.
- Configure both actions as `test`.
- Use only the official dummy token during that smoke.
- Remove or replace dummy configuration when the smoke is finished.

Do not commit a real secret, dummy secret, sitekey, or token. A dummy configuration that always approves must not remain on a persistent public endpoint. CORS does not stop server-to-server calls, so delete or harden the sandbox after validation.

## CORS Sandbox

`CWD_ALLOWED_ORIGINS` is the browser origin of the frontend, not the backend hostname. Use exact origins only.

Fictitious examples:

```text
http://localhost:3000
https://frontend-sandbox.example.test
```

Do not use a wildcard, production origin, invented future Render URL, or broader CORS setting to simplify tests. `curl` smoke requests can omit the `Origin` header.

## Blueprint Updates

After a `render.yaml` change is reviewed and merged into `develop`, open the existing Blueprint in Render and review the pending diff before syncing. No infrastructure change occurs during review. When you run Manual Sync, Render applies the Blueprint updates and may redeploy affected resources.

For updates that require prompted `sync: false` values, provide them in the Render Dashboard. Do not paste values into the repository. Confirm in the preview:

- Project is `cwd-contact-sandbox`.
- Environment is `Sandbox`.
- Branch is `develop`.
- Web Service and PostgreSQL are in `frankfurt`.
- Web Service auto deploy is off.
- PostgreSQL `ipAllowList` is empty.
- The health check path is `/actuator/health`.

## Deploy Controls

Two separate controls apply:

- Web Service auto deploy is configured in YAML with `autoDeployTrigger: off`. Ordinary commits to `develop` should not trigger the Web Service's Git deploy automatically; later service deploys should be manual.
- Blueprint Auto Sync is a separate Render Blueprint setting. If Auto Sync remains enabled, later changes to `render.yaml` on the linked branch can synchronize infrastructure and redeploy affected resources. `autoDeployTrigger: off` does not disable Blueprint Auto Sync.

After the Blueprint is managed by Render:

1. Open the Blueprint settings in Render.
2. Set Auto Sync = No.
3. Verify future Blueprint updates require Manual Sync.
4. Keep `autoDeployTrigger: off` for the Web Service.

Manual Deploy and Manual Sync are not equivalent. Manual Deploy operates on the Web Service deploy lifecycle for code/image changes. Manual Sync reapplies the Blueprint contract and can change resource configuration.

## Sync Validation

When a Manual Sync changes the sandbox instance types, verify the update sequence in order:

1. Web Service remains in Frankfurt and changes to `starter`.
2. PostgreSQL remains in Frankfurt and changes to `basic-256mb`.
3. Docker still uses `./Dockerfile`.
4. The image keeps `ENTRYPOINT ["/app/docker-entrypoint.sh"]`.
5. The container keeps `CMD ["java", "-jar", "/app/cwd-api.jar"]`.
6. Render keeps injecting the `fromDatabase` values.
7. The entrypoint derives the JDBC URL.
8. Spring Boot starts.
9. Flyway validates or applies migrations as needed.
10. `/actuator/health` returns HTTP 200 and status `UP`.
11. Render marks the deploy live or otherwise satisfactory.

If any step fails, review deploy logs. Do not modify variables at random, enable PostgreSQL public access, replace the internal URL with the external URL, or enable autodeploys. Correct the cause and run a controlled Manual Sync or Manual Deploy as appropriate.

## HTTP Smoke

Use the deployed backend URL only after Render creates it. Do not invent or commit that URL.

Health:

```bash
curl -i https://backend-sandbox.example.test/actuator/health
```

Contact submission:

```bash
curl -i \
  -X POST https://backend-sandbox.example.test/api/v1/contact-submissions \
  -H "Content-Type: application/json" \
  -H "Idempotency-Key: 00000000-0000-0000-0000-000000000000" \
  --data '{"source":"HOME","locale":"es","name":"Example Name","email":"name@example.test","message":"Example message","turnstileToken":"opaque-token"}'
```

Expected results are HTTP 201 for first creation and HTTP 200 for a replay with the same normalized payload and a fresh Turnstile token.

## Flyway Verification

Flyway is the canonical schema mechanism. Verify startup logs show migration `1 - create contact submission` applied successfully, then confirm the database contains:

- `flyway_schema_history`
- `contact_submission`

Use Render database tooling only after explicit approval for remote infrastructure operations.

## Log Review

Review startup and request logs for:

- No printed `RENDER_DATABASE_URL`
- No printed credential-bearing JDBC URL
- No printed database password
- No printed Turnstile secret
- Successful Flyway V1
- Health check success
- Contact endpoint status codes

Do not store smoke logs in the repository.

## Controlled Restart

Restart only the Web Service from Render after deploy validation. Confirm it starts again without manual database URL changes and returns `/actuator/health` as `UP`.

## Rollback

Rollback is manual. A Web Service rollback can restore an earlier service deploy, but it does not automatically revert structural Blueprint changes, PostgreSQL data, or Flyway migrations.

To change infrastructure configuration after Auto Sync is set to No, update `render.yaml`, review the diff, and use Manual Sync from the Blueprint. To redeploy code without changing infrastructure, use Manual Deploy on the Web Service.

## Teardown

When validation is complete, remove the Blueprint-managed sandbox resources from Render unless the team explicitly decides to keep them for more testing. Confirm deletion of:

- `cwd-api-sandbox`
- `cwd-api-sandbox-db`

No DNS or custom domain teardown is expected because this sandbox does not define them.

## Approval Criteria

- Blueprint creates only the declared sandbox Web Service and PostgreSQL resources.
- Resources are grouped under project `cwd-contact-sandbox` and environment `Sandbox`.
- Both resources are in Frankfurt.
- Web Service deploys branch `develop`.
- Web Service auto deploy and previews are disabled.
- Blueprint Auto Sync is set to No after initial creation.
- PostgreSQL public access is blocked.
- Database values are referenced with `fromDatabase`.
- Secret-like values use `sync: false`.
- The Docker image still supports direct `SPRING_DATASOURCE_URL`.
- The entrypoint derives JDBC URL from Render's internal `connectionString`.
- Flyway V1 applies during startup.
- Health and contact smoke checks pass.
- No secrets or real infrastructure identifiers are committed.

## What This Sandbox Does Not Represent

This sandbox is not production. It does not define billing controls, backups, HA, replicas, autoscaling, a persistent disk, connection pooling, DNS, a custom domain, rate limiting, alerting, structured logging, frontend integration, Cloudflare configuration, or CI/CD.
