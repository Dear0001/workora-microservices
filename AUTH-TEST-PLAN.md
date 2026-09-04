# Authentication Service Test Plan

This plan covers the Identity Service authentication API and the controls
needed to detect or prevent fraudulent activity.

## Test scope

Use the gateway base URL for end-to-end testing:

```text
http://localhost:8080/api/identity
```

The direct service base URL (`http://localhost:8101`) is useful for isolating
the Identity Service, but is not the recommended client path.

Required test data:

- A new Gmail test mailbox that can receive OTP messages.
- One verified user and one unverified user.
- A second user with a different email address.
- Valid and expired access/refresh tokens.
- A database snapshot or test database that can be reset between scenarios.

## Entry and exit criteria

### Entry

- PostgreSQL, Keycloak, Identity Service, and API Gateway are running.
- `MAIL_USERNAME` and `MAIL_PASSWORD` are configured with a Gmail app password.
- Database migrations/schema creation has completed.
- Test accounts and test mailbox are available.

### Exit

- All P0 and P1 cases pass.
- No password, OTP, refresh token, or SMTP credential appears in logs or API
  responses.
- Failed security cases are recorded as defects, not marked as passed because
  they are "expected limitations".
- No critical or high-severity fraud-control issue remains open.

## Endpoint test cases

| ID | Endpoint and scenario | Steps | Expected result | Priority |
| --- | --- | --- | --- | --- |
| AUTH-001 | `POST /api/v1/auth/register` valid registration | Submit a new valid email and an 8+ character password | HTTP 200; user ID and normalized email returned; no password or OTP returned; verification email arrives | P0 |
| AUTH-002 | Register duplicate email | Register the same email twice, including mixed case/whitespace | Second request is rejected; no second account is created | P0 |
| AUTH-003 | Register invalid input | Try blank/invalid email, blank password, and password shorter than 8 characters | HTTP 400 validation response; no database row or email side effect | P1 |
| AUTH-004 | Register SMTP failure | Stop/break SMTP and register a user | Failure is surfaced consistently; no silently successful "email sent" result; transaction behavior is documented | P1 |
| AUTH-005 | `POST /api/v1/auth/verify-email` valid OTP | Use the OTP delivered for the matching email | HTTP 200; account becomes verified; OTP and expiry are cleared | P0 |
| AUTH-006 | Verify invalid OTP | Use a wrong six-digit value | Request is rejected; account remains unverified; stored hash is unchanged | P0 |
| AUTH-007 | Verify expired/replayed OTP | Wait beyond 10 minutes, then submit; submit a successful OTP twice | Expired and replayed values are rejected | P0 |
| AUTH-008 | Verify malformed OTP | Submit letters, fewer/more than six digits, blank token, or another user's email | HTTP 400 or business error; no account state change | P1 |
| AUTH-009 | `POST /api/v1/auth/login` valid verified user | Submit correct credentials for a verified user | HTTP 200; access and refresh tokens returned; no password returned | P0 |
| AUTH-010 | Login invalid credentials | Try wrong password, unknown email, and wrong email casing | Login rejected; response does not reveal whether the email exists | P0 |
| AUTH-011 | Login unverified account | Use correct credentials before email verification | Login rejected with the configured unverified-account behavior | P0 |
| AUTH-012 | Login input abuse | Send blank fields, oversized values, SQL/script strings, and repeated attempts | Validation/rejection; no stack trace or sensitive data; rate limit activates when configured | P1 |
| AUTH-013 | `POST /api/v1/auth/refresh` valid token | Refresh once with the current refresh token | New access and refresh tokens returned; old refresh token no longer works | P0 |
| AUTH-014 | Refresh invalid token | Use expired, malformed, access-type, altered, or revoked token | HTTP 401/business rejection; no new token issued | P0 |
| AUTH-015 | Refresh token replay/concurrency | Send the same refresh token concurrently from two clients | Only one request succeeds, or the documented rotation policy prevents reuse | P0 |
| AUTH-016 | `POST /api/v1/auth/logout` valid token | Logout with the current refresh token, then refresh it | Logout succeeds; subsequent refresh is rejected | P0 |
| AUTH-017 | Logout invalid token | Send malformed, expired, access-type, or another user's token | Rejected without changing another account's session | P1 |
| AUTH-018 | `POST /api/v1/auth/password-reset-requests` known email | Request reset for a verified test account | OTP email arrives; response does not disclose unnecessary account data | P0 |
| AUTH-019 | Password reset unknown email | Request reset for an unknown email | Same generic response as a known email; no account enumeration | P0 |
| AUTH-020 | Reset OTP and password | Use the received OTP with a strong new password, then log in | Password changes; OTP clears; old password fails; new password works | P0 |
| AUTH-021 | Reset invalid/expired/replayed OTP | Use wrong, expired, and already-used OTPs | Reset rejected; password remains unchanged | P0 |
| AUTH-022 | Reset password validation | Try blank, short, reused, or oversized passwords | Validation/business policy enforced; no password stored in plaintext | P1 |
| AUTH-023 | `GET /api/v1/users/me` without token | Call through the gateway without `Authorization` | HTTP 401 with consistent JSON response | P0 |
| AUTH-024 | Get profile with valid Keycloak token | Call with a valid bearer token for an authorized user | HTTP 200; only the authenticated user's profile is returned | P0 |
| AUTH-025 | Get profile with expired/wrong-issuer token | Call with expired, altered, or wrong-realm token | HTTP 401; no profile data returned | P0 |
| AUTH-026 | `PATCH /api/v1/users/me` valid update | Update first and last name with a valid token | HTTP 200; only the current user's names change | P1 |
| AUTH-027 | Profile update authorization/input | Omit token; submit blank, oversized, HTML/script, and unexpected fields | Unauthorized/validation response; no cross-user modification or stored unsafe content | P1 |
| AUTH-028 | `GET /api/health` | Call the health endpoint through the gateway | HTTP 200 and no credentials required | P1 |

## Fraud and security scanning checklist

Run these checks during every release and during penetration testing:

| Control | Test/scan | Required behavior |
| --- | --- | --- |
| Credential stuffing | Repeat failed logins across many accounts and IPs | Rate limit, temporary backoff, alerting, or upstream WAF response |
| OTP brute force | Submit many incorrect OTPs for one email | Maximum five attempts per issued code, then a new-code requirement; never unlimited six-digit guesses |
| OTP abuse | Request many verification/reset emails | Reset requests have a one-minute per-account cooldown; add per-IP/global throttling at the gateway or WAF |
| Account enumeration | Compare timing/status/body for known and unknown emails | Same externally visible result and approximately similar timing |
| Token theft/replay | Reuse refresh tokens after rotation and logout | Replay rejected and security event recorded |
| JWT validation | Test bad signature, expired token, wrong issuer/audience, `none` algorithm, and wrong realm | Every invalid token rejected |
| Authorization bypass | Change IDs, email claims, paths, and roles | Users can access only their own profile and permitted operations |
| Injection | Fuzz email/name fields with SQL, JSON, HTML, CRLF, and log-injection payloads | Safely rejected or encoded; no query, header, or log injection |
| Mail header injection | Use newline characters in email/name fields | Input rejected; no arbitrary mail headers or recipients |
| Sensitive data exposure | Inspect API responses, logs, exceptions, database rows, and Actuator | No plaintext password/OTP/SMTP password/token leakage |
| Transport security | Test HTTP exposure outside local development | Production requires HTTPS and secure cookie/header handling |
| Configuration scan | Scan repository, history, `.env`, logs, and CI variables | No Gmail app password, JWT secret, or Keycloak admin secret committed |
| Dependency scan | Run the repository's approved dependency/SCA scanner | No unreviewed critical/high vulnerability |

## Current implementation notices

These are important findings to verify before production:

1. OTP verification now allows a maximum of five attempts per issued code.
   Password-reset requests also have a one-minute per-account cooldown.
   Per-IP/global throttling still belongs at the gateway or WAF.
2. Password-reset requests now return a generic response for unknown accounts
   and throttled requests, reducing account enumeration.
3. Login and OTP failures need centralized security-event logging and alerting.
   The service now logs OTP lockouts and reset throttling without logging
   credentials, tokens, or OTP values.
4. SMTP credentials must be supplied through deployment secrets. Never place
   them in `application.yml`, source control, screenshots, or chat.
5. The service currently contains legacy custom JWT authentication alongside
   Keycloak resource-server validation. The supported production flow must be
   selected and tested explicitly; accepting tokens from unintended issuers is
   a security defect.
6. Run the test suite against a disposable database. Do not use real customer
   accounts or production mailboxes.

## Recommended execution order

1. Health and connectivity: AUTH-028, then gateway routing.
2. Registration and verification: AUTH-001 through AUTH-008.
3. Login/session lifecycle: AUTH-009 through AUTH-017.
4. Password reset: AUTH-018 through AUTH-022.
5. Authorization/profile: AUTH-023 through AUTH-027.
6. Fraud/security scanning checklist.
7. Keycloak issuer, token, realm, and role validation.
8. Retest all failed P0/P1 cases and record evidence (request, response,
   timestamp, account, environment, and defect ID).

## Verification performed

The following checks were run after the security changes:

| Check | Result |
| --- | --- |
| Identity Maven test/context startup | Passed |
| Direct `GET http://localhost:8101/api/health` | Passed (HTTP 200) |
| Gateway Identity Swagger UI | Passed (HTTP 200) |
| Gateway Identity OpenAPI document | Passed (HTTP 200) |
| Anonymous `GET /api/identity/api/v1/users/me` | Passed security boundary (HTTP 401) |
| Unknown password-reset email response | Fixed in code; requires live endpoint test |
| OTP attempt limit and reset cooldown | Implemented in code; requires live functional test |

The full endpoint table still requires functional execution with test
accounts, real OTP delivery, and valid Keycloak tokens. Downstream Swagger
URLs return connection errors when their corresponding services are not
running.

## Test evidence record

Create one record for every executed case:

| Field | Value |
| --- | --- |
| Test case ID | `AUTH-___` |
| Date/time and timezone | |
| Environment/build/commit | |
| Base URL | |
| Test account identifier | Use a test identifier only |
| Request method/path | |
| Request body/headers | Redact password, OTP, and tokens |
| Expected result | |
| Actual status/response | Redact secrets |
| Mail/database/log evidence | |
| Result | Pass / Fail / Blocked |
| Defect ID and retest link | |

Do not store live OTPs, access tokens, refresh tokens, Gmail passwords, or
Keycloak administrator credentials in the evidence record.

## Manual test procedures

Run these procedures from PowerShell after starting the infrastructure and
applications with `.\start-all.ps1`. Use only test accounts and a test mailbox.

### 1. Health and Swagger

```powershell
Invoke-RestMethod http://localhost:8080/api/identity/api/health
Invoke-WebRequest http://localhost:8080/gateway/swagger-ui.html -UseBasicParsing
Invoke-WebRequest http://localhost:8080/identify/swagger-ui.html -UseBasicParsing
```

Expected: health returns HTTP 200 and both Swagger pages load.

### 2. Registration and email verification

```powershell
$register = @{
  email = "testuser@example.com"
  password = "TestPassword123!"
  firstName = "Test"
  lastName = "User"
} | ConvertTo-Json

Invoke-RestMethod `
  -Uri "http://localhost:8080/api/identity/api/v1/auth/register" `
  -Method Post -ContentType "application/json" -Body $register
```

Confirm that the verification email arrives and that the response contains no
password or OTP. Copy the six-digit OTP only into the next request:

```powershell
$verify = @{ email = "testuser@example.com"; token = "123456" } | ConvertTo-Json
Invoke-RestMethod `
  -Uri "http://localhost:8080/api/identity/api/v1/auth/verify-email" `
  -Method Post -ContentType "application/json" -Body $verify
```

Replace `123456` with the value from the test mailbox. Test a wrong OTP, a
replayed OTP, an expired OTP, another email address, and six-digit guesses
until the five-attempt limit is reached. Record the resulting status and
response, but never save the OTP in evidence.

### 3. Login, refresh, and logout

```powershell
$loginBody = @{
  email = "testuser@example.com"
  password = "TestPassword123!"
} | ConvertTo-Json

$login = Invoke-RestMethod `
  -Uri "http://localhost:8080/api/identity/api/v1/auth/login" `
  -Method Post -ContentType "application/json" -Body $loginBody

$accessToken = $login.data.accessToken
$refreshToken = $login.data.refreshToken
```

Test correct credentials, a wrong password, an unknown email, and an
unverified user. Do not print `$accessToken` or `$refreshToken`.

```powershell
$refreshBody = @{ refreshToken = $refreshToken } | ConvertTo-Json
$rotated = Invoke-RestMethod `
  -Uri "http://localhost:8080/api/identity/api/v1/auth/refresh" `
  -Method Post -ContentType "application/json" -Body $refreshBody

$logoutBody = @{ refreshToken = $rotated.data.refreshToken } | ConvertTo-Json
Invoke-RestMethod `
  -Uri "http://localhost:8080/api/identity/api/v1/auth/logout" `
  -Method Post -ContentType "application/json" -Body $logoutBody
```

Verify that the original refresh token is rejected after rotation and the
latest refresh token is rejected after logout.

### 4. Password reset

```powershell
$resetRequest = @{ email = "testuser@example.com" } | ConvertTo-Json
Invoke-RestMethod `
  -Uri "http://localhost:8080/api/identity/api/v1/auth/password-reset-requests" `
  -Method Post -ContentType "application/json" -Body $resetRequest
```

Confirm that the reset email arrives. Request a reset for an unknown email and
compare the response; both responses must be generic. Repeat a known-email
request within one minute and confirm throttling without account disclosure.

Use the mailbox OTP to reset the password:

```powershell
$reset = @{
  email = "testuser@example.com"
  token = "123456"
  newPassword = "NewPassword123!"
} | ConvertTo-Json

Invoke-RestMethod `
  -Uri "http://localhost:8080/api/identity/api/v1/auth/password-resets" `
  -Method Post -ContentType "application/json" -Body $reset
```

Verify that the old password fails, the new password works, the OTP cannot be
replayed, and five incorrect OTP attempts are blocked.

### 5. Keycloak protected profile

These values come from different places:

- `client_id`: fixed value `workora-api`, imported from
  `keycloak/realm-export.json`.
- `grant_type`: fixed value `password` for this local test flow.
- `username`: the username you create for a test user in the Keycloak
  `workora` realm.
- `password`: the password assigned to that Keycloak test user.

Create the test user:

1. Open `http://localhost:8180/admin/master/console/`.
2. Sign in with the local administrator `admin` / `admin`.
3. Select the `workora` realm from the realm menu.
4. Open **Users** and select **Create new user**.
5. Set a username such as `keycloak-test-user`, then save.
6. Open the user's **Credentials** tab, choose **Set password**, enter a
   temporary password, and turn **Temporary** off.
7. Ensure the user is enabled and has a verified email if required.

Use the username and password you created in the token request. Do not print
the response because it contains an access token:

```powershell
$keycloak = Invoke-RestMethod `
  -Uri "http://localhost:8180/realms/workora/protocol/openid-connect/token" `
  -Method Post -ContentType "application/x-www-form-urlencoded" `
  -Body @{
    client_id = "workora-api"
    grant_type = "password"
    username = "the-username-you-created"
    password = "the-password-you-created"
  }

$authHeaders = @{ Authorization = "Bearer $($keycloak.access_token)" }
```

Test the profile endpoints:

```powershell
Invoke-RestMethod `
  -Uri "http://localhost:8080/api/identity/api/v1/users/me" `
  -Headers $authHeaders

$profile = @{ firstName = "Updated"; lastName = "Tester" } | ConvertTo-Json
Invoke-RestMethod `
  -Uri "http://localhost:8080/api/identity/api/v1/users/me" `
  -Method Patch -Headers $authHeaders `
  -ContentType "application/json" -Body $profile
```

Verify that no token returns HTTP 401, a valid token returns HTTP 200, and
expired, altered, wrong-realm, or wrong-issuer tokens return HTTP 401.
