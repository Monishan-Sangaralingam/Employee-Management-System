# Demo Script (End-to-End)

This demo runs the full system locally using Docker Compose, then walks through:

1) `docker compose up --build`
2) Login as admin
3) Create employee
4) Register user linked to employee
5) Employee check-in/out
6) Apply leave and approve as manager
7) Generate payroll and download payslip
8) Upload an employee document
9) Open dashboard

## Prerequisites

- Docker Desktop running (Windows)
- Ports free: `5173` (frontend), `8090` (backend)
- Use **PowerShell + `curl.exe`** (PowerShell’s `curl` alias is *not* real curl)

## Service URLs (local)

- Frontend UI: http://localhost:5173
- Backend API: http://localhost:8090

## Step 1 — Start everything (Docker Compose)

1. In repo root, create `.env` (required by `docker-compose.yml`):

   ```env
   JWT_SECRET=change-me-to-a-long-random-secret
   MYSQL_ROOT_PASSWORD=change-me
   MYSQL_PASSWORD=change-me
   # optional:
   # MYSQL_USER=ems_user
   # MYSQL_DATABASE=employee
   ```

2. Start services:

   ```powershell
  # Foreground (matches the demo step exactly)
  docker compose up --build

  # Or run detached:
  # docker compose up -d --build
   docker compose ps
   ```

3. (Optional) Tail logs if needed:

   ```powershell
   docker compose logs -f ems-backend
   ```

---

## Shared variables (PowerShell)

Run these once in your PowerShell session:

```powershell
$API = "http://localhost:8090"
$UI  = "http://localhost:5173"

# Demo credentials
$ADMIN_USER = "admin@local"
$ADMIN_PASS = "secret"

$MANAGER_USER = "manager1"
$MANAGER_PASS = "secret"

$EMP_USER = "employee1"
$EMP_PASS = "secret"

function To-Json($obj) {
  $obj | ConvertTo-Json -Compress
}
```

---

## Step 2 — Login as admin

### 2A) Ensure an admin user exists (one-time bootstrap)

If you *already* have an admin user, skip to 2B.

Create an admin user via the open registration endpoint (you’ll get `201` or `409` if it already exists):

```powershell
$adminRegisterBody = To-Json @{
  username = $ADMIN_USER
  password = $ADMIN_PASS
  roles    = @('ADMIN','HR','MANAGER','EMPLOYEE')
}

curl.exe -i -X POST "$API/api/auth/register" `
  -H "Content-Type: application/json" `
  -d $adminRegisterBody
```

Note: This is intended for demo/dev. In a production system, user/role creation would typically be restricted.

### 2B) Login as admin and capture the JWT

```powershell
$adminLoginBody = To-Json @{
  username = $ADMIN_USER
  password = $ADMIN_PASS
}

$adminLoginJson = curl.exe -s -X POST "$API/api/auth/login" `
  -H "Content-Type: application/json" `
  -d $adminLoginBody

$ADMIN_TOKEN = (ConvertFrom-Json $adminLoginJson).token
$ADMIN_TOKEN
```

UI navigation:
- Open http://localhost:5173
- You should see the **Login** page.
- Enter username/password (`admin@local` / `secret`) and click **Login**.
- You should land on **Dashboard**.

---

## Step 3 — Create employee

For the manager approval flow later, create **two employees**: one Manager employee and one Regular employee.

### 3A) Create manager’s employee record

```powershell
$managerEmpBody = To-Json @{
  firstName = 'Manny'
  lastName  = 'Manager'
  email     = 'manny.manager@example.com'
}

$managerEmpJson = curl.exe -s -X POST "$API/api/emp" `
  -H "Authorization: Bearer $ADMIN_TOKEN" `
  -H "Content-Type: application/json" `
  -d $managerEmpBody

$MANAGER_EMP_ID = (ConvertFrom-Json $managerEmpJson).id
$MANAGER_EMP_ID
```

### 3B) Create regular employee record

```powershell
$employeeEmpBody = To-Json @{
  firstName = 'Eve'
  lastName  = 'Employee'
  email     = 'eve.employee@example.com'
}

$employeeEmpJson = curl.exe -s -X POST "$API/api/emp" `
  -H "Authorization: Bearer $ADMIN_TOKEN" `
  -H "Content-Type: application/json" `
  -d $employeeEmpBody

$EMP_ID = (ConvertFrom-Json $employeeEmpJson).id
$EMP_ID
```

UI navigation (admin):
- Click **Employees** in the top nav.
- Click **Add Employee**.
- Enter first name / last name / email.
- Click **Save**.

---

## Step 4 — Register user linked to employee

Register two users and link them to the employees you created.

### 4A) Register manager user linked to the manager employee

```powershell
$managerRegisterBody = To-Json @{
  username   = $MANAGER_USER
  password   = $MANAGER_PASS
  roles      = @('MANAGER')
  employeeId = $MANAGER_EMP_ID
}

curl.exe -s -X POST "$API/api/auth/register" `
  -H "Content-Type: application/json" `
  -d $managerRegisterBody
```

### 4B) Register employee user linked to the regular employee

```powershell
$employeeRegisterBody = To-Json @{
  username   = $EMP_USER
  password   = $EMP_PASS
  roles      = @('EMPLOYEE')
  employeeId = $EMP_ID
}

curl.exe -s -X POST "$API/api/auth/register" `
  -H "Content-Type: application/json" `
  -d $employeeRegisterBody
```

### 4C) Login as employee + manager and capture tokens

```powershell
$empLoginBody = To-Json @{
  username = $EMP_USER
  password = $EMP_PASS
}

$empLoginJson = curl.exe -s -X POST "$API/api/auth/login" `
  -H "Content-Type: application/json" `
  -d $empLoginBody
$EMP_TOKEN = (ConvertFrom-Json $empLoginJson).token

$mgrLoginBody = To-Json @{
  username = $MANAGER_USER
  password = $MANAGER_PASS
}

$mgrLoginJson = curl.exe -s -X POST "$API/api/auth/login" `
  -H "Content-Type: application/json" `
  -d $mgrLoginBody
$MANAGER_TOKEN = (ConvertFrom-Json $mgrLoginJson).token

$EMP_TOKEN
$MANAGER_TOKEN
```

UI navigation:
- Use the **Logout** button.
- Login as `employee1` / `secret`.
- Confirm the header shows: **Dashboard**, **Attendance**, **Leave** (and not Payroll).

---

## Step 5 — Employee check-in/out

Attendance UI page exists but is not implemented yet, so use the API.

### 5A) Check-in

```powershell
curl.exe -s -X POST "$API/api/attendance/me/check-in" `
  -H "Authorization: Bearer $EMP_TOKEN"
```

### 5B) Check-out

```powershell
curl.exe -s -X POST "$API/api/attendance/me/check-out" `
  -H "Authorization: Bearer $EMP_TOKEN"
```

(Optional) Fetch this month’s attendance rows:

```powershell
$year  = (Get-Date).Year
$month = (Get-Date).Month
curl.exe -s "$API/api/attendance/me/monthly?year=$year&month=$month" `
  -H "Authorization: Bearer $EMP_TOKEN"
```

UI navigation:
- Click **Attendance** in the top nav.
- Page currently shows: “UI not implemented yet.”

---

## Step 6 — Apply leave and approve as manager

Leave UI page exists but is not implemented yet, so use the API.

Tip: Leave types are: `ANNUAL`, `SICK`, `CASUAL`, `UNPAID`.

### 6A) Employee applies for leave

Pick dates (example uses today + tomorrow):

```powershell
$startDate = (Get-Date).ToString('yyyy-MM-dd')
$endDate   = (Get-Date).AddDays(1).ToString('yyyy-MM-dd')

$leaveApplyBody = To-Json @{
  startDate = $startDate
  endDate   = $endDate
  type      = 'UNPAID'
  note      = 'Demo leave request'
}

$leaveApplyJson = curl.exe -s -X POST "$API/api/leave/me/apply" `
  -H "Authorization: Bearer $EMP_TOKEN" `
  -H "Content-Type: application/json" `
  -d $leaveApplyBody

$LEAVE_ID = (ConvertFrom-Json $leaveApplyJson).id
$LEAVE_ID
```

### 6B) Manager views pending leaves

```powershell
curl.exe -s "$API/api/leave/pending" `
  -H "Authorization: Bearer $MANAGER_TOKEN"
```

### 6C) Manager approves the leave

```powershell
$leaveDecideBody = To-Json @{
  status = 'APPROVED'
  note   = 'Approved for demo'
}

curl.exe -s -X PUT "$API/api/leave/$LEAVE_ID/decide" `
  -H "Authorization: Bearer $MANAGER_TOKEN" `
  -H "Content-Type: application/json" `
  -d $leaveDecideBody
```

UI navigation:
- Click **Leave** in the top nav.
- Page currently shows: “UI not implemented yet.”

---

## Step 7 — Generate payroll and download payslip

Payroll is available in the UI for roles `ADMIN`/`HR`, and also via API.

### 7A) Generate payroll (API)

Use the `/api/payroll/generate/{employeeId}/{period}` endpoint (period format: `YYYY-MM`).

```powershell
$period = (Get-Date).ToString('yyyy-MM')

$payrollBody = To-Json @{
  baseSalary = 1000
  allowances = 200
  deductions = 50
}

$payrollJson = curl.exe -s -X POST "$API/api/payroll/generate/$EMP_ID/$period" `
  -H "Authorization: Bearer $ADMIN_TOKEN" `
  -H "Content-Type: application/json" `
  -d $payrollBody

$PAYROLL_ID = (ConvertFrom-Json $payrollJson).id
$PAYROLL_ID
```

### 7B) Download the payslip PDF (API)

```powershell
curl.exe -L -o "payslip.pdf" "$API/api/payroll/$PAYROLL_ID/pdf" `
  -H "Authorization: Bearer $ADMIN_TOKEN"

# opens in default PDF viewer
start .\payslip.pdf
```

UI navigation (admin):
- Logout (if you’re currently `employee1`) and login as `admin@local` / `secret`.
- Click **Payroll** in the top nav.
- Enter Employee ID (`$EMP_ID`), click **Open Employee**.
- Choose Payroll Month, enter Allowances + Deductions.
- Click **Generate Payroll**, then **Download Payslip**.

---

## Step 8 — Upload an employee document

There is no UI for document upload yet, so use the API.

Upload rules (defaults): max 10MB and allowed MIME types `application/pdf`, `image/png`, `image/jpeg`.
If you upload a PDF, the server also checks the file signature starts with `%PDF`.

### 8A) Upload

Pick any local PDF/PNG/JPEG file. Example assumes you have `C:\temp\demo.pdf`.

```powershell
$FILE = "C:\temp\demo.pdf"

$docJson = curl.exe -s -X POST "$API/api/emp/$EMP_ID/documents" `
  -H "Authorization: Bearer $EMP_TOKEN" `
  -F "file=@$FILE"

$DOC_ID = (ConvertFrom-Json $docJson).id
$DOC_ID
```

### 8B) Download the uploaded document

```powershell
curl.exe -L -o "downloaded-demo.pdf" "$API/api/emp/$EMP_ID/documents/$DOC_ID" `
  -H "Authorization: Bearer $EMP_TOKEN"
```

---

## Step 9 — Open dashboard

UI navigation:
- Open http://localhost:5173
- Login (any authenticated user).
- Click **Dashboard**.
- You should see:
  - Total Employees
  - Active This Month
  - Avg Attendance Hours
  - Leaves Pending
  - Salary Expense This Month
  - Charts: Department Counts + Leave Status

API verification (optional):

```powershell
curl.exe -s "$API/api/dashboard" -H "Authorization: Bearer $EMP_TOKEN"
```
