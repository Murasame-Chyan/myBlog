package com.githubchart;

import org.junit.jupiter.api.Test;
import java.time.LocalDate;
import static org.junit.jupiter.api.Assertions.*;

class ContributionDataTest {

    @Test
    void shouldCreateContributionDay() {
        ContributionDay day = new ContributionDay(LocalDate.of(2026, 5, 10), 5, 2);
        assertEquals(LocalDate.of(2026, 5, 10), day.getDate());
        assertEquals(5, day.getCount());
        assertEquals(2, day.getLevel());
    }

    @Test
    void shouldComputeTotalsFromDays() {
        ContributionDay d1 = new ContributionDay(LocalDate.of(2026, 5, 9), 3, 1);
        ContributionDay d2 = new ContributionDay(LocalDate.of(2026, 5, 10), 7, 3);
        ContributionData data = new ContributionData("testuser", 2026,
                java.util.Arrays.asList(d1, d2));
        assertEquals("testuser", data.getUsername());
        assertEquals(2026, data.getYear());
        assertEquals(2, data.getDays().size());
        assertEquals(10, data.getTotalContributions());
        assertEquals(7, data.getMaxCount());
    }

    @Test
    void shouldCreateExceptionWithMessage() {
        GithubChartException ex = new GithubChartException("User not found: ghost");
        assertEquals("User not found: ghost", ex.getMessage());
        assertTrue(ex instanceof RuntimeException);
    }
}
