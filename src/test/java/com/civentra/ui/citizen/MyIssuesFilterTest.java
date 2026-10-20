package com.civentra.ui.citizen;

import com.civentra.model.CommunityIssue;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class MyIssuesFilterTest {
    @Test
    void appliesKeywordStatusCategoryAndInclusiveDateFiltersTogether() {
        CommunityIssue matching = issue("Broken streetlight", "Streetlights", "IN_PROGRESS",
                LocalDateTime.of(2026, 7, 10, 23, 59));
        CommunityIssue wrongCategory = issue("Broken streetlight", "Roads", "IN_PROGRESS",
                LocalDateTime.of(2026, 7, 10, 12, 0));
        CommunityIssue wrongStatus = issue("Broken streetlight", "Streetlights", "RESOLVED",
                LocalDateTime.of(2026, 7, 10, 12, 0));
        CommunityIssue outsideRange = issue("Broken streetlight", "Streetlights", "IN_PROGRESS",
                LocalDateTime.of(2026, 7, 11, 0, 0));

        List<CommunityIssue> results = MyIssuesView.filterIssues(
                List.of(matching, wrongCategory, wrongStatus, outsideRange), "streetlight", "IN_PROGRESS",
                "Streetlights", LocalDate.of(2026, 7, 10), LocalDate.of(2026, 7, 10));

        assertEquals(List.of(matching), results);
    }

    @Test
    void unfilteredIssuesWithMissingReportedDateRemainVisible() {
        CommunityIssue missingDate = issue("Water leak", null, "REPORTED", null);
        assertEquals(List.of(missingDate), MyIssuesView.filterIssues(
                List.of(missingDate), null, null, null, null, null));
    }

    private CommunityIssue issue(String title, String category, String status, LocalDateTime reportedAt) {
        CommunityIssue issue = new CommunityIssue();
        issue.setTitle(title);
        issue.setCategoryName(category);
        issue.setStatus(status);
        issue.setReportedAt(reportedAt);
        return issue;
    }
}
