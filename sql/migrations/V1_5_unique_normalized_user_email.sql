-- Prevent concurrent registrations from creating duplicate email addresses.
-- Existing duplicate addresses must be resolved by an administrator before applying.
DECLARE
    index_count NUMBER;
    duplicate_count NUMBER;
BEGIN
    SELECT COUNT(*) INTO index_count
      FROM user_indexes
     WHERE index_name = 'UX_USERS_EMAIL_NORMALIZED'
       AND table_name = 'USERS';

    IF index_count = 0 THEN
        SELECT COUNT(*) INTO duplicate_count
          FROM (
                SELECT LOWER(TRIM(email)) normalized_email
                  FROM users
                 WHERE email IS NOT NULL
                 GROUP BY LOWER(TRIM(email))
                HAVING COUNT(*) > 1
          );

        IF duplicate_count > 0 THEN
            RAISE_APPLICATION_ERROR(-20051,
                'Cannot create normalized email index: duplicate user emails must be resolved first.');
        END IF;

        EXECUTE IMMEDIATE
            'CREATE UNIQUE INDEX ux_users_email_normalized ON users (LOWER(TRIM(email)))';
    END IF;
END;
/
