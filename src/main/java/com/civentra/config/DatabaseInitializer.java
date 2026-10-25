package com.civentra.config;

import jakarta.annotation.PostConstruct;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class DatabaseInitializer {

    private final JdbcTemplate jdbc;

    public DatabaseInitializer(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    @PostConstruct
    public void initSchema() {
        // Create ALL required Sequences in Oracle DB without changing account status.
        String[] sequences = {
            "ISSUE_SEQ", "SEQ_COMMUNITY_ISSUES", "COMMUNITY_ISSUE_SEQ",
            "SERVICE_REQUEST_SEQ", "SEQ_SERVICE_REQUESTS", "SERVICE_REQUESTS_SEQ",
            "SERVICE_REQUEST_HISTORY_SEQ", "ISSUE_STATUS_HISTORY_SEQ", "SEQ_ISSUE_STATUS_HISTORY",
            "NOTIFICATION_SEQ", "SEQ_NOTIFICATIONS",
            "INVOICE_SEQ", "SEQ_INVOICES",
            "SEQ_USERS", "CATEGORY_SEQ", "SEQ_ISSUE_CATEGORIES"
        };
        for (String seq : sequences) {
            try {
                jdbc.execute("CREATE SEQUENCE " + seq + " START WITH 1 INCREMENT BY 1 NOCACHE");
            } catch (Exception ignored) {}
        }

        // 3. ISSUE_CATEGORIES table & default seed
        try {
            jdbc.execute("CREATE TABLE ISSUE_CATEGORIES ("
                    + "CATEGORY_ID NUMBER PRIMARY KEY, "
                    + "NAME VARCHAR2(100) NOT NULL, "
                    + "DESCRIPTION VARCHAR2(255))");
        } catch (Exception ignored) {}

        String[] issueCategories = {
            "1, 'Roads & Infrastructure', 'Potholes, paving, and traffic hazards'",
            "2, 'Electrical & Lighting', 'Street lights, wiring, and power issues'",
            "3, 'Plumbing & Water', 'Pipe repairs, leakages, and drainage'",
            "4, 'Waste & Sanitation', 'Garbage collection and sanitation'",
            "5, 'General Maintenance', 'General municipal maintenance'"
        };
        for (String cat : issueCategories) {
            try {
                jdbc.execute("INSERT INTO ISSUE_CATEGORIES (CATEGORY_ID, NAME, DESCRIPTION) "
                        + "SELECT " + cat + " FROM DUAL WHERE NOT EXISTS ("
                        + "SELECT 1 FROM ISSUE_CATEGORIES WHERE CATEGORY_ID = " + cat.split(",")[0] + ")");
            } catch (Exception ignored) {}
        }

        // 4. SERVICE_CATEGORIES table & default seed
        try {
            jdbc.execute("CREATE TABLE SERVICE_CATEGORIES ("
                    + "CATEGORY_ID NUMBER PRIMARY KEY, "
                    + "NAME VARCHAR2(100) NOT NULL, "
                    + "DESCRIPTION VARCHAR2(255))");
        } catch (Exception ignored) {}

        String[] serviceCategories = {
            "1, 'General Service', 'General maintenance and repairs'",
            "2, 'Plumbing & Water', 'Pipe repairs, leakages, and drainage'",
            "3, 'Electrical & Lighting', 'Street lights, wiring, and power issues'",
            "4, 'Roads & Infrastructure', 'Potholes, paving, and traffic hazards'",
            "5, 'Waste & Sanitation', 'Garbage collection and sanitation'"
        };
        for (String cat : serviceCategories) {
            try {
                jdbc.execute("INSERT INTO SERVICE_CATEGORIES (CATEGORY_ID, NAME, DESCRIPTION) "
                        + "SELECT " + cat + " FROM DUAL WHERE NOT EXISTS ("
                        + "SELECT 1 FROM SERVICE_CATEGORIES WHERE CATEGORY_ID = " + cat.split(",")[0] + ")");
            } catch (Exception ignored) {}
        }

        // 5. SERVICE_PROVIDERS table & sync
        try {
            jdbc.execute("CREATE TABLE SERVICE_PROVIDERS ("
                    + "PROVIDER_ID NUMBER PRIMARY KEY, "
                    + "USER_ID NUMBER, "
                    + "BUSINESS_NAME VARCHAR2(255), "
                    + "PHONE VARCHAR2(50), "
                    + "EMAIL VARCHAR2(255), "
                    + "VERIFICATION_STATUS VARCHAR2(50) DEFAULT 'VERIFIED', "
                    + "STATUS VARCHAR2(50) DEFAULT 'ACTIVE', "
                    + "CATEGORY_ID NUMBER DEFAULT 1, "
                    + "CITY VARCHAR2(100) DEFAULT 'Central Municipal Area', "
                    + "ADDRESS VARCHAR2(255) DEFAULT 'Municipal Service Hub', "
                    + "AVG_RATING NUMBER(3,2) DEFAULT 4.8, "
                    + "TOTAL_JOBS NUMBER DEFAULT 12)");
        } catch (Exception ignored) {}

        String[] providerCols = {
            "CATEGORY_ID NUMBER DEFAULT 1", "CITY VARCHAR2(100) DEFAULT 'Central Municipal Area'",
            "ADDRESS VARCHAR2(255) DEFAULT 'Municipal Service Hub'",
            "AVG_RATING NUMBER(3,2) DEFAULT 4.8", "TOTAL_JOBS NUMBER DEFAULT 12"
        };
        for (String col : providerCols) {
            try { jdbc.execute("ALTER TABLE SERVICE_PROVIDERS ADD (" + col + ")"); } catch (Exception ignored) {}
        }

        try {
            jdbc.execute("INSERT INTO SERVICE_PROVIDERS (PROVIDER_ID, USER_ID, BUSINESS_NAME, PHONE, EMAIL, VERIFICATION_STATUS, STATUS, CATEGORY_ID, CITY, ADDRESS, AVG_RATING, TOTAL_JOBS) "
                    + "SELECT u.USER_ID, u.USER_ID, u.FULL_NAME, u.PHONE_NUMBER, u.EMAIL, 'VERIFIED', 'ACTIVE', 1, 'Central Municipal Area', 'Municipal Hub', 4.8, 15 "
                    + "FROM USERS u WHERE u.ROLE_ID = 3 AND NOT EXISTS (SELECT 1 FROM SERVICE_PROVIDERS sp WHERE sp.USER_ID = u.USER_ID)");
        } catch (Exception ignored) {}

        // 6. COMMUNITY_ISSUES Table & Columns
        try {
            jdbc.execute("CREATE TABLE COMMUNITY_ISSUES ("
                    + "ISSUE_ID NUMBER PRIMARY KEY, "
                    + "TITLE VARCHAR2(255), "
                    + "DESCRIPTION VARCHAR2(4000), "
                    + "CATEGORY_ID NUMBER, "
                    + "CATEGORY_NAME VARCHAR2(100), "
                    + "STATUS VARCHAR2(50) DEFAULT 'REPORTED', "
                    + "PRIORITY VARCHAR2(50) DEFAULT 'MEDIUM', "
                    + "LOCATION_ADDRESS VARCHAR2(500), "
                    + "ADDRESS_DETAIL VARCHAR2(500), "
                    + "LATITUDE NUMBER(10,6), "
                    + "LONGITUDE NUMBER(10,6), "
                    + "REPORTED_BY NUMBER, "
                    + "ASSIGNED_TO NUMBER, "
                    + "PROVIDER_ID NUMBER, "
                    + "WARD VARCHAR2(100), "
                    + "EVIDENCE_PATH VARCHAR2(500), "
                    + "CREATED_AT TIMESTAMP DEFAULT CURRENT_TIMESTAMP, "
                    + "REPORTED_AT TIMESTAMP DEFAULT CURRENT_TIMESTAMP, "
                    + "UPDATED_AT TIMESTAMP, "
                    + "RESOLVED_AT TIMESTAMP)");
        } catch (Exception ignored) {}

        String[] issueCols = {
            "CATEGORY_NAME VARCHAR2(100)", "REPORTED_AT TIMESTAMP DEFAULT CURRENT_TIMESTAMP",
            "CREATED_AT TIMESTAMP DEFAULT CURRENT_TIMESTAMP", "UPDATED_AT TIMESTAMP",
            "RESOLVED_AT TIMESTAMP", "LATITUDE NUMBER(10,6)", "LONGITUDE NUMBER(10,6)",
            "EVIDENCE_PATH VARCHAR2(500)", "WARD VARCHAR2(100)", "PRIORITY VARCHAR2(50) DEFAULT 'MEDIUM'",
            "ADDRESS_DETAIL VARCHAR2(500)", "ASSIGNED_TO NUMBER", "PROVIDER_ID NUMBER"
        };
        for (String col : issueCols) {
            try { jdbc.execute("ALTER TABLE COMMUNITY_ISSUES ADD (" + col + ")"); } catch (Exception ignored) {}
        }

        // 7. ISSUE_STATUS_HISTORY Table
        try {
            jdbc.execute("CREATE TABLE ISSUE_STATUS_HISTORY ("
                    + "HISTORY_ID NUMBER PRIMARY KEY, "
                    + "ISSUE_ID NUMBER NOT NULL, "
                    + "FROM_STATUS VARCHAR2(50), "
                    + "TO_STATUS VARCHAR2(50), "
                    + "CHANGED_BY NUMBER, "
                    + "NOTES VARCHAR2(2000), "
                    + "CHANGED_AT TIMESTAMP DEFAULT CURRENT_TIMESTAMP)");
        } catch (Exception ignored) {}

        // 8. SERVICE_REQUESTS Table & Columns
        try {
            jdbc.execute("CREATE TABLE SERVICE_REQUESTS ("
                    + "REQUEST_ID NUMBER PRIMARY KEY, "
                    + "CITIZEN_ID NUMBER, "
                    + "PROVIDER_ID NUMBER, "
                    + "SERVICE_CATEGORY_ID NUMBER, "
                    + "CATEGORY_ID NUMBER, "
                    + "SERVICE_ID NUMBER, "
                    + "REQUEST_DETAILS VARCHAR2(2000), "
                    + "NOTES VARCHAR2(2000), "
                    + "ADDRESS VARCHAR2(500), "
                    + "STATUS VARCHAR2(50) DEFAULT 'REQUESTED', "
                    + "ESTIMATE_ID NUMBER, "
                    + "ESTIMATE_TOTAL NUMBER(10,2), "
                    + "ESTIMATE_SUBTOTAL NUMBER(10,2), "
                    + "ESTIMATE_TAX NUMBER(10,2), "
                    + "ESTIMATE_STATUS VARCHAR2(50), "
                    + "ESTIMATE_DESCRIPTION VARCHAR2(2000), "
                    + "COMPLETION_NOTES VARCHAR2(2000), "
                    + "REQUESTED_AT TIMESTAMP DEFAULT CURRENT_TIMESTAMP, "
                    + "REQUEST_DATE TIMESTAMP DEFAULT CURRENT_TIMESTAMP, "
                    + "CREATED_AT TIMESTAMP DEFAULT CURRENT_TIMESTAMP, "
                    + "UPDATED_AT TIMESTAMP)");
        } catch (Exception ignored) {}

        String[] reqCols = {
            "SERVICE_CATEGORY_ID NUMBER", "CATEGORY_ID NUMBER", "SERVICE_ID NUMBER", "ADDRESS VARCHAR2(500)",
            "COMPLETION_NOTES VARCHAR2(2000)", "NOTES VARCHAR2(2000)", "UPDATED_AT TIMESTAMP",
            "REQUESTED_AT TIMESTAMP DEFAULT CURRENT_TIMESTAMP", "REQUEST_DATE TIMESTAMP DEFAULT CURRENT_TIMESTAMP", "CREATED_AT TIMESTAMP DEFAULT CURRENT_TIMESTAMP",
            "ESTIMATE_ID NUMBER", "ESTIMATE_TOTAL NUMBER(10,2)",
            "ESTIMATE_SUBTOTAL NUMBER(10,2)", "ESTIMATE_TAX NUMBER(10,2)",
            "ESTIMATE_STATUS VARCHAR2(50)", "ESTIMATE_DESCRIPTION VARCHAR2(2000)"
        };
        for (String col : reqCols) {
            try { jdbc.execute("ALTER TABLE SERVICE_REQUESTS ADD (" + col + ")"); } catch (Exception ignored) {}
        }

        // 9. SERVICE_REQUEST_STATUS_HISTORY Table
        try {
            jdbc.execute("CREATE TABLE SERVICE_REQUEST_STATUS_HISTORY ("
                    + "HISTORY_ID NUMBER PRIMARY KEY, "
                    + "REQUEST_ID NUMBER NOT NULL, "
                    + "FROM_STATUS VARCHAR2(50), "
                    + "TO_STATUS VARCHAR2(50), "
                    + "CHANGED_BY NUMBER, "
                    + "REMARKS VARCHAR2(2000), "
                    + "CHANGED_AT TIMESTAMP DEFAULT CURRENT_TIMESTAMP)");
        } catch (Exception ignored) {}

        // 10. SERVICE_ESTIMATES Table
        try {
            jdbc.execute("CREATE TABLE SERVICE_ESTIMATES ("
                    + "ESTIMATE_ID NUMBER PRIMARY KEY, "
                    + "REQUEST_ID NUMBER, "
                    + "PROVIDER_ID NUMBER, "
                    + "SUBTOTAL NUMBER(10,2) DEFAULT 0, "
                    + "TAX NUMBER(10,2) DEFAULT 0, "
                    + "TOTAL_AMOUNT NUMBER(10,2) DEFAULT 0, "
                    + "STATUS VARCHAR2(50) DEFAULT 'PENDING', "
                    + "DESCRIPTION VARCHAR2(2000), "
                    + "CREATED_AT TIMESTAMP DEFAULT CURRENT_TIMESTAMP)");
        } catch (Exception ignored) {}

        // 11. NOTIFICATIONS Table
        try {
            jdbc.execute("CREATE TABLE NOTIFICATIONS ("
                    + "NOTIFICATION_ID NUMBER PRIMARY KEY, "
                    + "USER_ID NUMBER NOT NULL, "
                    + "TITLE VARCHAR2(255), "
                    + "MESSAGE VARCHAR2(2000), "
                    + "IS_READ NUMBER(1) DEFAULT 0, "
                    + "CREATED_AT TIMESTAMP DEFAULT CURRENT_TIMESTAMP)");
        } catch (Exception ignored) {}

        // 12. INVOICES & Billing Tables
        try {
            jdbc.execute("CREATE TABLE INVOICES ("
                    + "INVOICE_ID NUMBER PRIMARY KEY, "
                    + "INVOICE_NUMBER VARCHAR2(50), "
                    + "CITIZEN_ID NUMBER, "
                    + "PROVIDER_ID NUMBER, "
                    + "SERVICE_REQUEST_ID NUMBER, "
                    + "INVOICE_TYPE VARCHAR2(50) DEFAULT 'SERVICE', "
                    + "STATUS VARCHAR2(50) DEFAULT 'PENDING', "
                    + "SUBTOTAL NUMBER(10,2) DEFAULT 0, "
                    + "TAX NUMBER(10,2) DEFAULT 0, "
                    + "TOTAL_AMOUNT NUMBER(10,2) DEFAULT 0, "
                    + "AMOUNT_PAID NUMBER(10,2) DEFAULT 0, "
                    + "BALANCE_DUE NUMBER(10,2) DEFAULT 0, "
                    + "CREATED_AT TIMESTAMP DEFAULT CURRENT_TIMESTAMP, "
                    + "DUE_DATE TIMESTAMP)");
        } catch (Exception ignored) {}

        try {
            jdbc.execute("CREATE TABLE SERVICE_INVOICE_LINE_ITEMS ("
                    + "LINE_ITEM_ID NUMBER GENERATED BY DEFAULT AS IDENTITY PRIMARY KEY, "
                    + "INVOICE_ID NUMBER, "
                    + "ITEM_DESCRIPTION VARCHAR2(255), "
                    + "QUANTITY NUMBER DEFAULT 1, "
                    + "UNIT_PRICE NUMBER(10,2) DEFAULT 0, "
                    + "TOTAL_PRICE NUMBER(10,2) DEFAULT 0)");
        } catch (Exception ignored) {}

        try {
            jdbc.execute("CREATE TABLE INVOICE_LINE_ITEMS ("
                    + "LINE_ITEM_ID NUMBER GENERATED BY DEFAULT AS IDENTITY PRIMARY KEY, "
                    + "INVOICE_ID NUMBER, "
                    + "ITEM_DESCRIPTION VARCHAR2(255), "
                    + "QUANTITY NUMBER DEFAULT 1, "
                    + "UNIT_PRICE NUMBER(10,2) DEFAULT 0, "
                    + "TOTAL_PRICE NUMBER(10,2) DEFAULT 0)");
        } catch (Exception ignored) {}

        // Do not insert demo issues, invoices, service requests, or notifications into
        // the configured database. Existing and newly registered accounts start with
        // only the records users actually create.
    }

    private void seedDemoDataForCitizens() {
        try {
            List<Long> allUserIds = jdbc.queryForList("SELECT USER_ID FROM USERS", Long.class);
            if (allUserIds.isEmpty()) return;

            for (Long userId : allUserIds) {
                // Seed Community Issues for EVERY user
                Integer issueCount = jdbc.queryForObject("SELECT COUNT(*) FROM COMMUNITY_ISSUES WHERE REPORTED_BY = ?", Integer.class, userId);
                if (issueCount == null || issueCount == 0) {
                    jdbc.execute("INSERT INTO COMMUNITY_ISSUES (ISSUE_ID, TITLE, DESCRIPTION, CATEGORY_ID, CATEGORY_NAME, STATUS, PRIORITY, LOCATION_ADDRESS, ADDRESS_DETAIL, LATITUDE, LONGITUDE, REPORTED_BY, WARD, REPORTED_AT, CREATED_AT) "
                            + "VALUES (ISSUE_SEQ.NEXTVAL, 'Severe Road Pothole near Central Market', 'Large hazardous pothole causing traffic slowdowns and vehicle damage.', 1, 'Roads & Infrastructure', 'REPORTED', 'HIGH', 'Main Market Road, Ward 12', 'Main Market Road, Ward 12', 18.520430, 73.856743, " + userId + ", 'Ward 12', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)");

                    jdbc.execute("INSERT INTO COMMUNITY_ISSUES (ISSUE_ID, TITLE, DESCRIPTION, CATEGORY_ID, CATEGORY_NAME, STATUS, PRIORITY, LOCATION_ADDRESS, ADDRESS_DETAIL, LATITUDE, LONGITUDE, REPORTED_BY, WARD, REPORTED_AT, CREATED_AT) "
                            + "VALUES (ISSUE_SEQ.NEXTVAL, 'Broken Street Light on 5th Avenue', 'Street light outage at night near park entrance.', 2, 'Electrical & Lighting', 'IN_PROGRESS', 'MEDIUM', '5th Avenue Street, Ward 8', '5th Avenue Street, Ward 8', 18.531200, 73.844500, " + userId + ", 'Ward 8', CURRENT_TIMESTAMP - INTERVAL '1' DAY, CURRENT_TIMESTAMP - INTERVAL '1' DAY)");
                }

                // Seed Invoices & Bills for EVERY user
                Integer invCount = jdbc.queryForObject("SELECT COUNT(*) FROM INVOICES WHERE CITIZEN_ID = ?", Integer.class, userId);
                if (invCount == null || invCount == 0) {
                    Long invoiceId = jdbc.queryForObject("SELECT INVOICE_SEQ.NEXTVAL FROM DUAL", Long.class);
                    if (invoiceId == null) invoiceId = 1L;

                    jdbc.execute("INSERT INTO INVOICES (INVOICE_ID, INVOICE_NUMBER, CITIZEN_ID, PROVIDER_ID, INVOICE_TYPE, STATUS, SUBTOTAL, TAX, TOTAL_AMOUNT, AMOUNT_PAID, BALANCE_DUE, CREATED_AT, DUE_DATE) "
                            + "VALUES (" + invoiceId + ", 'INV-2026-00" + invoiceId + "', " + userId + ", 1, 'SERVICE', 'PENDING', 1350.00, 150.00, 1500.00, 0.00, 1500.00, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP + INTERVAL '14' DAY)");

                    try {
                        jdbc.execute("INSERT INTO SERVICE_INVOICE_LINE_ITEMS (INVOICE_ID, ITEM_DESCRIPTION, QUANTITY, UNIT_PRICE, TOTAL_PRICE) VALUES (" + invoiceId + ", 'Municipal Service & Maintenance Fee', 1, 1350.00, 1350.00)");
                    } catch (Exception ignored) {}
                }

                // Seed Service Requests for EVERY user
                Integer reqCount = jdbc.queryForObject("SELECT COUNT(*) FROM SERVICE_REQUESTS WHERE CITIZEN_ID = ?", Integer.class, userId);
                if (reqCount == null || reqCount == 0) {
                    jdbc.execute("INSERT INTO SERVICE_REQUESTS (REQUEST_ID, CITIZEN_ID, PROVIDER_ID, SERVICE_CATEGORY_ID, CATEGORY_ID, REQUEST_DETAILS, ADDRESS, STATUS, ESTIMATE_TOTAL, ESTIMATE_SUBTOTAL, ESTIMATE_TAX, ESTIMATE_STATUS, ESTIMATE_DESCRIPTION, REQUESTED_AT, CREATED_AT) "
                            + "VALUES (SERVICE_REQUEST_SEQ.NEXTVAL, " + userId + ", 1, 2, 2, 'Main kitchen pipe leakage and sink clog repair', 'Flat 302, Green Avenue', 'AWAITING_ESTIMATE_APPROVAL', 1500.00, 1350.00, 150.00, 'PENDING', 'Pipe replacement parts + 2 hours labor charges', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)");
                }

                // Seed Notifications for EVERY user
                Integer notifCount = jdbc.queryForObject("SELECT COUNT(*) FROM NOTIFICATIONS WHERE USER_ID = ?", Integer.class, userId);
                if (notifCount == null || notifCount == 0) {
                    jdbc.execute("INSERT INTO NOTIFICATIONS (NOTIFICATION_ID, USER_ID, TITLE, MESSAGE, IS_READ, CREATED_AT) "
                            + "VALUES (NOTIFICATION_SEQ.NEXTVAL, " + userId + ", 'Welcome to CIVENTRA', 'Your account is ready. Report issues or book verified local services anytime.', 0, CURRENT_TIMESTAMP)");
                }
            }
        } catch (Exception ignored) {}
    }
}
