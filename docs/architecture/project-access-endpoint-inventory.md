# Project Access Endpoint Inventory

## Status

Current endpoint inventory with target authorization requirements and delivery order.

The inventory is based on the existing Spring MVC controllers. It distinguishes endpoints already covered by project-access work, endpoints still requiring project-scoped enforcement and endpoints that need a separate global authorization policy.

## Authentication endpoints

| Method and path | Scope | Target rule | Status |
|---|---|---|---|
| `POST /api/auth/login` | Public authentication | Public; inactive collaborators are rejected | Implemented |
| `PUT /api/auth/change-password` | Current collaborator | Authenticated collaborator may change only their own password; principal comes from JWT `sub` | Implemented, should migrate to the central current-collaborator resolver for consistency |

## Project endpoints

| Method and path | Parent resolution | Required authorization | Important validation | Delivery |
|---|---|---|---|---|
| `GET /api/projects` | Current collaborator -> active memberships | Return only projects with active membership | Filter in repository/service, not frontend | Project enforcement PR |
| `GET /api/projects/{id}` | Project -> membership | `VIEW_PROJECT` | Return `404` for missing or hidden project | Project enforcement PR |
| `POST /api/projects` | Authenticated creator | Any active authenticated collaborator | Create project and active `OWNER` membership atomically; reject foreign `managerId` | Project creation PR |
| `PUT /api/projects/{id}` | Existing project -> membership | `MANAGE_PROJECT` | Authorize before loading mutable data; do not let `managerId` replace membership authority | Project enforcement PR, compatibility refinement later |
| `DELETE /api/projects/{id}` | Existing project -> membership | `DELETE_PROJECT` | Authorize before database cascades and object-storage cleanup | Project enforcement PR |

## Task endpoints

### Common task resolution

A project task resolves through:

```text
Task -> projectId -> ProjectMembership
```

A standalone task resolves through:

```text
Task -> assigneeId -> current collaborator
```

General task listings must include only accessible project tasks and standalone tasks assigned to the current collaborator.

| Method and path | Required authorization | Important validation | Delivery |
|---|---|---|---|
| `GET /api/tasks` | Accessible project memberships or standalone assignee | Query must filter before returning records | Task authorization PR |
| `GET /api/tasks/{id}` | View parent project or be standalone assignee | Missing and hidden tasks return `404` | Task authorization PR |
| `POST /api/tasks` | `CONTRIBUTE_TO_PROJECT`, or create standalone task for self | Project assignee must be an active project member; contributor assigns only self | Task authorization PR |
| `PUT /api/tasks/{id}` | Manager-any or contributor-owned task | Authorize existing parent and requested target parent; restrict reassignment | Task authorization PR |
| `DELETE /api/tasks/{id}` | Manager-any or contributor-owned task | Authorize before document and attachment cascades | Task authorization PR |
| `POST /api/tasks/{id}/start-timer` | Task assignee | Managers do not operate another collaborator's timer | Task authorization PR |
| `POST /api/tasks/{id}/pause-timer` | Task assignee | Same ownership rule as start | Task authorization PR |
| `POST /api/tasks/{id}/resume-timer` | Task assignee | Same ownership rule as start | Task authorization PR |
| `GET /api/tasks/{id}/total-time` | View task | Hidden task returns `404` | Task authorization PR |
| `GET /api/tasks/{id}/timer` | View task | Timer state must not expose hidden task existence | Task authorization PR |
| `POST /api/tasks/{id}/complete` | Task assignee for personal workflow | Administrative status changes remain separate from timer completion | Task authorization PR |

### Task move checks

When `projectId` changes:

1. load and authorize the existing task;
2. authorize the current parent;
3. authorize contribution access to the target project;
4. verify the requested assignee is allowed in the target project;
5. save only after every check succeeds.

Moving a project task to a standalone task is allowed only when the resulting assignee is the current collaborator and the caller is allowed to modify the existing task.

## Document endpoints

### Common document resolution

A document belongs to exactly one project or task.

```text
Document -> projectId -> ProjectMembership
```

or:

```text
Document -> taskId -> Task -> ProjectMembership or standalone assignee
```

| Method and path | Required authorization | Important validation | Delivery |
|---|---|---|---|
| `GET /api/documents/{id}` | View resolved project or task | Hidden document returns `404` | Document authorization PR |
| `GET /api/projects/{projectId}/documents` | `VIEW_PROJECT` | Authorize project before listing documents | Document authorization PR |
| `GET /api/tasks/{taskId}/documents` | View task | Authorize task before listing documents | Document authorization PR |
| `POST /api/documents` | Contribute to resolved parent | Parent must be exactly one project or task; record `createdById` | Document authorization and provenance PR |
| `PUT /api/documents/{id}` | Manager-any or contributor-owned document | Authorize current and target parent when moving | Document authorization and provenance PR |
| `DELETE /api/documents/{id}` | Manager-any or contributor-owned document | Authorize before attachment and object cleanup | Document authorization and provenance PR |

Until `createdById` exists, contributor-owned update and delete operations remain disabled and use the manager-level rule.

## Attachment endpoints

### Current metadata endpoints

| Method and path | Required authorization | Important validation | Delivery |
|---|---|---|---|
| `GET /api/attachments/{id}` | View resolved document parent | Resolve attachment -> document -> project/task | Attachment authorization PR |
| `GET /api/documents/{documentId}/attachments` | View document | Authorize document before listing metadata | Attachment authorization PR |

### Planned content endpoints

The final route names may be adjusted when attachment delivery is integrated, but every upload, download and delete operation must use the same parent-resolution rule.

| Operation | Required authorization | Important validation |
|---|---|---|
| Upload attachment to document | `CONTRIBUTE_TO_PROJECT` or allowed standalone-task ownership | Record `uploadedById`; validate content and storage limits before finalizing metadata |
| Download attachment content | View resolved document | Authorize before opening object-storage stream |
| Delete attachment | Manager-any or contributor-owned attachment | Authorize before metadata deletion and object-storage cleanup |

Object-storage keys are internal identifiers and never substitute for resource authorization.

## Dashboard endpoint

| Method and path | Required authorization | Important validation | Delivery |
|---|---|---|---|
| `GET /api/dashboard` | Authenticated collaborator | Projects, tasks, totals and recent items must be calculated only from accessible resources | Dashboard authorization PR after task enforcement |

The current dashboard uses project and task services plus collaborator data. It must not expose global task counts or collaborator information unrelated to the current collaborator's accessible projects.

## Collaborator endpoints

| Method and path | Current exposure | Target policy | Delivery |
|---|---|---|---|
| `GET /api/collaborators` | Any authenticated collaborator | Replace unrestricted global listing with a system-admin endpoint or a limited directory of collaborators visible through shared active projects | Global authorization PR |
| `GET /api/collaborators/{id}` | Any authenticated collaborator | System admin or collaborator visible through an allowed relationship | Global authorization PR |
| `POST /api/collaborators` | Any authenticated collaborator | System administrator only, unless replaced by a controlled invitation flow | Global authorization PR |
| `PUT /api/collaborators/{id}` | Any authenticated collaborator | Self-service fields for current collaborator; administrative fields for system admin only | Global authorization PR |
| `DELETE /api/collaborators/{id}` | Any authenticated collaborator | System administrator only, with membership and ownership safeguards | Global authorization PR |

Project roles must not be treated as system-administrator roles.

## Internal program endpoints

| Method and path | Current exposure | Target policy | Delivery |
|---|---|---|---|
| `GET /api/internal-programs` | Any authenticated collaborator | Define organization-level read policy | Global authorization PR |
| `GET /api/internal-programs/{id}` | Any authenticated collaborator | Define organization-level read policy | Global authorization PR |
| `POST /api/internal-programs` | Any authenticated collaborator | System administrator or future program-manager role | Global authorization PR |
| `PUT /api/internal-programs/{id}` | Any authenticated collaborator | System administrator or future program-manager role | Global authorization PR |
| `DELETE /api/internal-programs/{id}` | Any authenticated collaborator | System administrator only or explicit program-owner policy | Global authorization PR |

## Project membership endpoints to add

The controller does not yet expose membership management. The target API requires operations equivalent to:

| Operation | Required authorization | Invariant |
|---|---|---|
| List project memberships | Any active project member | Return membership data only for that project |
| Add member | Owner, or manager for contributor/viewer roles | Collaborator and project must exist; prevent duplicate membership |
| Change role | Owner, or manager within contributor/viewer range | Manager cannot modify manager/owner; preserve final owner |
| Change status or remove member | Same as role-change authority | Preserve final owner and prevent privilege escalation |
| Leave project | Current collaborator | Final owner cannot leave until another owner exists |

Exact route design should be finalized when the membership service is implemented.

## Cross-cutting verification checklist

Every protected resource operation must verify:

1. authentication is valid and resolves to an active collaborator;
2. the resource exists or is returned as hidden `404`;
3. the resource's current parent is resolved;
4. the membership is active or standalone ownership is valid;
5. the requested operation is permitted;
6. any target parent is independently resolved and authorized;
7. any referenced assignee or member satisfies project-membership rules;
8. destructive side effects begin only after authorization;
9. list and aggregate queries filter in the backend;
10. tests cover owner, manager, contributor, viewer, inactive membership, no membership, missing resource and parent-move cases.
