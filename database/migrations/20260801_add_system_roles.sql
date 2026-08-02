-- ============================================================================
-- DevFlow Hub - Add application-wide system roles
--
-- Project membership roles remain project-scoped. The new system_role column
-- is reserved for organization-level administration. Existing collaborators
-- are migrated to USER and must be promoted through a controlled process.
-- ============================================================================

BEGIN;

ALTER TABLE collaborators
    ADD COLUMN IF NOT EXISTS system_role VARCHAR(20);

UPDATE collaborators
SET system_role = 'USER'
WHERE system_role IS NULL;

ALTER TABLE collaborators
    ALTER COLUMN system_role SET DEFAULT 'USER',
    ALTER COLUMN system_role SET NOT NULL;

ALTER TABLE collaborators
    DROP CONSTRAINT IF EXISTS collaborators_system_role_check;

ALTER TABLE collaborators
    ADD CONSTRAINT collaborators_system_role_check
    CHECK (system_role IN ('USER', 'ADMIN'));

COMMIT;

-- After reviewing the chosen account, a database administrator may bootstrap
-- the first application administrator explicitly, for example:
-- UPDATE collaborators
-- SET system_role = 'ADMIN'
-- WHERE email = 'chosen.admin@example.com';
