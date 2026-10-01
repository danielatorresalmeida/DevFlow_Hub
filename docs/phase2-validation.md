# Phase 2 validation and integration notes

Validated locally on 2026-09-20. Changes are confined to DevFlow_Hub.

## Existing develop work

The branch starts at develop `8fe95dc`, four commits ahead of main `9f3cdb1`: `cbeb778` SystemRole, `6e0927f` global authorization, `5039095` initial administrator bootstrap and merge commit `8fe95dc` (PR #52). These features were reused, not reimplemented. Their database migration should precede running the new application on an existing database. The PR targets main and includes this existing develop history before document hardening; no main/develop merge or auto-merge is performed by this task.

The foundations are coherent with the architecture: ADMIN decisions use the active database account, JWT claims are descriptive, and project membership remains independent. Existing tests covered account creation, blocked USER writes, administrator writes and bootstrap disabled/existing-admin/duplicate-email/weak-password cases. New tests add self/other promotion attempts, ignored role injection on administrator updates, stale ADMIN JWT after database demotion and missing bootstrap fields. Global roles are exposed in the login DTO/JWT, not writable through generic collaborator JSON.

One required correction: the old installation script seeded a publicly known ADMIN password. Installation now creates schema only; public fixture accounts are isolated behind the dedicated demo database guard. This does not remove previously installed demo accounts. Existing database owners must review those separately.

## Evidence

| Check | Result |
|---|---|
| main baseline `clean verify` | 236 passed |
| develop baseline `clean verify` | 255 passed |
| Before document fix | 18 HTTP cases: 15 reproduced failures, 3 legitimate controls passed |
| Final backend `clean verify` | 283 passed, 0 failures/errors/skips |
| Frontend `npm ci`, lint, test, build | Passed; 53 tests in 12 files |
| PostgreSQL 18 migration test | Legacy USER default, preserved ADMIN on rerun, invalid/null roles rejected |
| Fresh schema + demo seed | Six accounts, two projects, five memberships, three tasks, two documents |
| Seed rerun | No duplicates; existing fixture edits preserved |
| Seed against a non-demo database | Rejected before writes |
| Real PostgreSQL API walkthrough | Allowed/denied document operations and cleanup passed |
| Browser walkthrough | Login, dashboard, project/members, task edit persisted, VIEWER read-only state |
| Independent read-only document review | No concrete in-scope surviving bypass or regression found |

The independent reviewer could not run Maven from its offline cache; runtime evidence above comes from the primary execution, not from that reviewer. The original IDOR triggers now return 404/403/401 as applicable while the legitimate CRUD controls pass. Security outcome for the scoped document boundary: fixed.

## CI and protection

One workflow handles pull requests and pushes for both main and develop. Backend runs Java 21 `clean verify`. Frontend runs Node 22 `npm ci`, lint, tests and build. The jobs are named **Backend** and **Frontend**.

Both branches were unprotected before this work. Protection was applied and read back: require a pull request, one approving review, dismissal of stale approvals, resolved conversations, current branch with successful Backend and Frontend checks; enforce for administrators; no force pushes or deletion. A separate reviewer is required because PR authors cannot approve their own PR. No branch contents were changed by protection settings.

To reproduce manually: GitHub repository **Settings → Branches → Add branch protection rule** for `main`, then `develop`; require PR, 1 approval, dismiss stale reviews, resolve conversations, require status checks **Backend** and **Frontend**, require up-to-date branch, apply to administrators, and leave force pushes/deletion disabled. Remote CI results are reported on the PR; local success alone is not a remote CI result.

## Remaining limitations

- Attachment metadata GET endpoints still have a separate unprotected resource boundary. They do not return document content, but filenames/metadata may be disclosed to authenticated outsiders. This is a known follow-up before deployment, not covered by a claim of repository-wide IDOR closure.
- No document provenance/creator field; project document edit/delete is intentionally OWNER/MANAGER only, even for an assigned task contributor.
- No document React UI, file upload/download or internal-program management UI.
- Existing global collaborator/internal-program directory visibility and global dashboard totals remain as documented.
- Bootstrap is a single-instance provisioning step; concurrent multi-instance first startup is not serialized. Disable it after provisioning. No generic API for granting/revoking ADMIN is introduced.
- The frontend dependency installation reports 5 existing audit alerts (3 moderate, 2 high). No dependency upgrade was mixed into this resource-authorization change.
- PostgreSQL checks are reproducible local scripts; CI Java integration tests still use H2.

Next justified work: review attachment metadata authorization, triage dependency alerts, and consider adding PostgreSQL checks to CI. No Phase 3 work is started.
