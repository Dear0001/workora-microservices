# Workora Microservices

This project is a Spring Boot microservice skeleton built from the Workora backend plan and the Spring Boot best-practice guidance in SKILL-BACKEND.md.

## Database ownership rule

Each service owns its own database/schema and must keep data isolated from other services:

| Service | Database/schema |
| --- | --- |
| auth-service | auth_db |
| user-service | user_db |
| organization-service | organization_db |
| project-service | project_db |
| work-service | task_db |
| bug-service | bug_db |
| recruitment-service | recruitment_db |
| notification-service | notification_db |
| payment-service | payment_db |
| api-gateway | gateway_db (optional, usually stateless) |

## Recommended layer responsibility

- controller: HTTP API handling
- service: business logic and validation
- repository: database access
- entity: persistence model
- dto: request/response payloads
- mapper: entity <-> DTO conversion
- client: outbound calls to other services
- event: RabbitMQ/Kafka events
- config: security, OpenAPI, CORS, gateway, and shared configuration
- exception: global exception handling
- util: cross-cutting utilities

## Service map

- api-gateway: routing, authentication validation, CORS, rate limiting, correlation ID propagation
- auth-service: OAuth/OIDC, registration, email verification, password reset, refresh sessions
- user-service: profile, experience, avatar, personal settings, account status
- organization-service: companies, memberships, departments, roles, invitations
- project-service: projects, phases, public/private visibility, Bug-Fix Jobs
- work-service: tasks, assignments, submissions, attachments, workspace visibility
- bug-service: bug reports, evidence, triage, review, lifecycle states
- recruitment-service: recruitment posts, applications, approval workflow
- notification-service: inbox, email, read/archive, preferences, delivery retries
- payment-service: fee approval, KHQR generation, payment verification, dispute flow

## Detailed endpoint design

See [API-ENDPOINTS.md](./API-ENDPOINTS.md) for the deep endpoint-by-endpoint contract map derived from the backend development plan.

See [AUTH-TEST-PLAN.md](./AUTH-TEST-PLAN.md) for the authentication test
cases, execution order, fraud scanning checklist, and production security
notices.

## Run locally

```bash
cd Workora-Microservices
mvn test
```

## Start infrastructure with Docker

The [docker-compose.yml](./docker-compose.yml) file starts Keycloak and one
PostgreSQL container with a persistent volume for each database-owning service:

```bash
cd Workora-Microservices
docker compose up -d
```

Check database status with:

```bash
docker compose ps
```

### Local PostgreSQL credentials

Each PostgreSQL container uses these development-only defaults:

| Setting | Value |
| --- | --- |
| Username | `workora` |
| Password | `workora123` |
| Host ports | `5433` through `5440` |
| Container port | `5432` |

The database names are listed in the
[database ownership table](#database-ownership-rule). For example, Identity
Service uses `identity_db` on `localhost:5433`:

```text
jdbc:postgresql://localhost:5433/identity_db
```

The password `workora123` is only for local development. Do not reuse it in
staging or production. Replace the `POSTGRES_PASSWORD` values in
`docker-compose.yml` with deployment secrets, and update each service's
`DB_PASSWORD` environment variable to match. Existing PostgreSQL volumes keep
their original credentials; changing the Compose file alone does not change
an already-initialized database password.

Keycloak is available at:

```text
http://localhost:8180
```

The local realm is `workora`, imported from
[keycloak/realm-export.json](./keycloak/realm-export.json). The configured
public client is `workora-api`. Local administrator credentials are
`admin` / `admin`; change them before sharing or deploying this environment.

Open the administration console at:

```text
http://localhost:8180/admin/master/console/
```

If the admin login does not work after changing credentials, recreate only
the Keycloak container:

```bash
docker compose rm -sf keycloak
docker compose up -d keycloak
docker compose logs -f keycloak
```

Wait for the logs to show that Keycloak has started and the `workora` realm
has been imported. Use a private browser window if an old Keycloak cookie
remains.

The OpenID Connect issuer is:

```text
http://localhost:8180/realms/workora
```

Obtain a token from:

```text
http://localhost:8180/realms/workora/protocol/openid-connect/token
```

For local password-grant testing, create a user in the `workora` realm and
request a token:

```bash
curl -X POST "http://localhost:8180/realms/workora/protocol/openid-connect/token" ^
  -H "Content-Type: application/x-www-form-urlencoded" ^
  -d "client_id=workora-api" ^
  -d "grant_type=password" ^
  -d "username=your-user@example.com" ^
  -d "password=your-password"
```

Copy the `access_token` from the response and use it in Swagger's
**Authorize** dialog.

Send the returned access token to protected APIs as:

```http
Authorization: Bearer <access-token>
```

The identity service validates Keycloak-issued JWTs using the
`KEYCLOAK_ISSUER_URI` setting. Keycloak is used for API token validation;
legacy custom identity endpoints remain available while clients migrate.

The databases are exposed on host ports `5433` through `5440`; inside the
Compose network each database listens on its normal PostgreSQL port `5432`.
Each Spring Boot service is configured to use its matching database by
default. Start a service after the databases are healthy, for example:

```bash
mvn spring-boot:run -pl identity-service
```

The datasource settings support `DB_URL`, `DB_USERNAME`, and `DB_PASSWORD`
environment variables for deployment-specific connection values.

### Gmail OTP email configuration

The identity service sends verification and password-reset OTPs through Gmail
SMTP. Set a Google app password in the same PowerShell window used to start
the service:

```powershell
$env:MAIL_USERNAME="your-gmail-address@gmail.com"
$env:MAIL_PASSWORD="your-16-character-app-password"
mvn spring-boot:run -pl identity-service
```

Do not use the normal Gmail password or commit the app password. If
`MAIL_PASSWORD` is not set, Spring's mail health check reports
`AuthenticationFailedException: no password specified`, and OTP emails cannot
be sent.

## Swagger UI

Open Swagger through the gateway using this single format:

| Service | Swagger URL |
| --- | --- |
| API Gateway | http://localhost:8080/gateway/swagger-ui.html |
| Identity | http://localhost:8080/identify/swagger-ui.html |
| Organization | http://localhost:8080/organization/swagger-ui.html |
| Project | http://localhost:8080/project/swagger-ui.html |
| Work | http://localhost:8080/work/swagger-ui.html |
| Bug | http://localhost:8080/bug/swagger-ui.html |
| Recruitment | http://localhost:8080/recruitment/swagger-ui.html |
| Notification | http://localhost:8080/notification/swagger-ui.html |
| Payment | http://localhost:8080/payment/swagger-ui.html |

Use the gateway URLs above as the single Swagger access format. The individual
service ports are for service traffic and are not the documented Swagger entry
points.

## Identity authentication

The recommended Keycloak authentication flow is:

1. Start Keycloak with `docker compose up -d keycloak`.
2. Create or manage users in the `workora` realm.
3. Request an access token for the `workora-api` client.
4. Add the token to Swagger with **Authorize**.
5. Call protected APIs through the gateway.

Keycloak realm registration and reset-password features are enabled for local
development. Production deployments should use secure administrator
credentials, HTTPS, restricted redirect URIs, and externally managed secrets.
