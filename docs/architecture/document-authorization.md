# Document authorization — Phase 2

## Baseline and integration order

Reviewed main `9f3cdb1` (236 backend tests) and develop `8fe95dc` (255 backend tests). Both passed `clean verify`; unchanged frontend passed lint, 53 tests and build. Phase 2 starts from develop to reuse SystemRole, global administration and InitialAdminBootstrap without reimplementing them. No main/develop merge is performed; the PR will include the existing develop commits and this hardening.

## Existing model

- An active authenticated collaborator has system role USER or ADMIN. ADMIN manages collaborator and internal-program writes through SystemAuthorizationService. Reads of the existing collaborator/program directories remain available to authenticated users. SystemRole is read from the database for administrative decisions; the JWT claim is descriptive and does not override current database permissions.
- Project membership is independent: active OWNER/MANAGER can manage resources; CONTRIBUTOR can contribute; VIEWER can read. Missing/inactive membership hides the project with 404. An active member lacking the requested permission receives 403.
- Project tasks inherit membership. Task-specific assignee actions remain in TaskAccessService. Standalone tasks are private to their current assignee.
- A document has exactly one parent, project or task. It has no creator field. Assigned task ownership is not document authorship. The existing endpoint inventory explicitly defers contributor-owned editing until provenance exists.

## Document policy to enforce

| Operation | Direct project / task in project | Standalone task |
|---|---|---|
| Read by ID / list | VIEW_PROJECT, any active project member | Current assignee |
| Create | CONTRIBUTE_TO_PROJECT: OWNER, MANAGER, CONTRIBUTOR | Current assignee |
| Edit / delete existing document | MANAGE_PROJECT: OWNER, MANAGER | Current assignee |
| Reassign | Manage original parent, then contribute to destination | Check original and destination under the same rules |

ADMIN has no project/document bypass. A manager moving a document into a project where they are only a contributor may subsequently lose edit/delete permission; destination contribution is the same rule as creating content there.

## Enforcement and errors

Use existing ProjectAccessService, TaskAccessService and CurrentCollaboratorResolver at the service boundary, including the optional findById service API. Validate active identity before resource lookup. Authorize the original document before validating/mutating update data, then authorize the destination before changing the managed entity.

Unknown/hidden document IDs return the same 404 `Document not found.`. Unknown/hidden parents use the existing project/task 404 response. Destination existence no longer produces a distinguishable 400; this intentional change avoids existence disclosure. Visible but insufficient roles remain 403. Invalid exactly-one-parent input remains 400. Invalid stored associations fail closed. Keep normalization, audit-field handling, transaction boundaries and list ordering.

Project/task visibility changes also change document visibility dynamically; no copied permission flags are introduced. Attachment metadata endpoints are a separate existing boundary and are not evidence that document content is authorized; their status must be stated explicitly in the delivery limitations. No file-upload/download feature is added.
