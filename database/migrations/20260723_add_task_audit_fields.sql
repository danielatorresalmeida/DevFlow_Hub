-- ============================================================================
-- DevFlow Hub - Add task audit fields
--
-- This migration is idempotent and can be executed on an existing PostgreSQL
-- database. It adds server-managed creation and update timestamps to tasks.
-- ============================================================================

BEGIN;

ALTER TABLE tasks
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

DO $validation$
DECLARE
    invalid_count INTEGER;
BEGIN
    SELECT COUNT(*)
    INTO invalid_count
    FROM tasks
    WHERE created_at IS NULL
       OR updated_at IS NULL;

    IF invalid_count > 0 THEN
        RAISE EXCEPTION
            'Task audit migration validation failed: % invalid rows remain.',
            invalid_count;
    END IF;
END
$validation$;

COMMIT;
