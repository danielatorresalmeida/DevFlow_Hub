-- ============================================================================
-- DevFlow Hub - Restore core foreign keys
--
-- This migration restores foreign keys that may be missing from databases
-- created with earlier versions of the project.
--
-- All relationships use ON DELETE SET NULL so that dependent records remain
-- available when their referenced project, collaborator, or manager is deleted.
-- ============================================================================

BEGIN;

-- 1. Validate that existing references are not orphaned.

DO $validation$
DECLARE
    orphan_projects_manager BIGINT;
    orphan_tasks_project BIGINT;
    orphan_tasks_assignee BIGINT;
    orphan_programs_manager BIGINT;
BEGIN
    SELECT COUNT(*)
    INTO orphan_projects_manager
    FROM projects p
    WHERE p.manager_id IS NOT NULL
      AND NOT EXISTS (
          SELECT 1
          FROM collaborators c
          WHERE c.id = p.manager_id
      );

    SELECT COUNT(*)
    INTO orphan_tasks_project
    FROM tasks t
    WHERE t.project_id IS NOT NULL
      AND NOT EXISTS (
          SELECT 1
          FROM projects p
          WHERE p.id = t.project_id
      );

    SELECT COUNT(*)
    INTO orphan_tasks_assignee
    FROM tasks t
    WHERE t.assignee_id IS NOT NULL
      AND NOT EXISTS (
          SELECT 1
          FROM collaborators c
          WHERE c.id = t.assignee_id
      );

    SELECT COUNT(*)
    INTO orphan_programs_manager
    FROM internal_programs ip
    WHERE ip.manager_id IS NOT NULL
      AND NOT EXISTS (
          SELECT 1
          FROM collaborators c
          WHERE c.id = ip.manager_id
      );

    IF orphan_projects_manager > 0
       OR orphan_tasks_project > 0
       OR orphan_tasks_assignee > 0
       OR orphan_programs_manager > 0 THEN

        RAISE EXCEPTION
            'Core foreign key migration blocked. Orphans: projects.manager_id=%, tasks.project_id=%, tasks.assignee_id=%, internal_programs.manager_id=%.',
            orphan_projects_manager,
            orphan_tasks_project,
            orphan_tasks_assignee,
            orphan_programs_manager;
    END IF;
END
$validation$;

-- 2. Restore project manager relationship.

ALTER TABLE projects
    DROP CONSTRAINT IF EXISTS projects_manager_id_fkey;

ALTER TABLE projects
    DROP CONSTRAINT IF EXISTS fk_projects_manager;

ALTER TABLE projects
    ADD CONSTRAINT fk_projects_manager
    FOREIGN KEY (manager_id)
    REFERENCES collaborators(id)
    ON DELETE SET NULL;

-- 3. Restore task project relationship.

ALTER TABLE tasks
    DROP CONSTRAINT IF EXISTS tasks_project_id_fkey;

ALTER TABLE tasks
    DROP CONSTRAINT IF EXISTS fk_tasks_project;

ALTER TABLE tasks
    ADD CONSTRAINT fk_tasks_project
    FOREIGN KEY (project_id)
    REFERENCES projects(id)
    ON DELETE SET NULL;

-- 4. Restore task assignee relationship.

ALTER TABLE tasks
    DROP CONSTRAINT IF EXISTS tasks_assignee_id_fkey;

ALTER TABLE tasks
    DROP CONSTRAINT IF EXISTS fk_tasks_assignee;

ALTER TABLE tasks
    ADD CONSTRAINT fk_tasks_assignee
    FOREIGN KEY (assignee_id)
    REFERENCES collaborators(id)
    ON DELETE SET NULL;

-- 5. Restore internal program manager relationship.

ALTER TABLE internal_programs
    DROP CONSTRAINT IF EXISTS internal_programs_manager_id_fkey;

ALTER TABLE internal_programs
    DROP CONSTRAINT IF EXISTS fk_internal_programs_manager;

ALTER TABLE internal_programs
    ADD CONSTRAINT fk_internal_programs_manager
    FOREIGN KEY (manager_id)
    REFERENCES collaborators(id)
    ON DELETE SET NULL;

-- 6. Validate the installed constraints.
-- PostgreSQL represents ON DELETE SET NULL with confdeltype = 'n'.

DO $validation$
BEGIN
    IF NOT EXISTS (
        SELECT 1
        FROM pg_constraint
        WHERE conrelid = 'public.projects'::regclass
          AND conname = 'fk_projects_manager'
          AND contype = 'f'
          AND confrelid = 'public.collaborators'::regclass
          AND confdeltype = 'n'
    ) THEN
        RAISE EXCEPTION
            'Validation failed for fk_projects_manager.';
    END IF;

    IF NOT EXISTS (
        SELECT 1
        FROM pg_constraint
        WHERE conrelid = 'public.tasks'::regclass
          AND conname = 'fk_tasks_project'
          AND contype = 'f'
          AND confrelid = 'public.projects'::regclass
          AND confdeltype = 'n'
    ) THEN
        RAISE EXCEPTION
            'Validation failed for fk_tasks_project.';
    END IF;

    IF NOT EXISTS (
        SELECT 1
        FROM pg_constraint
        WHERE conrelid = 'public.tasks'::regclass
          AND conname = 'fk_tasks_assignee'
          AND contype = 'f'
          AND confrelid = 'public.collaborators'::regclass
          AND confdeltype = 'n'
    ) THEN
        RAISE EXCEPTION
            'Validation failed for fk_tasks_assignee.';
    END IF;

    IF NOT EXISTS (
        SELECT 1
        FROM pg_constraint
        WHERE conrelid = 'public.internal_programs'::regclass
          AND conname = 'fk_internal_programs_manager'
          AND contype = 'f'
          AND confrelid = 'public.collaborators'::regclass
          AND confdeltype = 'n'
    ) THEN
        RAISE EXCEPTION
            'Validation failed for fk_internal_programs_manager.';
    END IF;
END
$validation$;

COMMIT;