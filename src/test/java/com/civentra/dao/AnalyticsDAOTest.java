package com.civentra.dao;

import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;

import java.sql.Timestamp;
import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class AnalyticsDAOTest {
    private final JdbcTemplate jdbc = mock(JdbcTemplate.class);
    private final AnalyticsDAO analytics = new AnalyticsDAO(jdbc);

    @Test
    void issueCountsUseStartInclusiveAndDayAfterEndExclusiveBounds() {
        LocalDate from = LocalDate.of(2026, 5, 1);
        LocalDate to = LocalDate.of(2026, 5, 31);
        when(jdbc.queryForObject(anyString(), eq(Integer.class), any(), any())).thenReturn(12);

        assertEquals(12, analytics.getIssueCount(from, to));

        verify(jdbc).queryForObject(contains("REPORTED_AT >= ? AND REPORTED_AT < ?"), eq(Integer.class),
                eq(Timestamp.valueOf(from.atStartOfDay())), eq(Timestamp.valueOf(LocalDate.of(2026, 6, 1).atStartOfDay())));
    }

    @Test
    void dateRangeRequiresBothDatesAndStartCannotFollowEnd() {
        assertThrows(IllegalArgumentException.class, () -> AnalyticsDAO.validateDateRange(null, LocalDate.now()));
        assertThrows(IllegalArgumentException.class, () -> AnalyticsDAO.validateDateRange(
                LocalDate.of(2026, 6, 1), LocalDate.of(2026, 5, 31)));
    }
}
