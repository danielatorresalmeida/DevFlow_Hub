\set ON_ERROR_STOP on
-- PostgreSQL regression test: only a session-local temporary table is changed.
CREATE TEMP TABLE collaborators (id INTEGER PRIMARY KEY, email TEXT NOT NULL);
INSERT INTO collaborators VALUES (1, 'legacy@example.test'), (2, 'second@example.test');
\ir ../migrations/20260801_add_system_roles.sql
DO $$ BEGIN
    IF (SELECT count(*) FROM collaborators WHERE system_role = 'USER') <> 2 THEN
        RAISE EXCEPTION 'Existing accounts must migrate to USER';
    END IF;
END $$;
UPDATE collaborators SET system_role = 'ADMIN' WHERE id = 1;
\ir ../migrations/20260801_add_system_roles.sql
INSERT INTO collaborators (id, email) VALUES (3, 'default@example.test');
DO $$ BEGIN
    IF (SELECT system_role FROM collaborators WHERE id = 1) <> 'ADMIN'
       OR (SELECT system_role FROM collaborators WHERE id = 3) <> 'USER' THEN
        RAISE EXCEPTION 'Rerun must preserve ADMIN and default new accounts to USER';
    END IF;
    BEGIN
        INSERT INTO collaborators VALUES (4, 'invalid@example.test', 'SUPERADMIN');
        RAISE EXCEPTION 'Invalid role was accepted';
    EXCEPTION WHEN check_violation THEN NULL;
    END;
    BEGIN
        INSERT INTO collaborators VALUES (5, 'null@example.test', NULL);
        RAISE EXCEPTION 'Null role was accepted';
    EXCEPTION WHEN not_null_violation THEN NULL;
    END;
END $$;
SELECT 'System role migration checks passed' AS result;
