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
- identity-service: Keycloak registration/login, email verification, password reset, token sessions, and user profiles
- user-service: profile, experience, avatar, personal settings, account status
- organization-service: companies, memberships, departments, roles, invitations
- project-service: projects, phases, public/private visibility, Bug-Fix Jobs
- work-service: tasks, assignments, submissions, attachments, workspace visibility
- bug-service: bug reports, evidence, triage, review, lifecycle states
- recruitment-service: recruitment posts, applications, approval workflow
- notification-service: inbox, email, read/archive, preferences, delivery retries
- payment-service: fee approval, KHQR generation, payment verification, dispute flow

## Detailed endpoint design

See [API-ENDPOINTS.md](identity-service/API-ENDPOINTS.md) for the deep endpoint-by-endpoint contract map derived from the backend development plan.

See [AUTH-TEST-PLAN.md](identity-service/AUTH-TEST-PLAN.md) for the authentication test
cases, execution order, fraud scanning checklist, and production security
notices.

## Run locally

```bash
cd Workora-Microservices
mvn test
```

To start Docker infrastructure and all Spring Boot applications on Windows,
run:

```powershell
.\start-all.ps1
```

The script clean-builds every Maven module, rebuilds the application Docker
images without cache, and recreates all infrastructure and application
containers. Run it again after code changes to rebuild and restart everything.
Set `JAVA_HOME` before running it if Java is not installed at the default path.

On Linux or macOS, start the project in three steps:

```bash
cd Workora-Microservices
chmod +x start-all-db.sh start-keycloak.sh start-all-service.sh remove-all-service.sh remove-all-workora.sh
./start-all-db.sh
./start-keycloak.sh
./start-all-service.sh
```

The scripts can be run again safely. `start-all-db.sh` starts the PostgreSQL
containers, `start-keycloak.sh` starts Keycloak, and `start-all-service.sh`
checks the application containers and source fingerprint before starting.
It rebuilds the application images only when a tracked source or build file
has changed, or when the saved state is missing.

After starting each service, the script checks its Actuator health endpoint.
If a service is not ready after two minutes, it prints the container status and
recent logs, runs Docker Compose for that service again, and retries once.
If it still fails, the script exits with the service logs so the startup error
is visible.

To automatically detect changes under `src/`, Maven files, the Dockerfile, or
the Compose file and rebuild/restart the application containers:

```bash
./start-all-service.sh --watch
```

The watcher uses polling and checks for changes every two seconds. Press
`Ctrl+C` to stop it.

To remove only the application service containers and images, run:

```bash
./remove-all-service.sh
```

The database containers, Keycloak, shared Docker network, and their data are not
removed.

To completely reset the Workora Docker Compose project, including all
containers, images, volumes, networks, and orphan containers, run:

```bash
./remove-all-workora.sh
```

This permanently deletes the database volumes.

## Start infrastructure with Docker

The [docker-compose.yml](./docker-compose.yml) file defines Keycloak, one
PostgreSQL container per database-owning service, and all Spring Boot
application containers. The startup scripts above start these groups separately.

The startup scripts use the Compose project name
`workora-microservices-2` and the shared Docker network
`workora-microservices_default`.

Alternatively, to start the same groups directly with Compose, use:

```bash
docker compose --project-name workora-microservices-2 up -d identity-db organization-db project-db work-db bug-db recruitment-db notification-db payment-db
docker compose --project-name workora-microservices-2 up -d keycloak
docker compose --project-name workora-microservices-2 build
docker compose --project-name workora-microservices-2 up -d
```

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

Keycloak is the identity provider and is available at:

```text
http://localhost:8180
```

The local realm is `workora`, imported from
[keycloak/realm-export.json](./keycloak/realm-export.json). The configured
public client is `workora-api`. Local administrator credentials are
`admin` / `admin`; change them before sharing or deploying this environment.

The compatibility endpoints under `identity-service` delegate registration,
login, refresh, logout, and password-reset email actions to Keycloak. The
service stores only the local Workora profile and application data.

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

With PowerShell, create the authorization header explicitly after obtaining a
Keycloak token:

```powershell
$authHeaders = @{
  Authorization = "Bearer $($keycloak.access_token)"
}
```

Use `$authHeaders` on protected requests. `Invoke-RestMethod` does not send a
token automatically just because it is stored in `$keycloak`.

## Authentication manual TC reference

Use the gateway base URL:

```text
http://localhost:8080/api/identity
```

Never record real passwords, OTPs, access tokens, or refresh tokens. Replace
them with placeholders in test evidence.

| TC | Request | Expected result |
| --- | --- | --- |
| AUTH-001 Register | `POST /api/v1/auth/register` with email, password, first name, and last name | HTTP 200, verification email arrives, no password/OTP in response |
| AUTH-002 Verify email | `POST /api/v1/auth/verify-email` with email and `<otp-from-mailbox>` | HTTP 200; wrong, expired, replayed, or malformed OTP is rejected |
| AUTH-003 Login | `POST /api/v1/auth/login` with verified test credentials | HTTP 200 with access/refresh tokens; no password returned |
| AUTH-004 Refresh | `POST /api/v1/auth/refresh` with `<current-refresh-token>` | HTTP 200 with rotated tokens; old token is rejected |
| AUTH-005 Logout | `POST /api/v1/auth/logout` with `<current-refresh-token>` | HTTP 200; token cannot be refreshed afterward |
| AUTH-006 Password reset request | `POST /api/v1/auth/password-reset-requests` with email | Generic HTTP 200 response for known and unknown emails; cooldown applies |
| AUTH-007 Password reset | `POST /api/v1/auth/password-resets` with email, OTP, and new password | HTTP 200; old password and reused OTP fail |
| AUTH-008 Protected profile | `GET /api/v1/users/me` with `Authorization: Bearer <keycloak-access-token>` | HTTP 200 for valid Keycloak token; HTTP 401 without/invalid token |
| AUTH-009 Profile update | `PATCH /api/v1/users/me` with bearer token and names | HTTP 200; only authenticated user's profile changes |
| AUTH-010 Health/routing | `GET /api/health` | HTTP 200 through gateway; Swagger loads |

Example registration request:

```json
{
  "email": "testuser@example.com",
  "password": "<test-password>",
  "firstName": "Test",
  "lastName": "User"
}
```

Example verification request:

```json
{
  "email": "testuser@example.com",
  "token": "<otp-from-test-mailbox>"
}
```

Example protected profile request:

```powershell
Invoke-RestMethod `
  -Uri "http://localhost:8080/api/identity/api/v1/users/me" `
  -Headers $authHeaders `
  -Method Get
```

If this returns HTTP 401, confirm that `$authHeaders` was created from
`$keycloak.access_token`, not from the legacy Identity `/auth/login` token.

The identity service validates Keycloak-issued JWTs using the
`KEYCLOAK_ISSUER_URI` setting. Keycloak is used for API token validation;
legacy custom identity endpoints remain available while clients migrate.

## Identity implementation status

The current Identity Service implements local registration, BCrypt password
storage, email and password-reset OTPs, custom login/refresh/logout, profile
read/update, and Keycloak JWT validation. Keycloak is the recommended
production token issuer. Keycloak's token, revocation, discovery, and JWKS
endpoints are used directly; the Identity Service does not reimplement those
OAuth endpoints.

Google federation, RabbitMQ identity events, complete profile attributes
(gender, date of birth, avatar, bio, and experience), account
disable/delete workflows, gateway rate limiting, refresh-token family
revocation, and production TLS/observability remain roadmap items. They are
not represented as implemented features.

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

For Identity protected endpoints, click the **Authorize** lock button in
Swagger, paste only the Keycloak access token, and select **Authorize**.
Swagger will automatically send `Authorization: Bearer <token>`. Do not paste
the `Bearer ` prefix if the dialog already supplies it.

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
