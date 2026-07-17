-- =============================================================
-- DevFlow Hub - PostgreSQL setup script
-- This script can be run more than once without deleting data.
-- =============================================================

BEGIN;

-- 1. Main tables
CREATE TABLE IF NOT EXISTS collaborators (
    id BIGSERIAL PRIMARY KEY,
    name VARCHAR(150) NOT NULL,
    email VARCHAR(150) NOT NULL UNIQUE,
    password VARCHAR(255) NOT NULL,
    role VARCHAR(100) NOT NULL,
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

-- 2. Columns added during later project iterations
ALTER TABLE collaborators
    ADD COLUMN IF NOT EXISTS password VARCHAR(255),
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
UPDATE collaborators SET password = 'password123' WHERE password IS NULL OR BTRIM(password) = '';
UPDATE collaborators SET active = TRUE WHERE active IS NULL;
UPDATE collaborators SET created_at = CURRENT_TIMESTAMP WHERE created_at IS NULL;
UPDATE tasks SET total_time_seconds = 0 WHERE total_time_seconds IS NULL;
UPDATE tasks SET timer_active = FALSE WHERE timer_active IS NULL;
UPDATE tasks SET created_at = CURRENT_TIMESTAMP WHERE created_at IS NULL;
UPDATE tasks SET updated_at = CURRENT_TIMESTAMP WHERE updated_at IS NULL;

ALTER TABLE collaborators
    ALTER COLUMN password SET NOT NULL,
    ALTER COLUMN active SET DEFAULT TRUE,
    ALTER COLUMN active SET NOT NULL,
    ALTER COLUMN created_at SET DEFAULT CURRENT_TIMESTAMP,
    ALTER COLUMN created_at SET NOT NULL;

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

-- 6. Indexes
CREATE UNIQUE INDEX IF NOT EXISTS ux_collaborators_email_lower ON collaborators(LOWER(email));
CREATE INDEX IF NOT EXISTS idx_projects_manager_id ON projects(manager_id);
CREATE INDEX IF NOT EXISTS idx_tasks_project_id ON tasks(project_id);
CREATE INDEX IF NOT EXISTS idx_tasks_assignee_id ON tasks(assignee_id);
CREATE INDEX IF NOT EXISTS idx_tasks_status ON tasks(status);
CREATE INDEX IF NOT EXISTS idx_internal_programs_manager_id ON internal_programs(manager_id);

-- 7. Demonstration data
-- Plain-text passwords are used only for this academic local project.
INSERT INTO collaborators (name, email, password, role, active)
VALUES
    ('Ana Silva', 'ana.silva@example.com', 'password123', 'Frontend Developer', TRUE),
    ('Bruno Costa', 'bruno.costa@example.com', 'password123', 'Backend Developer', TRUE),
    ('Carla Mendes', 'carla.mendes@example.com', 'password123', 'Project Manager', TRUE)
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
