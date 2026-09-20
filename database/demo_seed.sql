-- DEMO / LOCAL DEVELOPMENT ONLY. Public passwords; never run in production.
-- Run database/devflow_hub.sql first. This seed is restricted to devflow_demo.
-- Re-running preserves existing demo passwords, roles and edited content.
BEGIN;
DO $$ BEGIN
    IF current_database() <> 'devflow_demo' THEN
        RAISE EXCEPTION 'Demo seed requires a dedicated database named devflow_demo';
    END IF;
END $$;

INSERT INTO collaborators (name, email, password, role, system_role, active)
SELECT name, email, '{bcrypt}' || crypt('DevFlowDemo-2026!', gen_salt('bf', 10)), role, system_role, TRUE
FROM (VALUES
    ('Demo Owner', 'owner@demo.example', 'Software Developer', 'USER'),
    ('Demo Manager', 'manager@demo.example', 'Team Lead', 'USER'),
    ('Demo Contributor', 'contributor@demo.example', 'Junior Developer', 'USER'),
    ('Demo Viewer', 'viewer@demo.example', 'Reviewer', 'USER'),
    ('Demo Outsider', 'outsider@demo.example', 'Developer', 'USER'),
    ('Demo Administrator', 'admin@demo.example', 'System Administrator', 'ADMIN')
) AS demo(name, email, role, system_role)
ON CONFLICT (email) DO NOTHING;

INSERT INTO projects (name, description, status, manager_id)
SELECT 'Interview Workspace', 'A local walkthrough of collaboration, tasks and document permissions.', 'IN_PROGRESS', id
FROM collaborators WHERE email = 'owner@demo.example'
AND NOT EXISTS (SELECT 1 FROM projects WHERE name = 'Interview Workspace');
INSERT INTO projects (name, description, status, manager_id)
SELECT 'Private Workspace', 'Only the outsider account can access this project.', 'PLANNED', id
FROM collaborators WHERE email = 'outsider@demo.example'
AND NOT EXISTS (SELECT 1 FROM projects WHERE name = 'Private Workspace');

INSERT INTO project_memberships (project_id, collaborator_id, role, status)
SELECT p.id, c.id, demo.role, 'ACTIVE'
FROM (VALUES
    ('Interview Workspace', 'owner@demo.example', 'OWNER'),
    ('Interview Workspace', 'manager@demo.example', 'MANAGER'),
    ('Interview Workspace', 'contributor@demo.example', 'CONTRIBUTOR'),
    ('Interview Workspace', 'viewer@demo.example', 'VIEWER'),
    ('Private Workspace', 'outsider@demo.example', 'OWNER')
) AS demo(project_name, email, role)
JOIN projects p ON p.name = demo.project_name
JOIN collaborators c ON c.email = demo.email
ON CONFLICT (project_id, collaborator_id) DO NOTHING;

INSERT INTO tasks (title, description, status, priority, project_id, assignee_id)
SELECT demo.title, demo.description, demo.status, demo.priority, p.id, c.id
FROM (VALUES
    ('Review resource permissions', 'Compare OWNER, CONTRIBUTOR and VIEWER access.', 'IN_PROGRESS', 'HIGH', 'owner@demo.example'),
    ('Prepare interview walkthrough', 'Create and edit a task, then run the document API demonstration.', 'PENDING', 'MEDIUM', 'contributor@demo.example'),
    ('Verify local setup', 'PostgreSQL schema, backend and frontend run locally.', 'COMPLETED', 'LOW', 'manager@demo.example')
) AS demo(title, description, status, priority, email)
JOIN projects p ON p.name = 'Interview Workspace'
JOIN collaborators c ON c.email = demo.email
WHERE NOT EXISTS (SELECT 1 FROM tasks t WHERE t.title = demo.title AND t.project_id = p.id);

INSERT INTO documents (title, content, project_id)
SELECT 'Architecture notes', 'React calls the Spring Boot API. PostgreSQL stores projects, tasks, documents and memberships.', id
FROM projects p WHERE name = 'Interview Workspace'
AND NOT EXISTS (SELECT 1 FROM documents d WHERE d.project_id = p.id AND d.title = 'Architecture notes');
INSERT INTO documents (title, content, task_id)
SELECT 'Task checklist', 'Confirm allowed reads and rejected unauthorized updates before the interview.', id
FROM tasks t WHERE title = 'Review resource permissions'
AND NOT EXISTS (SELECT 1 FROM documents d WHERE d.task_id = t.id AND d.title = 'Task checklist');
COMMIT;
