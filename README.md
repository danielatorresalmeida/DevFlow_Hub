# DevFlow Hub

> Portuguese documentation: [`docs/README.pt.md`](docs/README.pt.md)

DevFlow Hub is an academic full-stack web application for managing collaborators, projects, Agile tasks, role-based access and tracked work time in one workspace.

The final submission is available on the `main` branch and is tagged as `submission-2026-07-30`.

## Overview

DevFlow Hub was created to reduce the fragmentation that occurs when project information, task ownership and time records are spread across messages, documents, spreadsheets and separate tools.

The application currently provides:

- JWT-based authentication;
- an authenticated dashboard with live PostgreSQL data;
- project creation, editing and deletion;
- task creation, editing and deletion;
- standalone tasks and project tasks;
- task status, priority, assignment and project association;
- task timer start, pause, resume and completion workflows;
- project memberships with `OWNER`, `MANAGER`, `CONTRIBUTOR` and `VIEWER` roles;
- project ownership transfer;
- structured API errors for HTTP `400`, `401`, `403`, `404` and `409`;
- persistent PostgreSQL storage;
- document and attachment metadata foundations;
- a configurable local object-storage provider;
- automated backend and frontend validation.

Internal-program CRUD is available in the backend. The dedicated React management page remains planned.

## Architecture

```text
React + TypeScript + Vite
          |
          | HTTP, JSON and JWT Bearer
          v
Spring Boot REST API
          |
          +------------------+
          |                  |
          v                  v
     PostgreSQL       Local object storage
```

The React frontend is responsible for navigation, forms, authenticated client state and data presentation. The Spring Boot backend is responsible for authentication, resource authorization, business rules, validation, transactions and persistence.

The object-storage abstraction is implemented in the backend, but the current HTTP API exposes attachment metadata only. File upload, download and content deletion remain on the roadmap.

Architecture documentation:

- [`docs/architecture/frontend-decision.md`](docs/architecture/frontend-decision.md)
- [`docs/architecture/project-access-control.md`](docs/architecture/project-access-control.md)
- [`docs/architecture/project-access-permission-matrix.md`](docs/architecture/project-access-permission-matrix.md)
- [`docs/architecture/project-access-endpoint-inventory.md`](docs/architecture/project-access-endpoint-inventory.md)

## Technology stack

### Backend

- Java 21
- Spring Boot 4.0.6
- Spring MVC
- Spring Security
- OAuth2 Resource Server
- Spring Data JPA
- Jakarta Validation
- Maven Wrapper

### Frontend

- React 19
- TypeScript 6
- Vite 8
- React Router 8.3.0
- Native Fetch API
- ESLint
- Vitest
- React Testing Library
- jsdom

### Data and infrastructure

- PostgreSQL
- H2 for automated tests
- Configurable local object storage
- GitHub Actions
- PowerShell PostgreSQL smoke tests

## Project structure

```text
DevFlow_Hub/
|-- .github/
|   `-- workflows/
|-- backend/
|   `-- src/
|-- database/
|   |-- migrations/
|   |-- devflow_hub.sql
|   |-- migrate_existing_database.sql
|   |-- INSTALACAO_BASE_DADOS_LOCAL.txt
|   `-- README.md
|-- docs/
|   `-- architecture/
|-- frontend/
|   |-- public/
|   `-- src/
|-- scripts/
|   `-- smoke-test-api.ps1
|-- LOG.md
`-- README.md
```

Generated directories such as `backend/target`, `frontend/node_modules` and `frontend/dist`, together with local configuration files such as `.env.local`, are intentionally excluded from Git.

## Quick start

### Prerequisites

- Java 21
- PostgreSQL
- Node.js `>=22.22.0`
- npm

### 1. Create the database

Create a PostgreSQL database named:

```text
devflow_hub
```

Run the installation script from the repository root:

```powershell
psql -U postgres -d devflow_hub -v ON_ERROR_STOP=1 -f database/devflow_hub.sql
```

The same script can be opened and executed through pgAdmin Query Tool.

Database documentation:

- [`database/README.md`](database/README.md)
- [`database/INSTALACAO_BASE_DADOS_LOCAL.txt`](database/INSTALACAO_BASE_DADOS_LOCAL.txt)

### 2. Configure the backend

Example PowerShell configuration:

```powershell
$env:DB_URL = "jdbc:postgresql://localhost:5432/devflow_hub"
$env:DB_USERNAME = "postgres"
$env:DB_PASSWORD = "<local-postgresql-password>"
$env:SHOW_SQL = "false"
$env:JWT_SECRET = "<valid-base64-secret-containing-at-least-32-decoded-bytes>"
$env:JWT_ISSUER = "https://devflow-hub.local"
$env:JWT_EXPIRATION = "PT15M"
$env:OBJECT_STORAGE_PROVIDER = "local"
$env:OBJECT_STORAGE_LOCAL_ROOT_DIRECTORY = ".\data\object-storage"
$env:PORT = "8080"
```

`JWT_SECRET` must be valid Base64 and contain at least 32 bytes after decoding.

Start the backend:

```powershell
Set-Location ".\backend"
.\mvnw.cmd spring-boot:run
```

The API is available at:

```text
http://localhost:8080/api
```

### 3. Install and start the frontend

```powershell
Set-Location ".\frontend"
npm ci
npm run dev
```

The application is available by default at:

```text
http://localhost:5173
```

During local development, requests beginning with `/api` are proxied to `http://localhost:8080`.

For another backend origin, create `frontend/.env.local`:

```text
VITE_API_BASE_URL=https://api.example.com
```

Do not commit `.env.local`.

## Demonstration data and accounts

### Fresh database installation

The current `database/devflow_hub.sql` seed creates a reproducible baseline containing:

- 3 collaborators;
- 2 projects;
- 3 tasks;
- 2 internal programs;
- initial project memberships;
- foreign keys, constraints and indexes.

The seed accounts are documented in [`database/README.md`](database/README.md).

### Prepared presentation database

The local database prepared for the final presentation contains four role-specific accounts:

| Project role | Name | Email |
|---|---|---|
| `OWNER` | Bruno Silva | `bruno.silva@devflowhub.pt` |
| `MANAGER` | Daniel Rocha | `daniel.rocha@devflowhub.pt` |
| `CONTRIBUTOR` | Carla Gomes | `carla.gomes@devflowhub.pt` |
| `VIEWER` | Ana Silva | `ana.silva@devflowhub.pt` |

Shared local demonstration password:

```text
DevFlowTest-123!
```

These are public academic demonstration credentials. They must be changed or removed before any use outside the local demonstration environment.

The role-specific presentation accounts are not yet created automatically by the current seed. To reproduce them on a fresh installation, synchronize the collaborators, password hash and memberships in `database/devflow_hub.sql`, or add an equivalent data migration.

## Authentication and session behaviour

The login endpoint is public:

```text
POST /api/auth/login
```

All other `/api/**` endpoints require a valid Bearer token, except where explicitly documented.

Protected requests must include:

```http
Authorization: Bearer <jwt-token>
```

The frontend stores the current session in `sessionStorage` and removes it when:

- the user signs out;
- the token expiry time is reached;
- an authenticated request returns HTTP `401`;
- the stored session is invalid.

The default JWT duration is `PT15M`. The current implementation uses absolute expiry and does not refresh the token based on user activity. A longer local value such as `PT1H` can be used for extended manual testing without changing the source code.

## Project access control

Project permissions are determined by active `project_memberships`, not by the professional value stored in `Collaborator.role`.

| Role | View project | Contribute | Manage project | Delete project | Manage memberships |
|---|---:|---:|---:|---:|---:|
| `OWNER` | Yes | Yes | Yes | Yes | Managers, Contributors and Viewers |
| `MANAGER` | Yes | Yes | Yes | No | Contributors and Viewers |
| `CONTRIBUTOR` | Yes | Yes | No | No | No |
| `VIEWER` | Yes | No | No | No | No |

Additional rules:

- only active memberships grant project access;
- project creation automatically assigns the creator as `OWNER` and initial manager;
- ownership transfer is explicit and transactional;
- generic membership endpoints cannot create, update or remove `OWNER`;
- removed memberships become `INACTIVE` and can be reactivated;
- optimistic-locking and duplicate conflicts return HTTP `409`;
- inaccessible resources are hidden with HTTP `404`;
- known resources with disallowed actions return HTTP `403`.

## Task rules

- project tasks follow the user's active membership and role;
- standalone tasks are private to their assignee;
- project assignees must be eligible active project members;
- timer actions are restricted to the task assignee;
- moving a task validates the current project, destination project and assignee before saving;
- task status cannot be changed while its timer is active;
- completed tasks cannot restart the timer.

Task scheduling dates are not implemented yet. `createdAt` and `updatedAt` are audit fields, not `startDate` and `dueDate` planning fields.

## Main API areas

### Authentication

```text
POST /api/auth/login
PUT  /api/auth/change-password
```

### Collaborators

```text
GET    /api/collaborators
GET    /api/collaborators/{id}
POST   /api/collaborators
PUT    /api/collaborators/{id}
DELETE /api/collaborators/{id}
```

### Projects and memberships

```text
GET    /api/projects
GET    /api/projects/{id}
POST   /api/projects
PUT    /api/projects/{id}
DELETE /api/projects/{id}
GET    /api/projects/{projectId}/members
POST   /api/projects/{projectId}/members
PATCH  /api/projects/{projectId}/members/{collaboratorId}
DELETE /api/projects/{projectId}/members/{collaboratorId}
POST   /api/projects/{projectId}/ownership-transfer
```

### Tasks and timer

```text
GET    /api/tasks
GET    /api/tasks/{id}
POST   /api/tasks
PUT    /api/tasks/{id}
DELETE /api/tasks/{id}
POST   /api/tasks/{id}/start-timer
POST   /api/tasks/{id}/pause-timer
POST   /api/tasks/{id}/resume-timer
GET    /api/tasks/{id}/total-time
GET    /api/tasks/{id}/timer
POST   /api/tasks/{id}/complete
```

### Documents and attachment metadata

```text
GET    /api/documents/{id}
GET    /api/projects/{projectId}/documents
GET    /api/tasks/{taskId}/documents
POST   /api/documents
PUT    /api/documents/{id}
DELETE /api/documents/{id}
GET    /api/attachments/{id}
GET    /api/documents/{documentId}/attachments
```

### Internal programs and dashboard

```text
GET    /api/internal-programs
GET    /api/internal-programs/{id}
POST   /api/internal-programs
PUT    /api/internal-programs/{id}
DELETE /api/internal-programs/{id}
GET    /api/dashboard
```

## Testing and validation

### Backend

```powershell
Set-Location ".\backend"
.\mvnw.cmd clean test
```

Final validated result:

```text
Tests run: 236
Failures: 0
Errors: 0
Skipped: 0
BUILD SUCCESS
```

The backend suite covers services, controllers, JPA persistence, authentication, resource authorization, project memberships, ownership transfer, task movement, concurrency, object storage and domain rules.

### Frontend

```powershell
Set-Location ".\frontend"
npm run lint
npm test
npm run build
```

Final validated result:

```text
Test Files: 12 passed
Tests: 53 passed
Lint: passed
Build: passed
```

### PostgreSQL smoke tests

With the backend running:

```powershell
powershell.exe `
  -NoProfile `
  -ExecutionPolicy Bypass `
  -File ".\scripts\smoke-test-api.ps1"
```

A successful run ends with:

```text
All PostgreSQL API smoke tests passed.
```

### GitHub Actions

The workflow is located at:

```text
.github/workflows/build-validation.yml
```

It validates the backend and frontend through Maven, npm, ESLint, Vitest and the production build.

## Current limitations

- no dedicated React page for internal-program management;
- document and attachment authorization is not yet fully aligned with project and task access control;
- attachment content upload, download and deletion are not exposed through HTTP;
- tasks do not yet have planning `startDate` and `dueDate` fields;
- no refresh-token workflow or activity-aware session renewal;
- no warning before session expiry or unsaved-change protection;
- the interface is currently English-only;
- collaborator and internal-program dashboard totals are still global;
- no separate global administrative policy for collaborators and internal programs;
- presentation role accounts are not yet synchronized with the fresh-install seed.

## Roadmap

- English and Portuguese internationalisation;
- task start dates, due dates and overdue indicators;
- secure refresh tokens and session-expiry warnings;
- dedicated internal-program React interface;
- project and task notes pages;
- complete document and attachment authorization;
- secure attachment upload and download;
- import centre for Notion and Toggl Track data;
- duplicate detection, mapping, conflict resolution and import reports;
- filters, notifications and report export;
- functional identifiers such as `PRJ-0025` while preserving internal database IDs;
- deployment, monitoring, backup and production-security review.

## Repository strategy and releases

- `main`: stable submission branch;
- `develop`: integration branch;
- `feature/*`: new functionality;
- `fix/*`: corrections;
- `test/*`: tests;
- `docs/*`: documentation;
- `chore/*`: configuration and maintenance.

Release tags:

- `presentation-2026-07-30-final`: presentation snapshot;
- `submission-2026-07-30`: complete final academic submission.

## Author

Daniela Torres Almeida
