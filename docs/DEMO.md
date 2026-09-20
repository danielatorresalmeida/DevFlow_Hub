# Local interview demo

**DEMO / LOCAL DEVELOPMENT ONLY.** The accounts below and their password are public fixtures. Use an isolated local database; never deploy this seed or reuse its credentials. The normal installation SQL creates schema only, with no accounts or automatic ADMIN. Existing installations are not reset or stripped of old demo accounts: review/remove those accounts explicitly before any deployment.

## Start from a fresh clone

Requirements: Java 21, PostgreSQL with `psql`/`createdb` on PATH, Node >=22.22 and npm. Commands start at the repository root, in PowerShell. On Windows, PostgreSQL binaries may need to be added to PATH from its installation `bin` directory. Use your own local PostgreSQL password when prompted.

```powershell
createdb -h localhost -U postgres devflow_demo
psql -h localhost -U postgres -d devflow_demo -v ON_ERROR_STOP=1 -f database/devflow_hub.sql
psql -h localhost -U postgres -d devflow_demo -v ON_ERROR_STOP=1 -f database/demo_seed.sql
```

`demo_seed.sql` checks the database name before writing. It creates six accounts, two projects, five memberships, three tasks and two documents. Re-running adds missing fixtures without resetting existing passwords, memberships or edits. For an exact reset, create a new disposable local database after removing the previous demo through your database administrator's normal process; do not run destructive reset commands against real data.

Configure and run the backend in this terminal:

```powershell
$env:DB_URL = 'jdbc:postgresql://localhost:5432/devflow_demo'
$env:DB_USERNAME = 'postgres'
$env:DB_PASSWORD = '<your-local-postgresql-password>'
$random = [System.Security.Cryptography.RandomNumberGenerator]::Create()
$bytes = New-Object byte[] 32
$random.GetBytes($bytes)
$random.Dispose()
$env:JWT_SECRET = [Convert]::ToBase64String($bytes)
$env:DEVFLOW_BOOTSTRAP_ADMIN_ENABLED = 'false'
$env:PORT = '8080'
Set-Location backend
.\mvnw.cmd spring-boot:run
```

The JWT signing secret is generated locally, is not a demo password and must not be committed. The seed already supplies a local ADMIN, so leave bootstrap disabled. For a non-demo installation use the one-time bootstrap described in the README, then disable it after the first successful start. Enabled bootstrap with an existing ADMIN intentionally fails startup; incomplete configuration also fails without creating an account.

In a second terminal, from the repository root:

```powershell
Set-Location frontend
npm ci
npm run dev
```

Open `http://localhost:5173`. The Vite proxy forwards `/api` to port 8080. If that port is occupied, stop the conflicting local service or configure the proxy deliberately; do not silently connect the frontend to another API. Nothing in this walkthrough requires Docker or hosted services.

## Accounts

Shared password: `DevFlowDemo-2026!` — **DEMO / LOCAL DEVELOPMENT ONLY**.

| Email | System role | Interview Workspace | Private Workspace |
|---|---|---|---|
| owner@demo.example | USER | OWNER | None |
| manager@demo.example | USER | MANAGER | None |
| contributor@demo.example | USER | CONTRIBUTOR | None |
| viewer@demo.example | USER | VIEWER | None |
| outsider@demo.example | USER | None | OWNER |
| admin@demo.example | ADMIN | None | None |

ADMIN manages collaborators and internal programs through the API. It does not see project documents without membership. Professional titles shown on the dashboard are descriptive; they are not project roles.

## Interview walkthrough (about 10 minutes)

1. Sign in as `owner@demo.example`. The dashboard shows one accessible project and three tasks; collaborator totals remain global.
2. Open **Projects → Interview Workspace**. In **Members**, explain OWNER, MANAGER, CONTRIBUTOR and VIEWER and the active membership requirement.
3. Open **Tasks → Create task**, choose Interview Workspace and an eligible assignee, then save. Open that task and use **Edit task** to change its description or priority. The form and persisted task are real application features.
4. Sign out and sign in as `viewer@demo.example`. Open the same project: **Read-only access** is displayed and management controls are unavailable. Explain that the API enforces this too; hidden UI controls alone are not authorization.
5. Demonstrate documents through the API script below. There is currently **no documents React page** and no HTTP file-upload/download endpoint. The script logs in all six accounts, creates two temporary documents, checks viewer reads, forbidden contributor edits/deletes, outsider and ADMIN 404s, a rejected cross-project move, an allowed manager edit and missing IDs. It deletes only the documents it created, including on failure. It does not print JWTs.
6. Show `DocumentAuthorizationIntegrationTest` and the document policy. HTTP integration tests use signed JWTs through Spring Security; the local script proves the selected workflows against PostgreSQL.

From the repository root while the API runs:

```powershell
.\scripts\demo-documents.ps1
# Optional alternative loopback port:
# .\scripts\demo-documents.ps1 -BaseUrl http://localhost:8081
```

A successful run includes `All local document authorization checks passed.` and cleanup responses `204`. The script requires a loopback `BaseUrl`; run it against the local demo API described above. This initial URL check does not enforce a redirect destination policy. There is no automatic permission escalation or role editing.

## Real screenshots

Captured on 2026-09-20 from the running React application and local PostgreSQL demo, in the browser's narrow responsive viewport. No mockups. The task description includes a saved edit from the walkthrough. Full-page dashboard/task images can be opened at their original size.

| Login | Project detail | Read-only membership |
|---|---|---|
| ![Login](screenshots/login.png) | ![Project](screenshots/project.png) | ![VIEWER access](screenshots/viewer-access.png) |

Additional captures: [Dashboard](screenshots/dashboard.png), [Task management](screenshots/task.png).

## Verification commands

```powershell
# Backend, from backend/
.\mvnw.cmd clean verify
# Focused security tests
.\mvnw.cmd '-Dtest=DocumentAuthorizationIntegrationTest,DocumentServiceTest,ApiSecurityIntegrationTest,InitialAdminBootstrapTest' test
# Frontend, from frontend/
npm ci
npm run lint
npm test
npm run build
# PostgreSQL migration test, from repository root (temporary table only)
psql -h localhost -U postgres -d devflow_demo -f database/tests/system_roles.sql
```

The schema uses `ddl-auto=validate`; Hibernate does not silently create missing production tables. H2 remains the automated Java-suite database; PostgreSQL migration and API demo checks are separate explicit commands.
