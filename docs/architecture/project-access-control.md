# Project Access Control Architecture Decision

## Status

Accepted for incremental implementation.

The persistence foundation is already defined through `ProjectMembership`. The central authorization foundation and project-service enforcement are being delivered in separate pull requests so that each security boundary can be reviewed independently.

## Context

DevFlow Hub authenticates API requests with JWT Bearer tokens. Authentication proves which collaborator is making a request, but it does not by itself prove that the collaborator may access a specific project or a resource that belongs to that project.

The current domain contains direct identifiers instead of JPA relationships for several resource links:

- `Project.managerId`
- `ProjectMembership.projectId`
- `ProjectMembership.collaboratorId`
- `Task.projectId`
- `Task.assigneeId`
- `Document.projectId`
- `Document.taskId`
- `Attachment.documentId`

Without resource-level authorization, an authenticated collaborator could attempt to access another project's resources by changing an identifier in a request. The authorization model must therefore prevent insecure direct object reference risks and must remain enforceable when resources are read, updated, moved between parents or deleted through a parent operation.

## Decision

### Authentication and authorization remain separate

Spring Security remains responsible for validating JWTs and establishing the authenticated principal.

Application services remain responsible for resource authorization. Controllers must not be the only enforcement point, because the same service methods may later be called by other controllers, scheduled jobs or integration adapters.

### Current collaborator resolution is centralized

A central resolver obtains the authenticated JWT from Spring Security, validates its `sub` claim, resolves the corresponding collaborator and rejects collaborators that are missing or inactive.

Business services must not parse JWT subjects independently.

### Project membership is the source of truth

`ProjectMembership` is the authoritative source for project access.

Only memberships with status `ACTIVE` grant access.

`Collaborator.role` remains a professional or organizational description and must not be used as a project authorization role.

The project roles are:

- `OWNER`
- `MANAGER`
- `CONTRIBUTOR`
- `VIEWER`

The complete operation matrix is documented in [`project-access-permission-matrix.md`](project-access-permission-matrix.md).

### Authorization is evaluated through named permissions

Services request a permission instead of embedding repeated role comparisons.

The initial project-level permissions are:

- `VIEW_PROJECT`
- `CONTRIBUTE_TO_PROJECT`
- `MANAGE_PROJECT`
- `DELETE_PROJECT`

Resource-specific checks may add concepts such as managing any resource, managing an owned resource or operating an assigned task, but they must still resolve the relevant project membership through the same central authorization service.

### Resource resolution follows the ownership chain

Authorization for each resource type follows this chain:

```text
Project
  -> ProjectMembership

Task with projectId
  -> Project
  -> ProjectMembership

Standalone task
  -> assigneeId
  -> current collaborator

Document with projectId
  -> Project
  -> ProjectMembership

Document with taskId
  -> Task
  -> ProjectMembership or standalone-task assignee rule

Attachment
  -> Document
  -> Project or Task
  -> ProjectMembership or standalone-task assignee rule
```

A missing link in this chain must not result in broader access.

### Existing and target parents are both authorized

When an update can move a resource, authorization must be checked against both the current parent and the requested target parent.

Examples include:

- changing `Task.projectId`;
- changing `Document.projectId`;
- changing `Document.taskId`;
- moving an attachment if that capability is introduced later.

The required order is:

1. load the existing resource;
2. authorize access to its current parent;
3. validate and authorize the target parent;
4. apply the mutation;
5. persist the resource.

This prevents a collaborator from using an authorized resource as a bridge into an unauthorized project.

### Parent deletion is authorized before cascades

A project, task or document must be authorized before repository deletion begins.

Cascade cleanup, attachment deletion and object-storage cleanup must not execute before authorization succeeds. This prevents a lower-level delete operation from bypassing the parent authorization rule.

### HTTP responses hide inaccessible resources consistently

The API uses these semantics:

- `401 Unauthorized` when authentication is missing, invalid, refers to a missing collaborator or refers to an inactive collaborator;
- `404 Not Found` when the resource does not exist or when the authenticated collaborator has no active membership and the resource must remain hidden;
- `403 Forbidden` when the collaborator has an active membership and can know the resource exists, but the membership role does not allow the requested operation.

Error messages must not disclose whether a hidden project or nested resource exists.

### Project creation establishes ownership atomically

Any active authenticated collaborator may create a project.

The collaborator who creates it becomes an active `OWNER` automatically.

While `projects.manager_id` remains for compatibility, the backend sets it to the creator's collaborator ID. A client-provided `managerId` that is different from the authenticated creator is rejected with HTTP `400` rather than silently trusted.

Project creation and owner-membership creation occur in one transaction. A membership failure must roll back the project insert.

### Multiple owners are allowed

A project may have more than one active `OWNER`.

The last active owner cannot be removed, deactivated or demoted. Ownership transfer therefore requires adding or promoting another owner before changing the existing final owner.

A `MANAGER` cannot add, remove, promote or demote an `OWNER` or another `MANAGER`. Managers may manage `CONTRIBUTOR` and `VIEWER` memberships only.

### Ownership-sensitive resource actions require provenance

The following ownership definitions apply:

- an owned task is a task whose `assigneeId` equals the current collaborator ID;
- an owned document is a document whose future `createdById` equals the current collaborator ID;
- an owned attachment is an attachment whose future `uploadedById` equals the current collaborator ID.

Until the required provenance field exists, the application must use the more restrictive permission and must not infer ownership from mutable or unrelated data.

### Standalone tasks remain private to the assignee

A task with no `projectId` is a standalone task.

A standalone task may be viewed and changed only by its assignee. Creation must either assign the task to the authenticated collaborator or be rejected. A standalone task with no assignee must not be exposed through general task listings.

Timer start, pause, resume and completion actions are performed by the task assignee. Project managers can reassign or update a project task according to their project permissions, but they do not operate another collaborator's personal timer.

### Listings and aggregates are filtered at the data boundary

List, count and dashboard operations must use access-aware repository queries or already-filtered service results.

The application must not load all projects, tasks, documents or attachments and then rely on the frontend to hide unauthorized records.

Dashboard totals and recent-item lists must be computed only from resources accessible to the current collaborator.

### Global resources require a separate policy

Collaborators and internal programs are not project-scoped resources.

Their current authenticated CRUD endpoints require a future system-level administration policy. Project roles must not be reused as global administrator roles.

Until that policy is implemented, these endpoints remain a known authorization gap and must not be treated as covered by this decision.

## Implementation status and boundaries

The authorization architecture has been delivered incrementally. The current status is:

| Area | Status |
|---|---|
| Project-membership persistence | Implemented |
| Current-collaborator resolution and project permissions | Implemented |
| Project listing, read, update and delete enforcement | Implemented |
| Transactional project creation with automatic `OWNER` membership | Implemented |
| Task authorization, standalone-task rules and parent moves | Implemented |
| Project-membership management endpoints | Implemented |
| Explicit transactional ownership transfer | Implemented |
| Dashboard filtering for projects, tasks, status totals, tracked time and recent items | Implemented |
| Document authorization and creator provenance | Pending |
| Attachment authorization, upload provenance and HTTP object-storage operations | Pending |
| Separate global authorization for collaborators and internal programs | Pending |

The collaborator and internal-program totals in the dashboard remain global until the separate organization-level authorization policy is implemented. The current endpoint inventory is documented in [`project-access-endpoint-inventory.md`](project-access-endpoint-inventory.md).

## Consequences

### Positive consequences

- authorization rules are centralized and testable;
- project membership has one clear meaning;
- direct identifier manipulation does not grant access;
- nested-resource checks follow a consistent chain;
- service-layer checks protect all callers;
- project creation cannot assign ownership to another collaborator;
- listings and dashboards do not leak inaccessible records;
- future object-storage operations inherit the same resource authorization model.

### Trade-offs

- repositories need access-aware queries;
- updates that move resources require more than one authorization check;
- provenance columns are required for ownership-sensitive document and attachment operations;
- membership management needs invariants for the final owner;
- global resources need a separate authorization design;
- controller and service tests must represent multiple roles and hidden-resource behaviour.

## Rejected alternatives

### Use `Collaborator.role` for authorization

Rejected because it describes a collaborator's professional role and does not vary by project.

### Authorize only in controllers

Rejected because service methods could be called from another entry point and bypass controller checks.

### Return `403` for every inaccessible resource

Rejected because it confirms that a hidden resource exists and increases enumeration risk.

### Trust `managerId` supplied during project creation

Rejected because it would let a collaborator create a project under another collaborator's identity or ownership context.

### Allow a project to have no owner

Rejected because membership administration, destructive actions and ownership transfer would have no accountable authority.
