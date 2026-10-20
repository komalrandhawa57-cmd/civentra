package com.civentra.model;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class IssueStatusTest {
    @Test
    void pendingIsNotMisreportedAsReported() {
        CommunityIssue issue = new CommunityIssue();
        issue.setStatus("PENDING");
        assertEquals(IssueStatus.PENDING, issue.getStatus());
    }

    @Test
    void unknownDatabaseStatusIsNotSilentlyConvertedToReported() {
        CommunityIssue issue = new CommunityIssue();
        assertThrows(IllegalArgumentException.class, () -> issue.setStatus("UNRECOGNIZED_STATUS"));
    }
}
