# Identity Service

The identity service owns Workora user accounts, credentials, email
verification, JWT sessions, password reset tokens, roles, and the current
user profile.

## Responsibilities

- Register a user with a unique, normalized email address.
- Hash passwords with BCrypt before persistence.
- Require email verification before login.
- Issue short-lived access tokens and longer-lived refresh tokens.
- Validate refresh tokens and rotate them on refresh.
- Invalidate the stored refresh-token hash during logout.
- Provide the authenticated user's profile.
- Allow authenticated users to update their first and last names.
- Expose health checks for local development and service monitoring.

## Running

Start the identity database from the repository root:

```bash
docker compose up -d identity-db
```

Start the service with Java 17:

```bash
mvn spring-boot:run -pl identity-service
```

Default service port:

```text
http://localhost:8101
```

The default datasource is the PostgreSQL container configured in the root
`docker-compose.yml`:

```text
Database: identity_db
Username: workora
Password: workora123
Host port: 5433
```

The connection can be overridden with `DB_URL`, `DB_USERNAME`, and
`DB_PASSWORD`.

## Access through the API gateway

Use the gateway prefix for client requests:

```text
http://localhost:8080/api/identity
```

Swagger UI:

```text
http://localhost:8080/identify/swagger-ui.html
```

OpenAPI document:

```text
http://localhost:8080/identify/v3/api-docs
```

The gateway removes `/api/identity` before forwarding the request. For
example:

```text
Client:  POST /api/identity/api/v1/auth/login
Service: POST /api/v1/auth/login
```

## Endpoints

All paths below are relative to the gateway base URL
`http://localhost:8080/api/identity`.

| Method | Path | Authentication | Purpose |
| --- | --- | --- | --- |
| GET | `/api/health` | Public | Service health response |
| POST | `/api/v1/auth/register` | Public | Create an unverified user |
| POST | `/api/v1/auth/verify-email` | Public | Verify an email token |
| POST | `/api/v1/auth/login` | Public | Authenticate and issue JWTs |
| POST | `/api/v1/auth/refresh` | Public | Rotate a refresh session |
| POST | `/api/v1/auth/logout` | Public | Clear the stored refresh session |
| POST | `/api/v1/auth/password-reset-requests` | Public | Generate a reset token |
| POST | `/api/v1/auth/password-resets` | Public | Set a new password |
| GET | `/api/v1/users/me` | Bearer JWT | Read the current profile |
| PATCH | `/api/v1/users/me` | Bearer JWT | Update first and last names |

Actuator health is also available at:

```text
http://localhost:8080/identify/actuator/health
```

## Business flows

### 1. Registration and email verification

1. The request email is trimmed and converted to lowercase.
2. The service rejects an existing email.
3. The password is BCrypt-hashed; plaintext passwords are not stored.
4. A six-digit email-verification OTP is generated and stored for 10 minutes.
5. The user is created with role `USER` and `emailVerified = false`.
6. The OTP is sent to the registered email address; it is not returned by the API.
7. The token is submitted to `/api/v1/auth/verify-email`.
8. A valid token marks the account verified and is then cleared.

Example registration:

```http
POST /api/identity/api/v1/auth/register
Content-Type: application/json
```

```json
{
  "email": "user@example.com",
  "password": "password123",
  "firstName": "Dalen",
  "lastName": "Phea"
}
```

Example verification:

```json
{
  "email": "user@example.com",
  "token": "six-digit-code-from-email"
}
```

Passwords must contain at least eight characters. Email and required fields
are validated before the business operation runs.

### 2. Login and JWT sessions

Login looks up the normalized email and verifies the BCrypt password. Login
fails when credentials are invalid or the email has not been verified.

On success, the service returns:

- An access token with type `access`.
- A refresh token with type `refresh`.
- User identity and role information.

The default lifetimes are:

- Access token: 15 minutes.
- Refresh token: 7 days.

The signing secret and lifetimes are configured in `application.yml`. Use a
long random secret outside local development.

### 3. Refresh and logout

Refresh validates all of the following:

- The refresh JWT signature.
- The `refresh` token type.
- The token expiration time.
- The user identified by the token subject.
- The BCrypt hash stored for the user's current refresh token.

Successful refresh generates a new access token and refresh token and replaces
the stored refresh-token hash. The old refresh token is therefore no longer
the current session token.

Logout validates the refresh token and clears the stored refresh-token hash.
Subsequent refresh attempts with that token fail.

### 4. Password reset

The reset request looks up the email and stores a generated UUID reset token.
The reset endpoint accepts that token and a new password, hashes the new
password, and clears the token after success.

The service sends a six-digit reset OTP by email. The OTP is BCrypt-hashed in
the database, expires after 10 minutes, and is cleared after a successful
password change. The reset request must include the email address associated
with the code.

## Gmail SMTP configuration

Use a Gmail app password, not your normal Google password. Configure it
through environment variables and never commit it:

```powershell
$env:MAIL_USERNAME="your-gmail-address@gmail.com"
$env:MAIL_PASSWORD="your-16-character-app-password"
```

The default SMTP host is `smtp.gmail.com` on port `587` with STARTTLS.
Because the app password was shared outside the application configuration,
revoke it in Google Account security and create a new one before use.

### 5. Current profile

The authenticated user's email is taken from the access-token subject rather
than from request input. The service returns:

- User ID
- Email
- First name
- Last name
- Role
- Email verification status

Profile updates only change the supplied first and last names. Both fields
must be non-blank when sent through the current request DTO.

## Data model

The JPA entity `User` is stored in the `users` table with:

| Field | Meaning |
| --- | --- |
| `id` | UUID primary key |
| `email` | Unique normalized login identifier |
| `password` | BCrypt password hash |
| `firstName`, `lastName` | Profile data |
| `role` | Enum persisted as a string; defaults to `USER` |
| `emailVerified` | Controls account availability |
| `emailVerificationToken` | BCrypt hash of the one-time email verification OTP |
| `passwordResetToken` | BCrypt hash of the password reset OTP |
| `refreshTokenHash` | BCrypt hash of the current refresh token |

The service implements `UserDetails`. Its username is the email address, its
authority is `ROLE_<role>`, and an account is enabled only after email
verification.

## Security

- Stateless Spring Security sessions.
- BCrypt password hashing.
- JWT access-token authentication through the `Authorization` header.
- Public authentication, health, error, and Swagger endpoints.
- Protected profile endpoints require:

```http
Authorization: Bearer <access-token>
```

Invalid or missing credentials return the service's JSON authentication
response:

```json
{
  "success": false,
  "message": "Authentication required"
}
```

## Error behavior

Business rule failures return HTTP `400`, including:

- Email already registered
- Invalid credentials
- Email not verified
- Invalid verification token
- Invalid refresh token
- Invalid password reset token
- User not found

Unexpected failures return HTTP `500` with the standard API response shape.
