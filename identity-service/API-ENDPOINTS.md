# Workora API Endpoint Map

This document translates the backend plan into a concrete REST endpoint blueprint. The design follows the architecture described in Workora-Backend-Development-Plan.md and the Spring Boot best-practice guidance in SKILL-BACKEND.md.

## 1. Shared conventions

- API version prefix: `/api/v1`
- Resource IDs are UUIDs/ULIDs, never sequential DB IDs
- Standard responses: `200 OK`, `201 Created`, `204 No Content`, `400 Bad Request`, `401 Unauthorized`, `403 Forbidden`, `404 Not Found`, `409 Conflict`, `422 Unprocessable Entity`
- Use RFC 9457-style problem details for errors
- Use pagination: `?page=0&size=20&sort=createdAt,desc`
- Use `PATCH` for partial updates
- Require `Authorization: Bearer <token>` on protected resources
- Use `X-Correlation-ID` across requests and downstream calls

## 2. Gateway routing

The API gateway exposes public routes and forwards to the owning microservice.

- `GET /health/live`
- `GET /health/ready`
- `GET /api/v1/companies/**` -> organization-service
- `GET /api/v1/projects/**` -> project-service
- `GET /api/v1/tasks/**` -> work-service
- `GET /api/v1/bug-reports/**` -> bug-service
- `GET /api/v1/recruitment-posts/**` -> recruitment-service
- `GET /api/v1/notifications/**` -> notification-service
- `GET /api/v1/payments/**` -> payment-service

## 3. Auth service

Primary database: `auth_db`

### OAuth and identity metadata

- `POST /oauth/token`
  - Exchange authorization code, refresh token, or client credentials
- `POST /oauth/revoke`
  - Revoke token family/session
- `GET /.well-known/openid-configuration`
- `GET /.well-known/jwks.json`

### Authn / account lifecycle

- `POST /api/v1/auth/register`
- `POST /api/v1/auth/verify-email`
- `POST /api/v1/auth/password-reset-requests`
- `POST /api/v1/auth/password-resets`
- `POST /api/v1/auth/login`
- `POST /api/v1/auth/logout`
- `POST /api/v1/auth/refresh-token`

### User profile and me endpoints

- `GET /api/v1/users/me`
- `PATCH /api/v1/users/me`
- `GET /api/v1/users/{userId}`
- `PATCH /api/v1/users/{userId}` (admin only)
- `DELETE /api/v1/users/{userId}` (soft delete / disable)

### Payment profile endpoints

- `GET /api/v1/users/me/payment-profiles/bakong`
- `POST /api/v1/users/me/payment-profiles/bakong`
- `PATCH /api/v1/users/me/payment-profiles/bakong`
- `DELETE /api/v1/users/me/payment-profiles/bakong`
- `POST /api/v1/users/me/payment-profiles/bakong/qr-upload`

### OAuth / role scopes

- `openid`
- `profile`
- `email`
- `company:read`
- `company:write`
- `project:read`
- `project:write`
- `task:read`
- `task:write`
- `bug:read`
- `bug:write`
- `recruitment:read`
- `recruitment:write`
- `notification:read`
- `notification:write`
- `payment:read`
- `payment:write`

## 4. User service

Primary database: `user_db`

- `GET /api/v1/users`
  - list users with filtering by company/project role or status
- `GET /api/v1/users/{userId}/profile`
- `PATCH /api/v1/users/{userId}/profile`
- `GET /api/v1/users/{userId}/experience`
- `PATCH /api/v1/users/{userId}/experience`
- `GET /api/v1/users/{userId}/verification-status`
- `POST /api/v1/users/{userId}/disable`
- `POST /api/v1/users/{userId}/enable`
- `GET /api/v1/users/{userId}/activity`

## 5. Organization service

Primary database: `organization_db`

### Company endpoints

- `POST /api/v1/companies`
- `GET /api/v1/companies/{companyId}`
- `PATCH /api/v1/companies/{companyId}`
- `DELETE /api/v1/companies/{companyId}` (archive / soft delete)
- `GET /api/v1/companies`
- `GET /api/v1/companies/{companyId}/profile`
- `GET /api/v1/companies/{companyId}/posts`
- `GET /api/v1/companies/{companyId}/feedback`
- `GET /api/v1/companies/{companyId}/ratings`

### Department endpoints

- `POST /api/v1/companies/{companyId}/departments`
- `GET /api/v1/companies/{companyId}/departments`
- `GET /api/v1/departments/{departmentId}`
- `PATCH /api/v1/departments/{departmentId}`
- `DELETE /api/v1/departments/{departmentId}`
- `GET /api/v1/departments/{departmentId}/members`
- `POST /api/v1/departments/{departmentId}/members/{userId}`
- `DELETE /api/v1/departments/{departmentId}/members/{userId}`

### Company membership / role endpoints

- `POST /api/v1/companies/{companyId}/members/invitations`
- `GET /api/v1/companies/{companyId}/invitations`
- `POST /api/v1/companies/{companyId}/invitations/{invitationId}/accept`
- `POST /api/v1/companies/{companyId}/members/{userId}/roles`
- `PATCH /api/v1/companies/{companyId}/members/{userId}/roles`
- `DELETE /api/v1/companies/{companyId}/members/{userId}`
- `GET /api/v1/companies/{companyId}/members`

### Rating endpoints

- `POST /api/v1/companies/{companyId}/ratings`
- `GET /api/v1/companies/{companyId}/ratings`
- `GET /api/v1/users/{userId}/ratings`
- `PATCH /api/v1/ratings/{ratingId}`
- `POST /api/v1/ratings/{ratingId}/response`

## 6. Project service

Primary database: `project_db`

### Project endpoints

- `POST /api/v1/companies/{companyId}/projects`
- `GET /api/v1/companies/{companyId}/projects`
- `GET /api/v1/departments/{departmentId}/projects`
- `GET /api/v1/projects/{projectId}`
- `PATCH /api/v1/projects/{projectId}`
- `DELETE /api/v1/projects/{projectId}`
- `POST /api/v1/projects/{projectId}/archive`
- `GET /api/v1/projects/{projectId}/members`
- `POST /api/v1/projects/{projectId}/managers/{userId}`

### Phase endpoints

- `POST /api/v1/projects/{projectId}/phases`
- `GET /api/v1/projects/{projectId}/phases`
- `GET /api/v1/phases/{phaseId}`
- `PATCH /api/v1/phases/{phaseId}`
- `DELETE /api/v1/phases/{phaseId}`
- `POST /api/v1/phases/{phaseId}/publish`
- `POST /api/v1/phases/{phaseId}/hide`
- `POST /api/v1/phases/{phaseId}/close`
- `GET /api/v1/public/phases`
- `GET /api/v1/public/phases/{phaseId}`
- `GET /api/v1/phases/{phaseId}/members`
- `POST /api/v1/phases/{phaseId}/members/{userId}`
- `DELETE /api/v1/phases/{phaseId}/members/{userId}`

### Bug-Fix Job endpoints

- `POST /api/v1/phases/{phaseId}/bug-fix-jobs`
- `GET /api/v1/phases/{phaseId}/bug-fix-jobs`
- `GET /api/v1/bug-fix-jobs/{jobId}`
- `PATCH /api/v1/bug-fix-jobs/{jobId}`
- `POST /api/v1/bug-fix-jobs/{jobId}/publish`
- `POST /api/v1/bug-fix-jobs/{jobId}/close`
- `POST /api/v1/bug-fix-jobs/{jobId}/fund`

## 7. Work service

Primary database: `task_db`

### Task CRUD and assignment

- `POST /api/v1/phases/{phaseId}/tasks`
- `GET /api/v1/phases/{phaseId}/tasks`
- `GET /api/v1/tasks/{taskId}`
- `PATCH /api/v1/tasks/{taskId}`
- `DELETE /api/v1/tasks/{taskId}`
- `GET /api/v1/tasks/{taskId}/assignees`
- `POST /api/v1/tasks/{taskId}/assignees`
- `DELETE /api/v1/tasks/{taskId}/assignees/{userId}`
- `PATCH /api/v1/tasks/{taskId}/status`

### Task submissions and attachments

- `POST /api/v1/tasks/{taskId}/submissions`
- `GET /api/v1/tasks/{taskId}/submissions`
- `GET /api/v1/tasks/{taskId}/submissions/{submissionId}`
- `GET /api/v1/tasks/{taskId}/attachments`
- `POST /api/v1/tasks/{taskId}/attachments`
- `DELETE /api/v1/tasks/{taskId}/attachments/{attachmentId}`

### Workspace and query views

- `GET /api/v1/workspace`
- `GET /api/v1/dashboard`
- `GET /api/v1/history`

## 8. Bug service

Primary database: `bug_db`

### Bug report lifecycle

- `POST /api/v1/phases/{phaseId}/bug-reports`
- `POST /api/v1/tasks/{taskId}/bug-reports`
- `GET /api/v1/bug-reports/{reportId}`
- `GET /api/v1/bug-reports`
- `PATCH /api/v1/bug-reports/{reportId}`
- `POST /api/v1/bug-reports/{reportId}/triage`
- `POST /api/v1/bug-reports/{reportId}/accept`
- `POST /api/v1/bug-reports/{reportId}/reject`
- `POST /api/v1/bug-reports/{reportId}/resolve`
- `POST /api/v1/bug-reports/{reportId}/duplicate`
- `POST /api/v1/bug-reports/{reportId}/close`

### Evidence and attachments

- `POST /api/v1/bug-reports/{reportId}/evidence`
- `GET /api/v1/bug-reports/{reportId}/evidence`
- `DELETE /api/v1/bug-reports/{reportId}/evidence/{evidenceId}`

## 9. Recruitment service

Primary database: `recruitment_db`

### Recruitment posts

- `POST /api/v1/recruitment-posts`
- `GET /api/v1/recruitment-posts`
- `GET /api/v1/recruitment-posts/{postId}`
- `PATCH /api/v1/recruitment-posts/{postId}`
- `DELETE /api/v1/recruitment-posts/{postId}`
- `POST /api/v1/recruitment-posts/{postId}/close`

### Applications

- `POST /api/v1/recruitment-posts/{postId}/applications`
- `GET /api/v1/recruitment-posts/{postId}/applications`
- `GET /api/v1/applications/{applicationId}`
- `PATCH /api/v1/applications/{applicationId}`
- `POST /api/v1/applications/{applicationId}/manager-review`
- `POST /api/v1/applications/{applicationId}/owner-approval`
- `POST /api/v1/applications/{applicationId}/withdraw`

## 10. Notification service

Primary database: `notification_db`

- `GET /api/v1/notifications`
- `GET /api/v1/notifications/{notificationId}`
- `PATCH /api/v1/notifications/{notificationId}/read`
- `PATCH /api/v1/notifications/{notificationId}/archive`
- `DELETE /api/v1/notifications/{notificationId}`
- `GET /api/v1/notifications/preferences`
- `PATCH /api/v1/notifications/preferences`
- `POST /api/v1/notifications/test-email`

## 11. Payment service

Primary database: `payment_db`

### Fee and payment orchestration

- `POST /api/v1/bug-fix-jobs/{jobId}/fee-approval`
- `GET /api/v1/fee-payments/{paymentId}`
- `GET /api/v1/fee-payments/{paymentId}/qr`
- `POST /api/v1/fee-payments/{paymentId}/verify`
- `POST /api/v1/fee-payments/{paymentId}/confirm-receipt`
- `POST /api/v1/fee-payments/{paymentId}/dispute`
- `GET /api/v1/disputes/{disputeId}`

### Bakong profile endpoints

- `GET /api/v1/users/me/payment-profiles/bakong`
- `POST /api/v1/users/me/payment-profiles/bakong`
- `PATCH /api/v1/users/me/payment-profiles/bakong`
- `DELETE /api/v1/users/me/payment-profiles/bakong`
- `POST /api/v1/users/me/payment-profiles/bakong/qr-upload`

## 12. Feed and query services

These are read-model endpoints built from event projections.

- `GET /api/v1/feed`
- `GET /api/v1/feed/opportunities`
- `GET /api/v1/feed/companies`
- `GET /api/v1/dashboard`
- `GET /api/v1/dashboard/company/{companyId}`
- `GET /api/v1/dashboard/project/{projectId}`
- `GET /api/v1/history`
- `GET /api/v1/workspace`

## 13. Recommended package layout per service

```text
service-name/
  src/main/java/com/workora/service/
    config/
    controller/
    dto/
      request/
      response/
    entity/
    exception/
    mapper/
    repository/
    service/
    client/
    event/
    util/
```

## 14. Key implementation notes

- Each service owns its own database and publishes events to RabbitMQ/Kafka.
- Cross-service access should use IDs and events, not direct DB joins.
- The API gateway validates tokens and routes requests; domain services enforce resource ownership.
- Audit state transitions for all lifecycle changes: create, archive, approve, reject, assign, status change, verify, close.
- MinIO is used for all file/object storage; services store metadata and object keys only.


