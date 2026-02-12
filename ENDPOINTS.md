# API Endpoints (Detailed)

Base URL (local): `http://localhost:8090`

Auth is JWT Bearer for protected endpoints:

```
Authorization: Bearer <token>
```

Role notes below reflect the backend `@PreAuthorize` rules.

## Auth

### POST /api/auth/login

Request:

```json
{
  "username": "admin",
  "password": "secret"
}
```

Response (200):

```json
{
  "token": "<jwt>",
  "username": "admin",
  "roles": ["ADMIN"],
  "employeeId": 123
}
```

Curl:

```bash
curl -s -X POST http://localhost:8090/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{"username":"admin","password":"secret"}'
```

### POST /api/auth/register

Creates a user, optionally linked to an employee.

Request:

```json
{
  "username": "newuser",
  "password": "secret",
  "roles": ["EMPLOYEE"],
  "employeeId": 123
}
```

Responses:
- 201 Created: `{ id, username, roles, message }`
- 409 Conflict if username already exists

## Employees

Base: `/api/emp`

### GET /api/emp

Lists all employees.

Curl:

```bash
curl -s http://localhost:8090/api/emp
```

### POST /api/emp

Role: `ADMIN` or `HR`

Request:

```json
{
  "firstName": "Ada",
  "lastName": "Lovelace",
  "email": "ada@example.com"
}
```

Curl:

```bash
TOKEN="<paste-jwt>"
curl -s -X POST http://localhost:8090/api/emp \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d '{"firstName":"Ada","lastName":"Lovelace","email":"ada@example.com"}'
```

### GET /api/emp/{id}

Role: `MANAGER` or `HR` or the employee themselves (`{id} == principal.employee.id`).

### PUT /api/emp/{id}

Role: `MANAGER` or `HR` or the employee themselves.

### DELETE /api/emp/{id}

Role: `ADMIN` or `HR`

### GET /api/emp/email-id/{mail}

Fetch by email.

## Employee Documents

### POST /api/emp/{id}/documents

Upload a document for an employee.

Role: `ADMIN` or `HR` or the employee themselves.

Content-Type: `multipart/form-data`

Curl:

```bash
TOKEN="<paste-jwt>"
EMP_ID=123
curl -s -X POST "http://localhost:8090/api/emp/${EMP_ID}/documents" \
  -H "Authorization: Bearer $TOKEN" \
  -F "file=@./somefile.pdf"
```

### GET /api/emp/{id}/documents/{docId}

Downloads the stored file bytes with `Content-Disposition: attachment`.

Role: `ADMIN` or `HR` or the employee themselves.

## Attendance

Base: `/api/attendance`

### POST /api/attendance/me/check-in

Authenticated user must be linked to an employee.

Curl:

```bash
TOKEN="<paste-jwt>"
curl -s -X POST http://localhost:8090/api/attendance/me/check-in \
  -H "Authorization: Bearer $TOKEN"
```

### POST /api/attendance/me/check-out

Same rules as check-in.

### GET /api/attendance/me/monthly?year=YYYY&month=M

Returns attendance rows for the authenticated employee.

### GET /api/attendance/report/{employeeId}/monthly?year=YYYY&month=M

Role: `ADMIN` or `HR`

### GET /api/attendance/report/{employeeId}/totals?year=YYYY&month=M

Role: `ADMIN` or `HR`

## Leave

Base: `/api/leave`

### POST /api/leave/me/apply

Authenticated user must be linked to an employee.

Request:

```json
{
  "startDate": "2026-02-10",
  "endDate": "2026-02-12",
  "type": "ANNUAL",
  "note": "trip"
}
```

### GET /api/leave/me

List own leave requests.

### GET /api/leave/pending

Role: `MANAGER` or `HR`

### PUT /api/leave/{id}/decide

Role: `MANAGER` or `HR`

Request:

```json
{
  "status": "APPROVED",
  "note": "ok"
}
```

## Payroll

Base: `/api/payroll`

### POST /api/payroll/employee/{employeeId}/generate

Role: `ADMIN` or `HR`

Request (example):

```json
{
  "year": 2026,
  "month": 2,
  "baseSalary": 1000,
  "allowances": 200,
  "deductions": 50
}
```

### POST /api/payroll/generate/{employeeId}/{period}

Role: `ADMIN` or `HR`

`period` format: `yyyy-MM` (example: `2026-02`).

### GET /api/payroll/{id}/pdf

Role: `ADMIN` or `HR`

Downloads the generated PDF for a payroll record.

## Dashboard

### GET /api/dashboard

Role: authenticated.

## Audit

### GET /api/audit

Role: `ADMIN`

Supports pagination via Spring Data `Pageable` query params (e.g. `?page=0&size=50&sort=timestamp,desc`).
