DECLARE
    object_count NUMBER;
    next_history_id NUMBER;
BEGIN
    SELECT COUNT(*) INTO object_count FROM user_tab_columns
     WHERE table_name = 'SERVICE_REQUESTS' AND column_name = 'COMPLETION_NOTES';
    IF object_count = 0 THEN
        EXECUTE IMMEDIATE 'ALTER TABLE service_requests ADD (completion_notes VARCHAR2(2000))';
    END IF;

    SELECT COUNT(*) INTO object_count FROM user_tab_columns
     WHERE table_name = 'SERVICE_REQUESTS' AND column_name = 'UPDATED_AT';
    IF object_count = 0 THEN
        EXECUTE IMMEDIATE 'ALTER TABLE service_requests ADD (updated_at TIMESTAMP)';
        EXECUTE IMMEDIATE 'UPDATE service_requests SET updated_at = request_date WHERE updated_at IS NULL';
    END IF;

    SELECT COUNT(*) INTO object_count FROM user_tables WHERE table_name = 'SERVICE_REQUEST_STATUS_HISTORY';
    IF object_count = 0 THEN
        EXECUTE IMMEDIATE 'CREATE TABLE service_request_status_history (
            history_id NUMBER PRIMARY KEY,
            request_id NUMBER NOT NULL,
            from_status VARCHAR2(30),
            to_status VARCHAR2(30) NOT NULL,
            changed_by NUMBER,
            remarks VARCHAR2(2000),
            changed_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP NOT NULL,
            CONSTRAINT fk_service_request_history_request FOREIGN KEY (request_id) REFERENCES service_requests(request_id),
            CONSTRAINT fk_service_request_history_user FOREIGN KEY (changed_by) REFERENCES users(user_id)
        )';
    END IF;

    SELECT COUNT(*) INTO object_count FROM user_sequences WHERE sequence_name = 'SERVICE_REQUEST_HISTORY_SEQ';
    IF object_count = 0 THEN
        SELECT NVL(MAX(history_id), 0) + 1 INTO next_history_id FROM service_request_status_history;
        EXECUTE IMMEDIATE 'CREATE SEQUENCE service_request_history_seq START WITH ' || next_history_id || ' INCREMENT BY 1';
    END IF;

    SELECT COUNT(*) INTO object_count FROM user_indexes WHERE index_name = 'IX_SR_HISTORY_REQUEST';
    IF object_count = 0 THEN
        EXECUTE IMMEDIATE 'CREATE INDEX ix_sr_history_request ON service_request_status_history(request_id, changed_at)';
    END IF;
END;
/
