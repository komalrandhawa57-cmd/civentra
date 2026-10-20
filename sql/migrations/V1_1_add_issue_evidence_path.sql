DECLARE
    column_count NUMBER;
BEGIN
    SELECT COUNT(*)
      INTO column_count
      FROM user_tab_columns
     WHERE table_name = 'COMMUNITY_ISSUES'
       AND column_name = 'EVIDENCE_PATH';

    IF column_count = 0 THEN
        EXECUTE IMMEDIATE 'ALTER TABLE community_issues ADD (evidence_path VARCHAR2(512))';
    END IF;
END;
/
