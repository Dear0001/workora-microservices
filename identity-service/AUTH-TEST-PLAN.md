# Identity Service Authentication Test Plan

This plan matches the authentication code currently present in the repository.
Keycloak is the single source of truth for email verification and password
reset. These flows use Keycloak email action links, not a second Workora OTP.

## Test scope

Use the gateway base URL:

```text
http://localhost:8080/api/identity
```

The direct service URL (`http://localhost:8101`) is useful for isolating the
Identity Service. Keycloak must be running at `http://localhost:8180` with the
`workora` realm and `workora-api` client imported.

## Implemented-flow test cases

| ID | Scenario | Expected result |
| --- | --- | --- |
| AUTH-001 | `GET /api/health` | HTTP 200 with `identity-service` and `UP` |
| AUTH-002 | Register a new valid user | Keycloak user and local profile are created; response contains user ID/email/message and no password |
| AUTH-003 | Register duplicate email, including case/whitespace variants | Request is rejected; no duplicate local profile is created |
| AUTH-004 | Register invalid input | Bean validation returns HTTP 400 before the operation |
| AUTH-005 | Login with valid Keycloak credentials | HTTP 200 with access token, refresh token, local user ID, email, names, and role |
| AUTH-006 | Login with invalid credentials | Keycloak rejects the request; no token is returned |
| AUTH-007 | Refresh with a valid Keycloak refresh token | New Keycloak token response is mapped with the local profile |
| AUTH-008 | Refresh with malformed, expired, or revoked token | Request is rejected; no profile or token response is returned |
| AUTH-009 | Logout with a valid refresh token | Keycloak logout succeeds; subsequent use of that refresh token is rejected by Keycloak |
| AUTH-010 | Request password reset for an existing email | Keycloak sends an `UPDATE_PASSWORD` action email when SMTP is configured |
| AUTH-011 | Request password reset for an unknown email | The Keycloak lookup failure is surfaced; account-enumeration behavior must be reviewed before production |
| AUTH-012 | Verify email or reset password through compatibility endpoint | Endpoint returns the documented Keycloak-link message; completion occurs in Keycloak, not in this service |
| AUTH-013 | `GET /api/v1/users/me` without a bearer token | HTTP 401 with the standard authentication response |
| AUTH-014 | `GET /api/v1/users/me` with a valid Keycloak token | HTTP 200 for the token email only |
| AUTH-015 | Profile request with expired, altered, wrong-issuer, or wrong-realm token | HTTP 401; no profile data |
| AUTH-016 | `PATCH /api/v1/users/me` with valid names | HTTP 200; only the authenticated local profile changes |
| AUTH-017 | Profile update with blank names | HTTP 400; profile is unchanged |

## Security checks required before release

- Replace all local default credentials and secrets with deployment secrets.
- Confirm the accepted issuer, JWK set, client/authorized-party, and role claims
  match the intended Keycloak realm.
- Confirm registration cannot create a local profile when Keycloak creation or
  required-action setup fails.
- Confirm SMTP errors are visible and do not produce a success-shaped response.
- Check unknown-email password-reset behavior for account enumeration.
- Confirm no Workora verification/reset OTP is generated, stored, emailed, or
  accepted; Keycloak action links remain the only verification mechanism.
- Verify access tokens, refresh tokens, admin tokens, passwords, and SMTP
  credentials never appear in responses or logs.
- Test HTTPS, gateway rate limiting, CORS, correlation IDs, and centralized
  security-event logging before production; these controls are not implemented
  in the current gateway.
- Run dependency/SCA scanning and test against disposable databases and test
  Keycloak accounts only.

## Not implemented and therefore not testable yet

Google federation, Workora-owned OAuth token/revocation endpoints, refresh-token
families, optional MFA/high-risk-action OTP, user disable/delete lifecycle,
payment profiles, service-to-service authorization, and downstream domain-service
APIs require implementation before dedicated test cases can be marked complete.

## Manual smoke test

```powershell
Invoke-RestMethod http://localhost:8080/api/identity/api/health

$body = @{
  email = "testuser@example.com"
  password = "TestPassword123!"
  firstName = "Test"
  lastName = "User"
} | ConvertTo-Json

Invoke-RestMethod `
  -Uri "http://localhost:8080/api/identity/api/v1/auth/register" `
  -Method Post -ContentType "application/json" -Body $body
```

Create or verify the test user through the Keycloak realm, then obtain a
Keycloak bearer token and call:

```powershell
Invoke-RestMethod `
  -Uri "http://localhost:8080/api/identity/api/v1/users/me" `
  -Headers @{ Authorization = "Bearer <redacted-token>" }
```

Never save live tokens, passwords, admin credentials, or email credentials in
test evidence.
