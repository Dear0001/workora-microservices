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

## Run locally

```bash
cd Workora-Microservices
mvn test
```

## Start the database stack with Docker

The [docker-compose.yml](./docker-compose.yml) file starts one PostgreSQL
container and persistent volume for each database-owning service:

```bash
cd Workora-Microservices
docker compose up -d
```

Check database status with:

```bash
docker compose ps
```

The databases are exposed on host ports `5433` through `5440`; inside the
Compose network each database listens on its normal PostgreSQL port `5432`.
Each Spring Boot service is configured to use its matching database by
default. Start a service after the databases are healthy, for example:

```bash
mvn spring-boot:run -pl identity-service
```

The datasource settings support `DB_URL`, `DB_USERNAME`, and `DB_PASSWORD`
environment variables for deployment-specific connection values.

## Swagger UI

After starting each Spring Boot service, open its Swagger UI at
`http://localhost:<port>/swagger-ui.html`:

| Service | URL |
| --- | --- |
| API gateway | http://localhost:8080/swagger-ui.html |
| Identity | http://localhost:8101/swagger-ui.html |
| Organization | http://localhost:8102/swagger-ui.html |
| Project | http://localhost:8103/swagger-ui.html |
| Work | http://localhost:8104/swagger-ui.html |
| Bug | http://localhost:8105/swagger-ui.html |
| Recruitment | http://localhost:8106/swagger-ui.html |
| Notification | http://localhost:8107/swagger-ui.html |
| Payment | http://localhost:8108/swagger-ui.html |

The gateway exposes every service documentation through the same friendly
prefix format:

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
