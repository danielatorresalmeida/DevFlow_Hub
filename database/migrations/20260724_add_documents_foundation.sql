-- ============================================================================
-- DevFlow Hub - Add documents and attachments foundation
--
-- This migration is idempotent and must be executed manually because the
-- project does not currently use Flyway or Liquibase.
--
-- Ownership rules:
--   * A document belongs to exactly one project or exactly one task.
--   * A task-owned document stores only task_id. Its project is derived
--     through the task relationship.
--   * Attachments store metadata only. Physical file content is not stored
--     in PostgreSQL.
-- ============================================================================

BEGIN;

-- 1. Main tables

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

-- 2. Foreign keys

ALTER TABLE documents
    DROP CONSTRAINT IF EXISTS fk_documents_project;

ALTER TABLE documents
    ADD CONSTRAINT fk_documents_project
    FOREIGN KEY (project_id)
    REFERENCES projects(id)
    ON DELETE CASCADE;

ALTER TABLE documents
    DROP CONSTRAINT IF EXISTS fk_documents_task;

ALTER TABLE documents
    ADD CONSTRAINT fk_documents_task
    FOREIGN KEY (task_id)
    REFERENCES tasks(id)
    ON DELETE CASCADE;

ALTER TABLE attachments
    DROP CONSTRAINT IF EXISTS fk_attachments_document;

ALTER TABLE attachments
    ADD CONSTRAINT fk_attachments_document
    FOREIGN KEY (document_id)
    REFERENCES documents(id)
    ON DELETE CASCADE;

-- 3. Domain constraints

ALTER TABLE documents
    DROP CONSTRAINT IF EXISTS documents_owner_check;

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
    );

ALTER TABLE documents
    DROP CONSTRAINT IF EXISTS documents_title_not_blank_check;

ALTER TABLE documents
    ADD CONSTRAINT documents_title_not_blank_check
    CHECK (BTRIM(title) <> '');

ALTER TABLE attachments
    DROP CONSTRAINT IF EXISTS attachments_filename_not_blank_check;

ALTER TABLE attachments
    ADD CONSTRAINT attachments_filename_not_blank_check
    CHECK (BTRIM(original_filename) <> '');

ALTER TABLE attachments
    DROP CONSTRAINT IF EXISTS attachments_storage_key_not_blank_check;

ALTER TABLE attachments
    ADD CONSTRAINT attachments_storage_key_not_blank_check
    CHECK (BTRIM(storage_key) <> '');

ALTER TABLE attachments
    DROP CONSTRAINT IF EXISTS attachments_size_check;

ALTER TABLE attachments
    ADD CONSTRAINT attachments_size_check
    CHECK (size_bytes >= 0);

-- 4. Indexes

CREATE INDEX IF NOT EXISTS idx_documents_project_id
    ON documents(project_id);

CREATE INDEX IF NOT EXISTS idx_documents_task_id
    ON documents(task_id);

CREATE INDEX IF NOT EXISTS idx_attachments_document_id
    ON attachments(document_id);

CREATE UNIQUE INDEX IF NOT EXISTS ux_attachments_storage_key
    ON attachments(storage_key);

-- 5. Validation

DO $validation$
DECLARE
    invalid_document_count BIGINT;
    invalid_attachment_count BIGINT;
BEGIN
    SELECT COUNT(*)
    INTO invalid_document_count
    FROM documents
    WHERE BTRIM(title) = ''
       OR (
            project_id IS NULL
            AND task_id IS NULL
       )
       OR (
            project_id IS NOT NULL
            AND task_id IS NOT NULL
       );

    IF invalid_document_count > 0 THEN
        RAISE EXCEPTION
            'Document migration validation failed: % invalid documents.',
            invalid_document_count;
    END IF;

    SELECT COUNT(*)
    INTO invalid_attachment_count
    FROM attachments
    WHERE BTRIM(original_filename) = ''
       OR BTRIM(storage_key) = ''
       OR size_bytes < 0;

    IF invalid_attachment_count > 0 THEN
        RAISE EXCEPTION
            'Attachment migration validation failed: % invalid attachments.',
            invalid_attachment_count;
    END IF;
END
$validation$;

COMMIT;