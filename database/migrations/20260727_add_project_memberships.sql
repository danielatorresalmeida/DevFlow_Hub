-- ============================================================================
-- DevFlow Hub - Add project memberships
--
-- This migration introduces project-scoped membership roles as the future
-- source of truth for project authorization.
--
-- Existing project managers become OWNER members.
-- Existing assignees of project tasks become CONTRIBUTOR members unless they
-- already hold a stronger project role.
-- ============================================================================

BEGIN;

-- 1. Membership table

CREATE TABLE IF NOT EXISTS project_memberships (
    id BIGSERIAL PRIMARY KEY,
    project_id BIGINT NOT NULL,
    collaborator_id BIGINT NOT NULL,
    role VARCHAR(30) NOT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',
    version BIGINT NOT NULL DEFAULT 0,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- 2. Foreign keys

ALTER TABLE project_memberships
    DROP CONSTRAINT IF EXISTS fk_project_memberships_project;

ALTER TABLE project_memberships
    ADD CONSTRAINT fk_project_memberships_project
    FOREIGN KEY (project_id)
    REFERENCES projects(id)
    ON DELETE CASCADE;

ALTER TABLE project_memberships
    DROP CONSTRAINT IF EXISTS fk_project_memberships_collaborator;

ALTER TABLE project_memberships
    ADD CONSTRAINT fk_project_memberships_collaborator
    FOREIGN KEY (collaborator_id)
    REFERENCES collaborators(id)
    ON DELETE CASCADE;

-- 3. Domain constraints

ALTER TABLE project_memberships
    DROP CONSTRAINT IF EXISTS project_memberships_role_check;

ALTER TABLE project_memberships
    ADD CONSTRAINT project_memberships_role_check
    CHECK (
        role IN (
            'OWNER',
            'MANAGER',
            'CONTRIBUTOR',
            'VIEWER'
        )
    );

ALTER TABLE project_memberships
    DROP CONSTRAINT IF EXISTS project_memberships_status_check;

ALTER TABLE project_memberships
    ADD CONSTRAINT project_memberships_status_check
    CHECK (
        status IN (
            'ACTIVE',
            'INACTIVE'
        )
    );

ALTER TABLE project_memberships
    DROP CONSTRAINT IF EXISTS project_memberships_version_check;

ALTER TABLE project_memberships
    ADD CONSTRAINT project_memberships_version_check
    CHECK (version >= 0);

-- 4. Indexes and membership uniqueness

CREATE UNIQUE INDEX IF NOT EXISTS
    ux_project_memberships_project_collaborator
    ON project_memberships(
        project_id,
        collaborator_id
    );

CREATE INDEX IF NOT EXISTS
    idx_project_memberships_project_status
    ON project_memberships(
        project_id,
        status
    );

CREATE INDEX IF NOT EXISTS
    idx_project_memberships_collaborator_status
    ON project_memberships(
        collaborator_id,
        status
    );

-- 5. Migrate existing project managers to OWNER

INSERT INTO project_memberships (
    project_id,
    collaborator_id,
    role,
    status,
    created_at,
    updated_at
)
SELECT
    project.id,
    project.manager_id,
    'OWNER',
    'ACTIVE',
    CURRENT_TIMESTAMP,
    CURRENT_TIMESTAMP
FROM projects project
WHERE project.manager_id IS NOT NULL
ON CONFLICT (
    project_id,
    collaborator_id
)
DO UPDATE SET
    role = 'OWNER',
    status = 'ACTIVE',
    version = project_memberships.version + 1,
    updated_at = CURRENT_TIMESTAMP
WHERE project_memberships.role IS DISTINCT FROM 'OWNER'
   OR project_memberships.status IS DISTINCT FROM 'ACTIVE';

-- 6. Migrate existing project-task assignees to CONTRIBUTOR

INSERT INTO project_memberships (
    project_id,
    collaborator_id,
    role,
    status,
    created_at,
    updated_at
)
SELECT DISTINCT
    task.project_id,
    task.assignee_id,
    'CONTRIBUTOR',
    'ACTIVE',
    CURRENT_TIMESTAMP,
    CURRENT_TIMESTAMP
FROM tasks task
WHERE task.project_id IS NOT NULL
  AND task.assignee_id IS NOT NULL
ON CONFLICT (
    project_id,
    collaborator_id
)
DO UPDATE SET
    role = CASE
        WHEN project_memberships.role = 'VIEWER'
            THEN 'CONTRIBUTOR'
        ELSE project_memberships.role
    END,
    status = 'ACTIVE',
    version = project_memberships.version + 1,
    updated_at = CURRENT_TIMESTAMP
WHERE project_memberships.role = 'VIEWER'
   OR project_memberships.status IS DISTINCT FROM 'ACTIVE';

-- 7. Validate migrated access relationships

DO $validation$
DECLARE
    missing_owner_count BIGINT;
    missing_assignee_membership_count BIGINT;
    invalid_membership_count BIGINT;
BEGIN
    SELECT COUNT(*)
    INTO missing_owner_count
    FROM projects project
    WHERE project.manager_id IS NOT NULL
      AND NOT EXISTS (
          SELECT 1
          FROM project_memberships membership
          WHERE membership.project_id = project.id
            AND membership.collaborator_id =
                    project.manager_id
            AND membership.role = 'OWNER'
            AND membership.status = 'ACTIVE'
      );

    SELECT COUNT(*)
    INTO missing_assignee_membership_count
    FROM tasks task
    WHERE task.project_id IS NOT NULL
      AND task.assignee_id IS NOT NULL
      AND NOT EXISTS (
          SELECT 1
          FROM project_memberships membership
          WHERE membership.project_id =
                    task.project_id
            AND membership.collaborator_id =
                    task.assignee_id
            AND membership.status = 'ACTIVE'
      );

    SELECT COUNT(*)
    INTO invalid_membership_count
    FROM project_memberships membership
    WHERE membership.role NOT IN (
              'OWNER',
              'MANAGER',
              'CONTRIBUTOR',
              'VIEWER'
          )
       OR membership.status NOT IN (
              'ACTIVE',
              'INACTIVE'
          )
       OR membership.version < 0;

    IF missing_owner_count > 0
       OR missing_assignee_membership_count > 0
       OR invalid_membership_count > 0 THEN

        RAISE EXCEPTION
            'Project membership migration failed. Missing owners: %, missing assignee memberships: %, invalid memberships: %.',
            missing_owner_count,
            missing_assignee_membership_count,
            invalid_membership_count;
    END IF;
END
$validation$;

COMMIT;
