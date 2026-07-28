# Project Access Permission Matrix

## Status

Accepted as the target authorization matrix for incremental implementation.

## Legend

- **Yes**: the role may perform the operation.
- **Own**: the role may perform the operation only when the resource belongs to that collaborator according to the ownership definition below.
- **Assigned**: the task assignee may perform the operation.
- **Limited**: the role may perform the operation only within the stated membership-management limits.
- **No**: the role may not perform the operation.

Only an `ACTIVE` project membership grants a project role.

## Ownership definitions

| Resource | Ownership rule |
|---|---|
| Task | `task.assigneeId` equals the authenticated collaborator ID |
| Document | future `document.createdById` equals the authenticated collaborator ID |
| Attachment | future `attachment.uploadedById` equals the authenticated collaborator ID |

A missing provenance field does not grant ownership. Until `createdById` or `uploadedById` exists, operations that depend on those fields use the more restrictive manager-level rule.

## Project operations

| Operation | OWNER | MANAGER | CONTRIBUTOR | VIEWER |
|---|---:|---:|---:|---:|
| View project | Yes | Yes | Yes | Yes |
| List project resources | Yes | Yes | Yes | Yes |
| Update project metadata | Yes | Yes | No | No |
| Change compatibility `managerId` | Yes | Limited | No | No |
| Delete project | Yes | No | No | No |
| Create task or document in project | Yes | Yes | Yes | No |
| Upload attachment | Yes | Yes | Yes | No |

`managerId` is temporary compatibility data. Membership role remains authoritative. A manager may not use this field to create or promote an owner.

## Project membership operations

| Operation | OWNER | MANAGER | CONTRIBUTOR | VIEWER |
|---|---:|---:|---:|---:|
| View project membership directory | Yes | Yes | Yes | Yes |
| Add `VIEWER` | Yes | Yes | No | No |
| Add `CONTRIBUTOR` | Yes | Yes | No | No |
| Add `MANAGER` | Yes | No | No | No |
| Add `OWNER` | Yes | No | No | No |
| Change `VIEWER` to `CONTRIBUTOR` | Yes | Yes | No | No |
| Change `CONTRIBUTOR` to `VIEWER` | Yes | Yes | No | No |
| Promote to `MANAGER` | Yes | No | No | No |
| Promote to `OWNER` | Yes | No | No | No |
| Modify an existing `MANAGER` | Yes | No | No | No |
| Modify an existing `OWNER` | Yes, except final-owner rule | No | No | No |
| Remove `VIEWER` or `CONTRIBUTOR` | Yes | Yes | No | No |
| Remove `MANAGER` | Yes | No | No | No |
| Remove `OWNER` | Yes, except final-owner rule | No | No | No |
| Leave project | Yes, except final-owner rule | Yes | Yes | Yes |

The last active owner cannot leave, be removed, be deactivated or be demoted.

## Task operations

| Operation | OWNER | MANAGER | CONTRIBUTOR | VIEWER |
|---|---:|---:|---:|---:|
| View project task | Yes | Yes | Yes | Yes |
| Create project task | Yes | Yes | Yes | No |
| Assign task to active project member | Yes | Yes | Self only | No |
| Update any project task | Yes | Yes | No | No |
| Update assigned project task | Yes | Yes | Own | No |
| Move task out of current project | Yes | Yes | Own, with target access | No |
| Move task into another project | Yes, with target access | Yes, with target access | Own, with target access | No |
| Delete any project task | Yes | Yes | No | No |
| Delete assigned project task | Yes | Yes | Own | No |
| Start, pause or resume timer | Assigned | Assigned | Assigned | No |
| Complete through personal workflow | Assigned | Assigned | Assigned | No |
| Reassign task | Yes | Yes | Self only | No |

For timer and personal completion actions, **Assigned** takes precedence over project rank. A manager may update or reassign another collaborator's task but does not operate that collaborator's timer.

## Standalone task operations

| Operation | Authenticated assignee | Other collaborator |
|---|---:|---:|
| View | Yes | No |
| Update | Yes | No |
| Delete | Yes | No |
| Start, pause, resume or complete | Yes | No |
| Convert to a project task | Yes, with target project contribution access | No |

A new standalone task must be assigned to the authenticated creator. An unassigned standalone task is not returned by general listings.

## Document operations

| Operation | OWNER | MANAGER | CONTRIBUTOR | VIEWER |
|---|---:|---:|---:|---:|
| View project or task document | Yes | Yes | Yes | Yes |
| Create document | Yes | Yes | Yes | No |
| Update any document | Yes | Yes | No | No |
| Update owned document | Yes | Yes | Own | No |
| Move document to another authorized parent | Yes | Yes | Own | No |
| Delete any document | Yes | Yes | No | No |
| Delete owned document | Yes | Yes | Own | No |

A document update that changes `projectId` or `taskId` requires authorization for both the current and target parent.

## Attachment operations

| Operation | OWNER | MANAGER | CONTRIBUTOR | VIEWER |
|---|---:|---:|---:|---:|
| View metadata | Yes | Yes | Yes | Yes |
| Download content | Yes | Yes | Yes | Yes |
| Upload | Yes | Yes | Yes | No |
| Delete any attachment | Yes | Yes | No | No |
| Delete owned attachment | Yes | Yes | Own | No |

Attachment authorization is resolved through `Attachment -> Document -> Project or Task`.

## Response semantics

| Situation | Response |
|---|---|
| Missing or invalid JWT | `401 Unauthorized` |
| JWT collaborator missing or inactive | `401 Unauthorized` |
| Resource does not exist | `404 Not Found` |
| Resource exists but no active membership or standalone ownership exists | `404 Not Found` |
| Active membership exists but role is insufficient | `403 Forbidden` |
| Target parent is invalid or inaccessible during a move | `404 Not Found` when hidden, otherwise `400` for a visible invalid relationship |

The API must not reveal whether a hidden resource exists.
