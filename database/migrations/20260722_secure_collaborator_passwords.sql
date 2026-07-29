-- ============================================================================
-- DevFlow Hub - Secure collaborator password migration
--
-- This migration is idempotent and must be executed manually because this
-- project does not currently use Flyway or Liquibase.
--
-- Behaviour:
--   * NULL or blank passwords receive a unique, unrecoverable random password.
--   * Direct BCrypt hashes receive the {bcrypt} DelegatingPasswordEncoder prefix.
--   * Legacy unprefixed values are re-hashed while preserving their current
--     plaintext value as the user's password.
--   * The password column becomes NOT NULL.
--   * A format constraint prevents new unprefixed values.
-- ============================================================================

BEGIN;

CREATE EXTENSION IF NOT EXISTS pgcrypto;

LOCK TABLE collaborators IN SHARE ROW EXCLUSIVE MODE;

-- Accounts without a password receive unique random credentials. The generated
-- plaintext is intentionally discarded, so these accounts require a later
-- password reset before they can authenticate.
UPDATE collaborators
SET password = '{bcrypt}' || crypt(
    encode(gen_random_bytes(32), 'hex'),
    gen_salt('bf', 10)
)
WHERE password IS NULL
   OR BTRIM(password) = '';

-- Preserve already encoded direct BCrypt values by adding only the encoder id.
UPDATE collaborators
SET password = '{bcrypt}' || password
WHERE password ~ '^\$2[aby]\$[0-9]{2}\$';

-- Re-hash legacy unprefixed values. Already prefixed values are left unchanged,
-- which makes this migration safe to execute more than once.
UPDATE collaborators
SET password = '{bcrypt}' || crypt(password, gen_salt('bf', 10))
WHERE password IS NOT NULL
  AND BTRIM(password) <> ''
  AND password !~ '^\{[A-Za-z0-9_-]+\}.+'
  AND password !~ '^\$2[aby]\$[0-9]{2}\$';

ALTER TABLE collaborators
    ALTER COLUMN password SET NOT NULL;

DO $migration$
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
$migration$;

DO $validation$
DECLARE
    invalid_count INTEGER;
BEGIN
    SELECT COUNT(*)
    INTO invalid_count
    FROM collaborators
    WHERE password IS NULL
       OR BTRIM(password) = ''
       OR password !~ '^\{[A-Za-z0-9_-]+\}.+';

    IF invalid_count > 0 THEN
        RAISE EXCEPTION
            'Password migration validation failed: % invalid rows remain.',
            invalid_count;
    END IF;
END
$validation$;

COMMIT;
