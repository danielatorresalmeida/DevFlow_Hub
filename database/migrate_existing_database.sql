-- =============================================================
-- DevFlow Hub - optional migration for an existing English schema
-- Run this only when older tables use INTEGER ids.
-- A clean installation should use devflow_hub.sql instead.
-- =============================================================

BEGIN;

-- Foreign keys must be removed before changing id column types.
ALTER TABLE IF EXISTS projects DROP CONSTRAINT IF EXISTS projects_manager_id_fkey;
ALTER TABLE IF EXISTS projects DROP CONSTRAINT IF EXISTS fk_projects_manager;
ALTER TABLE IF EXISTS tasks DROP CONSTRAINT IF EXISTS tasks_project_id_fkey;
ALTER TABLE IF EXISTS tasks DROP CONSTRAINT IF EXISTS fk_tasks_project;
ALTER TABLE IF EXISTS tasks DROP CONSTRAINT IF EXISTS tasks_assignee_id_fkey;
ALTER TABLE IF EXISTS tasks DROP CONSTRAINT IF EXISTS fk_tasks_assignee;
ALTER TABLE IF EXISTS internal_programs DROP CONSTRAINT IF EXISTS internal_programs_manager_id_fkey;
ALTER TABLE IF EXISTS internal_programs DROP CONSTRAINT IF EXISTS fk_internal_programs_manager;

ALTER TABLE IF EXISTS collaborators ALTER COLUMN id TYPE BIGINT USING id::BIGINT;
ALTER TABLE IF EXISTS projects ALTER COLUMN id TYPE BIGINT USING id::BIGINT;
ALTER TABLE IF EXISTS projects ALTER COLUMN manager_id TYPE BIGINT USING manager_id::BIGINT;
ALTER TABLE IF EXISTS tasks ALTER COLUMN id TYPE BIGINT USING id::BIGINT;
ALTER TABLE IF EXISTS tasks ALTER COLUMN project_id TYPE BIGINT USING project_id::BIGINT;
ALTER TABLE IF EXISTS tasks ALTER COLUMN assignee_id TYPE BIGINT USING assignee_id::BIGINT;

ALTER TABLE IF EXISTS tasks
    ADD COLUMN IF NOT EXISTS created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    ADD COLUMN IF NOT EXISTS updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP;

UPDATE tasks
SET created_at = CURRENT_TIMESTAMP
WHERE created_at IS NULL;

UPDATE tasks
SET updated_at = CURRENT_TIMESTAMP
WHERE updated_at IS NULL;

ALTER TABLE tasks
    ALTER COLUMN created_at SET DEFAULT CURRENT_TIMESTAMP,
    ALTER COLUMN created_at SET NOT NULL,
    ALTER COLUMN updated_at SET DEFAULT CURRENT_TIMESTAMP,
    ALTER COLUMN updated_at SET NOT NULL;

ALTER TABLE IF EXISTS internal_programs ALTER COLUMN id TYPE BIGINT USING id::BIGINT;
ALTER TABLE IF EXISTS internal_programs ALTER COLUMN manager_id TYPE BIGINT USING manager_id::BIGINT;

ALTER TABLE projects
    ADD CONSTRAINT fk_projects_manager
    FOREIGN KEY (manager_id) REFERENCES collaborators(id) ON DELETE SET NULL;

ALTER TABLE tasks
    ADD CONSTRAINT fk_tasks_project
    FOREIGN KEY (project_id) REFERENCES projects(id) ON DELETE SET NULL;

ALTER TABLE tasks
    ADD CONSTRAINT fk_tasks_assignee
    FOREIGN KEY (assignee_id) REFERENCES collaborators(id) ON DELETE SET NULL;

ALTER TABLE internal_programs
    ADD CONSTRAINT fk_internal_programs_manager
    FOREIGN KEY (manager_id) REFERENCES collaborators(id) ON DELETE SET NULL;

COMMIT;
