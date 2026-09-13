# api-gateway

Public API entry point for Workora microservices.

## Implementation status

The gateway provides Spring Cloud Gateway routing, Keycloak JWT validation,
CORS, correlation-ID propagation, an in-memory request rate limit, and Swagger
aggregation. Rate limiting is local to one gateway instance; use a shared
Redis-backed limiter before running multiple gateway instances.

## Routes

- /api/identity/** → identity-service
- /api/organization/** → organization-service
- /api/project/** → project-service
- /api/work/** → work-service
- /api/bug/** → bug-service
- /api/recruitment/** → recruitment-service
- /api/notification/** → notification-service
- /api/payment/** → payment-service

The `/api/{service}/**` routes strip the first two path segments before
forwarding. The `/identify`, `/organization`, `/project`, `/work`, `/bug`,
`/recruitment`, `/notification`, and `/payment` routes are also configured for
service OpenAPI/Swagger access.

## Security behavior

- Keycloak bearer tokens are validated at the gateway using the configured
  issuer and JWK set. Authentication endpoints, service health endpoints, and
  OpenAPI resources are public; API business routes require authentication.
- The gateway converts the Keycloak `roles` claim to `ROLE_*` authorities.
  Resource-level company membership authorization remains the responsibility
  of each domain service.
- `X-Correlation-ID` is preserved when supplied or generated when absent,
  forwarded downstream, and returned in the response.
- API requests are limited to 120 requests per IP per 60-second window by
  default. Configure `GATEWAY_RATE_LIMIT_REQUESTS` and
  `GATEWAY_RATE_LIMIT_WINDOW_SECONDS`.
- CORS allows `http://localhost:3000` by default. Configure
  `GATEWAY_CORS_ALLOWED_ORIGINS` with a comma-separated allowlist.
- Unauthorized and forbidden requests return JSON responses with HTTP 401 and
  403 respectively.
