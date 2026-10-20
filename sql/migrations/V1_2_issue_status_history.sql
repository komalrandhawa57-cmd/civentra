DECLARE
    object_count NUMBER;
    next_issue_id NUMBER;
BEGIN
    SELECT COUNT(*) INTO object_count FROM user_sequences WHERE sequence_name = 'CATEGORY_SEQ';
    IF object_count = 0 THEN
        SELECT NVL(MAX(category_id), 0) + 1 INTO next_issue_id FROM issue_categories;
        EXECUTE IMMEDIATE 'CREATE SEQUENCE category_seq START WITH ' || next_issue_id || ' INCREMENT BY 1';
    END IF;
    SELECT COUNT(*) INTO object_count FROM user_sequences WHERE sequence_name = 'ISSUE_SEQ';
    IF object_count = 0 THEN
        SELECT NVL(MAX(issue_id), 0) + 1 INTO next_issue_id FROM community_issues;
        EXECUTE IMMEDIATE 'CREATE SEQUENCE issue_seq START WITH ' || next_issue_id || ' INCREMENT BY 1';
    END IF;

    SELECT COUNT(*) INTO object_count FROM user_tab_columns WHERE table_name = 'COMMUNITY_ISSUES' AND column_name = 'PROVIDER_ID';
    IF object_count = 0 THEN EXECUTE IMMEDIATE 'ALTER TABLE community_issues ADD (provider_id NUMBER)'; END IF;
    SELECT COUNT(*) INTO object_count FROM user_tab_columns WHERE table_name = 'COMMUNITY_ISSUES' AND column_name = 'OFFICER_NOTES';
    IF object_count = 0 THEN EXECUTE IMMEDIATE 'ALTER TABLE community_issues ADD (officer_notes VARCHAR2(1000))'; END IF;
    SELECT COUNT(*) INTO object_count FROM user_tab_columns WHERE table_name = 'COMMUNITY_ISSUES' AND column_name = 'ASSIGNED_AT';
    IF object_count = 0 THEN EXECUTE IMMEDIATE 'ALTER TABLE community_issues ADD (assigned_at TIMESTAMP)'; END IF;

    SELECT COUNT(*) INTO object_count FROM user_sequences WHERE sequence_name = 'ISSUE_STATUS_HISTORY_SEQ';
    IF object_count = 0 THEN
        EXECUTE IMMEDIATE 'CREATE SEQUENCE issue_status_history_seq START WITH 1 INCREMENT BY 1';
    END IF;

    SELECT COUNT(*) INTO object_count FROM user_tables WHERE table_name = 'ISSUE_STATUS_HISTORY';
    IF object_count = 0 THEN
        EXECUTE IMMEDIATE 'CREATE TABLE issue_status_history (
            history_id NUMBER PRIMARY KEY,
            issue_id NUMBER NOT NULL,
            from_status VARCHAR2(30) NOT NULL,
            to_status VARCHAR2(30) NOT NULL,
            changed_by NUMBER,
            notes VARCHAR2(1000),
            changed_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP NOT NULL,
            CONSTRAINT fk_issue_status_history_issue FOREIGN KEY (issue_id) REFERENCES community_issues(issue_id),
            CONSTRAINT fk_issue_status_history_user FOREIGN KEY (changed_by) REFERENCES users(user_id)
        )';
    END IF;

    SELECT COUNT(*) INTO object_count FROM user_indexes WHERE index_name = 'IX_ISSUE_STATUS_HISTORY_ISSUE';
    IF object_count = 0 THEN
        EXECUTE IMMEDIATE 'CREATE INDEX ix_issue_status_history_issue ON issue_status_history(issue_id, changed_at)';
    END IF;
END;
/
