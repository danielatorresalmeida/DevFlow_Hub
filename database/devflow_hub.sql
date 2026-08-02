-- =============================================================
-- DevFlow Hub - PostgreSQL setup script
-- This script can be run more than once without deleting data.
-- =============================================================

BEGIN;

-- Required for BCrypt hashing during controlled password migrations.
CREATE EXTENSION IF NOT EXISTS pgcrypto;

-- 1. Main tables
CREATE TABLE IF NOT EXISTS collaborators (
    id BIGSERIAL PRIMARY KEY,
    name VARCHAR(150) NOT NULL,
    email VARCHAR(150) NOT NULL UNIQUE,
    password VARCHAR(255) NOT NULL,
    role VARCHAR(100) NOT NULL,
    system_role VARCHAR(20) NOT NULL DEFAULT 'USER',
    active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS projects (
    id BIGSERIAL PRIMARY KEY,
    name VARCHAR(150) NOT NULL,
    description TEXT,
    status VARCHAR(50) NOT NULL DEFAULT 'PLANNED',
    start_date DATE,
    end_date DATE,
    manager_id BIGINT
);

CREATE TABLE IF NOT EXISTS tasks (
    id BIGSERIAL PRIMARY KEY,
    title VARCHAR(150) NOT NULL,
    description TEXT,
    status VARCHAR(50) NOT NULL DEFAULT 'PENDING',
    priority VARCHAR(50) NOT NULL DEFAULT 'MEDIUM',
    project_id BIGINT,
    assignee_id BIGINT,
    total_time_seconds BIGINT NOT NULL DEFAULT 0,
    timer_active BOOLEAN NOT NULL DEFAULT FALSE,
    timer_started_at TIMESTAMP,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS internal_programs (
    id BIGSERIAL PRIMARY KEY,
    name VARCHAR(150) NOT NULL,
    description TEXT,
    area VARCHAR(100),
    status VARCHAR(50) NOT NULL DEFAULT 'PLANNED',
    start_date DATE,
    end_date DATE,
    manager_id BIGINT
);

CREATE TABLE IF NOT EXISTS documents (
    id BIGSERIAL PRIMARY KEY,
    title VARCHAR(150) NOT NULL,
    content TEXT,
    project_id BIGINT,
    task_id BIGINT,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS attachments (
    id BIGSERIAL PRIMARY KEY,
    document_id BIGINT NOT NULL,
    original_filename VARCHAR(255) NOT NULL,
    storage_key VARCHAR(500) NOT NULL,
    content_type VARCHAR(150),
    size_bytes BIGINT NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

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

-- 2. Columns added during later project iterations
ALTER TABLE collaborators
    ADD COLUMN IF NOT EXISTS password VARCHAR(255),
    ADD COLUMN IF NOT EXISTS system_role VARCHAR(20) DEFAULT 'USER',
    ADD COLUMN IF NOT EXISTS active BOOLEAN DEFAULT TRUE,
    ADD COLUMN IF NOT EXISTS created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP;

ALTER TABLE projects
    ADD COLUMN IF NOT EXISTS manager_id BIGINT;

ALTER TABLE tasks
    ADD COLUMN IF NOT EXISTS project_id BIGINT,
    ADD COLUMN IF NOT EXISTS assignee_id BIGINT,
    ADD COLUMN IF NOT EXISTS total_time_seconds BIGINT DEFAULT 0,
    ADD COLUMN IF NOT EXISTS timer_active BOOLEAN DEFAULT FALSE,
    ADD COLUMN IF NOT EXISTS timer_started_at TIMESTAMP,
    ADD COLUMN IF NOT EXISTS created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    ADD COLUMN IF NOT EXISTS updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP;

ALTER TABLE internal_programs
    ADD COLUMN IF NOT EXISTS area VARCHAR(100),
    ADD COLUMN IF NOT EXISTS manager_id BIGINT;

-- 3. Safe defaults for existing rows
-- Secure existing password values before applying NOT NULL.
UPDATE collaborators
SET password = '{bcrypt}' || crypt(
    encode(gen_random_bytes(32), 'hex'),
    gen_salt('bf', 10)
)
WHERE password IS NULL
   OR BTRIM(password) = '';

UPDATE collaborators
SET password = '{bcrypt}' || password
WHERE password ~ '^\[aby]\$[0-9]{2}\$';

UPDATE collaborators
SET password = '{bcrypt}' || crypt(password, gen_salt('bf', 10))
WHERE password IS NOT NULL
  AND BTRIM(password) <> ''
  AND password !~ '^\{[A-Za-z0-9_-]+\}.+'
  AND password !~ '^\[aby]\$[0-9]{2}\$';
UPDATE collaborators SET system_role = 'USER' WHERE system_role IS NULL;
UPDATE collaborators SET active = TRUE WHERE active IS NULL;
UPDATE collaborators SET created_at = CURRENT_TIMESTAMP WHERE created_at IS NULL;
UPDATE tasks SET total_time_seconds = 0 WHERE total_time_seconds IS NULL;
UPDATE tasks SET timer_active = FALSE WHERE timer_active IS NULL;
UPDATE tasks SET created_at = CURRENT_TIMESTAMP WHERE created_at IS NULL;
UPDATE tasks SET updated_at = CURRENT_TIMESTAMP WHERE updated_at IS NULL;

ALTER TABLE collaborators
    ALTER COLUMN password SET NOT NULL,
    ALTER COLUMN system_role SET DEFAULT 'USER',
    ALTER COLUMN system_role SET NOT NULL,
    ALTER COLUMN active SET DEFAULT TRUE,
    ALTER COLUMN active SET NOT NULL,
    ALTER COLUMN created_at SET DEFAULT CURRENT_TIMESTAMP,
    ALTER COLUMN created_at SET NOT NULL;

DO $password_constraint$
BEGIN
    IF NOT EXISTS (
        SELECT 1
        FROM pg_constraint
        WHERE conrelid = 'public.collaborators'::regclass
          AND conname = 'collaborators_password_format_check'
    ) THEN
        ALTER TABLE collaborators
            ADD CONSTRAINT collaborators_password_format_check
            CHECK (password ~ '^\{[A-Za-z0-9_-]+\}.+');
    END IF;
END
$password_constraint$;

DO $system_role_constraint$
BEGIN
    IF NOT EXISTS (
        SELECT 1
        FROM pg_constraint
        WHERE conrelid = 'public.collaborators'::regclass
          AND conname = 'collaborators_system_role_check'
    ) THEN
        ALTER TABLE collaborators
            ADD CONSTRAINT collaborators_system_role_check
            CHECK (system_role IN ('USER', 'ADMIN'));
    END IF;
END
$system_role_constraint$;

ALTER TABLE tasks
    ALTER COLUMN total_time_seconds SET DEFAULT 0,
    ALTER COLUMN total_time_seconds SET NOT NULL,
    ALTER COLUMN timer_active SET DEFAULT FALSE,
    ALTER COLUMN timer_active SET NOT NULL,
    ALTER COLUMN created_at SET DEFAULT CURRENT_TIMESTAMP,
    ALTER COLUMN created_at SET NOT NULL,
    ALTER COLUMN updated_at SET DEFAULT CURRENT_TIMESTAMP,
    ALTER COLUMN updated_at SET NOT NULL;

-- 4. Foreign keys
ALTER TABLE projects DROP CONSTRAINT IF EXISTS projects_manager_id_fkey;
ALTER TABLE projects DROP CONSTRAINT IF EXISTS fk_projects_manager;
ALTER TABLE projects
    ADD CONSTRAINT fk_projects_manager
    FOREIGN KEY (manager_id) REFERENCES collaborators(id) ON DELETE SET NULL;

ALTER TABLE tasks DROP CONSTRAINT IF EXISTS tasks_project_id_fkey;
ALTER TABLE tasks DROP CONSTRAINT IF EXISTS fk_tasks_project;
ALTER TABLE tasks
    ADD CONSTRAINT fk_tasks_project
    FOREIGN KEY (project_id) REFERENCES projects(id) ON DELETE SET NULL;

ALTER TABLE tasks DROP CONSTRAINT IF EXISTS tasks_assignee_id_fkey;
ALTER TABLE tasks DROP CONSTRAINT IF EXISTS fk_tasks_assignee;
ALTER TABLE tasks
    ADD CONSTRAINT fk_tasks_assignee
    FOREIGN KEY (assignee_id) REFERENCES collaborators(id) ON DELETE SET NULL;

ALTER TABLE internal_programs DROP CONSTRAINT IF EXISTS internal_programs_manager_id_fkey;
ALTER TABLE internal_programs DROP CONSTRAINT IF EXISTS fk_internal_programs_manager;
ALTER TABLE internal_programs
    ADD CONSTRAINT fk_internal_programs_manager
    FOREIGN KEY (manager_id) REFERENCES collaborators(id) ON DELETE SET NULL;

ALTER TABLE documents DROP CONSTRAINT IF EXISTS fk_documents_project;
ALTER TABLE documents
    ADD CONSTRAINT fk_documents_project
    FOREIGN KEY (project_id) REFERENCES projects(id) ON DELETE CASCADE;

ALTER TABLE documents DROP CONSTRAINT IF EXISTS fk_documents_task;
ALTER TABLE documents
    ADD CONSTRAINT fk_documents_task
    FOREIGN KEY (task_id) REFERENCES tasks(id) ON DELETE CASCADE;

ALTER TABLE attachments DROP CONSTRAINT IF EXISTS fk_attachments_document;
ALTER TABLE attachments
    ADD CONSTRAINT fk_attachments_document
    FOREIGN KEY (document_id) REFERENCES documents(id) ON DELETE CASCADE;

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

-- 5. Domain constraints
ALTER TABLE projects DROP CONSTRAINT IF EXISTS projects_status_check;
ALTER TABLE projects DROP CONSTRAINT IF EXISTS projects_dates_check;
ALTER TABLE projects
    ADD CONSTRAINT projects_status_check
        CHECK (status IN ('PLANNED', 'IN_PROGRESS', 'COMPLETED')),
    ADD CONSTRAINT projects_dates_check
        CHECK (end_date IS NULL OR start_date IS NULL OR end_date >= start_date);

ALTER TABLE tasks DROP CONSTRAINT IF EXISTS tasks_status_check;
ALTER TABLE tasks DROP CONSTRAINT IF EXISTS tasks_priority_check;
ALTER TABLE tasks DROP CONSTRAINT IF EXISTS tasks_time_check;
ALTER TABLE tasks
    ADD CONSTRAINT tasks_status_check
        CHECK (status IN ('PENDING', 'IN_PROGRESS', 'REVIEW', 'COMPLETED')),
    ADD CONSTRAINT tasks_priority_check
        CHECK (priority IN ('LOW', 'MEDIUM', 'HIGH')),
    ADD CONSTRAINT tasks_time_check
        CHECK (total_time_seconds >= 0);

ALTER TABLE internal_programs DROP CONSTRAINT IF EXISTS programs_status_check;
ALTER TABLE internal_programs DROP CONSTRAINT IF EXISTS programs_dates_check;
ALTER TABLE internal_programs
    ADD CONSTRAINT programs_status_check
        CHECK (status IN ('PLANNED', 'ACTIVE', 'COMPLETED')),
    ADD CONSTRAINT programs_dates_check
        CHECK (end_date IS NULL OR start_date IS NULL OR end_date >= start_date);

ALTER TABLE documents DROP CONSTRAINT IF EXISTS documents_owner_check;
ALTER TABLE documents DROP CONSTRAINT IF EXISTS documents_title_not_blank_check;
ALTER TABLE documents
    ADD CONSTRAINT documents_owner_check
        CHECK (
            (
                project_id IS NOT NULL
                AND task_id IS NULL
            )
            OR
            (
                project_id IS NULL
                AND task_id IS NOT NULL
            )
        ),
    ADD CONSTRAINT documents_title_not_blank_check
        CHECK (BTRIM(title) <> '');

ALTER TABLE attachments
    DROP CONSTRAINT IF EXISTS attachments_filename_not_blank_check;

ALTER TABLE attachments
    DROP CONSTRAINT IF EXISTS attachments_storage_key_not_blank_check;

ALTER TABLE attachments
    DROP CONSTRAINT IF EXISTS attachments_size_check;

ALTER TABLE attachments
    ADD CONSTRAINT attachments_filename_not_blank_check
        CHECK (BTRIM(original_filename) <> ''),
    ADD CONSTRAINT attachments_storage_key_not_blank_check
        CHECK (BTRIM(storage_key) <> ''),
    ADD CONSTRAINT attachments_size_check
        CHECK (size_bytes >= 0);

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

-- 6. Indexes
CREATE UNIQUE INDEX IF NOT EXISTS ux_collaborators_email_lower ON collaborators(LOWER(email));
CREATE INDEX IF NOT EXISTS idx_projects_manager_id ON projects(manager_id);
CREATE INDEX IF NOT EXISTS idx_tasks_project_id ON tasks(project_id);
CREATE INDEX IF NOT EXISTS idx_tasks_assignee_id ON tasks(assignee_id);
CREATE INDEX IF NOT EXISTS idx_tasks_status ON tasks(status);
CREATE INDEX IF NOT EXISTS idx_internal_programs_manager_id ON internal_programs(manager_id);
CREATE INDEX IF NOT EXISTS idx_documents_project_id ON documents(project_id);
CREATE INDEX IF NOT EXISTS idx_documents_task_id ON documents(task_id);
CREATE INDEX IF NOT EXISTS idx_attachments_document_id ON attachments(document_id);
CREATE UNIQUE INDEX IF NOT EXISTS ux_attachments_storage_key ON attachments(storage_key);

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

-- 7. Demonstration data
-- BCrypt hashes are generated for local demonstration accounts.
INSERT INTO collaborators (name, email, password, role, system_role, active)
VALUES
    ('Ana Silva', 'ana.silva@example.com', '{bcrypt}' || crypt('password123', gen_salt('bf', 10)), 'Frontend Developer', 'USER', TRUE),
    ('Bruno Costa', 'bruno.costa@example.com', '{bcrypt}' || crypt('password123', gen_salt('bf', 10)), 'Backend Developer', 'USER', TRUE),
    ('Carla Mendes', 'carla.mendes@example.com', '{bcrypt}' || crypt('password123', gen_salt('bf', 10)), 'Project Manager', 'ADMIN', TRUE)
ON CONFLICT (email) DO NOTHING;

INSERT INTO projects (name, description, status, start_date, end_date, manager_id)
SELECT
    'DevFlow Hub MVP',
    'Initial version of the DevFlow Hub academic project.',
    'IN_PROGRESS',
    DATE '2026-05-18',
    NULL,
    (SELECT id FROM collaborators WHERE email = 'carla.mendes@example.com')
WHERE NOT EXISTS (
    SELECT 1 FROM projects WHERE name = 'DevFlow Hub MVP'
);

INSERT INTO projects (name, description, status, start_date, end_date, manager_id)
SELECT
    'Agile Dashboard',
    'Dashboard for project and task visibility.',
    'PLANNED',
    DATE '2026-06-01',
    NULL,
    (SELECT id FROM collaborators WHERE email = 'carla.mendes@example.com')
WHERE NOT EXISTS (
    SELECT 1 FROM projects WHERE name = 'Agile Dashboard'
);

INSERT INTO tasks (title, description, status, priority, project_id, assignee_id)
SELECT
    'Create collaborator page',
    'Build the collaborator management page.',
    'COMPLETED',
    'HIGH',
    (SELECT id FROM projects WHERE name = 'DevFlow Hub MVP' ORDER BY id LIMIT 1),
    (SELECT id FROM collaborators WHERE email = 'ana.silva@example.com')
WHERE NOT EXISTS (
    SELECT 1 FROM tasks WHERE title = 'Create collaborator page'
);

INSERT INTO tasks (title, description, status, priority, project_id, assignee_id)
SELECT
    'Create task timer',
    'Implement start, pause, resume, and total-time actions.',
    'IN_PROGRESS',
    'HIGH',
    (SELECT id FROM projects WHERE name = 'DevFlow Hub MVP' ORDER BY id LIMIT 1),
    (SELECT id FROM collaborators WHERE email = 'bruno.costa@example.com')
WHERE NOT EXISTS (
    SELECT 1 FROM tasks WHERE title = 'Create task timer'
);

INSERT INTO tasks (title, description, status, priority, project_id, assignee_id)
SELECT
    'Prepare final documentation',
    'Write the academic documentation for the final delivery.',
    'PENDING',
    'MEDIUM',
    (SELECT id FROM projects WHERE name = 'Agile Dashboard' ORDER BY id LIMIT 1),
    (SELECT id FROM collaborators WHERE email = 'carla.mendes@example.com')
WHERE NOT EXISTS (
    SELECT 1 FROM tasks WHERE title = 'Prepare final documentation'
);

-- Project memberships derived from demonstration data

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

INSERT INTO internal_programs (
    name,
    description,
    area,
    status,
    start_date,
    end_date,
    manager_id
)
SELECT
    'Frontend Onboarding',
    'Internal onboarding program for frontend contributors.',
    'Frontend',
    'ACTIVE',
    DATE '2026-05-20',
    NULL,
    (SELECT id FROM collaborators WHERE email = 'ana.silva@example.com')
WHERE NOT EXISTS (
    SELECT 1 FROM internal_programs WHERE name = 'Frontend Onboarding'
);

INSERT INTO internal_programs (
    name,
    description,
    area,
    status,
    start_date,
    end_date,
    manager_id
)
SELECT
    'Backend API Training',
    'Internal training program focused on Spring Boot APIs.',
    'Backend',
    'PLANNED',
    DATE '2026-06-05',
    NULL,
    (SELECT id FROM collaborators WHERE email = 'bruno.costa@example.com')
WHERE NOT EXISTS (
    SELECT 1 FROM internal_programs WHERE name = 'Backend API Training'
);

COMMIT;
