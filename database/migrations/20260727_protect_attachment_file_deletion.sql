-- ============================================================================
-- DevFlow Hub - Protect attachment files during parent deletion
--
-- Attachment metadata must not be deleted automatically without first running
-- the application storage cleanup. Parent deletion services now remove every
-- attachment explicitly before deleting its document.
--
-- ON DELETE RESTRICT acts as a database-level safety net against code paths
-- that bypass the coordinated file deletion workflow.
-- ============================================================================

BEGIN;

ALTER TABLE attachments
    DROP CONSTRAINT IF EXISTS attachments_document_id_fkey;

ALTER TABLE attachments
    DROP CONSTRAINT IF EXISTS fk_attachments_document;

ALTER TABLE attachments
    ADD CONSTRAINT fk_attachments_document
    FOREIGN KEY (document_id)
    REFERENCES documents(id)
    ON DELETE RESTRICT;

DO $validation$
BEGIN
    IF NOT EXISTS (
        SELECT 1
        FROM pg_constraint
        WHERE conrelid = 'public.attachments'::regclass
          AND conname = 'fk_attachments_document'
          AND contype = 'f'
          AND confrelid = 'public.documents'::regclass
          AND confdeltype = 'r'
    ) THEN
        RAISE EXCEPTION
            'Validation failed for fk_attachments_document.';
    END IF;
END
$validation$;

COMMIT;
