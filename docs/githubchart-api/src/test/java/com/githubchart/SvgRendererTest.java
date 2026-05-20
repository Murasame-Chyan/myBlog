package com.githubchart;

import org.junit.jupiter.api.Test;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;

class SvgRendererTest {

    @Test
    void shouldRenderSvgWithCorrectStructure() {
        List<ContributionDay> days = new ArrayList<>();
        for (int i = 0; i < 365; i++) {
            days.add(new ContributionDay(
                    LocalDate.of(2026, 1, 1).plusDays(i), i % 10, (i % 10) / 2));
        }
        ContributionData data = new ContributionData("testuser", 2026, days);
        String svg = new SvgRenderer().render(data, ColorScheme.DEFAULT);

        assertNotNull(svg);
        assertTrue(svg.contains("<svg"));
        assertTrue(svg.contains("</svg>"));
        assertTrue(svg.contains("<rect"));
        assertTrue(svg.contains("testuser"));
        assertTrue(svg.contains("2026"));
        // Should have exactly 365 rects
        assertEquals(365, countSubstrings(svg, "<rect"));
    }

    @Test
    void shouldRenderWithCustomColorScheme() {
        List<ContributionDay> days = new ArrayList<>();
        days.add(new ContributionDay(LocalDate.of(2026, 5, 9), 0, 0));
        days.add(new ContributionDay(LocalDate.of(2026, 5, 10), 10, 4));
        ContributionData data = new ContributionData("testuser", 2026, days);
        String svg = new SvgRenderer().render(data, ColorScheme.HALLOWEEN);

        assertTrue(svg.contains("#EEEEEE"));
        assertTrue(svg.contains("#03001C"));
    }

    @Test
    void shouldIncludeMonthLabels() {
        List<ContributionDay> days = new ArrayList<>();
        days.add(new ContributionDay(LocalDate.of(2026, 1, 1), 1, 1));
        ContributionData data = new ContributionData("testuser", 2026, days);
        String svg = new SvgRenderer().render(data, ColorScheme.DEFAULT);

        assertTrue(svg.contains("Jan"));
        assertTrue(svg.contains("Dec"));
    }

    @Test
    void shouldIncludeDayLabels() {
        List<ContributionDay> days = new ArrayList<>();
        days.add(new ContributionDay(LocalDate.of(2026, 1, 4), 1, 1));
        ContributionData data = new ContributionData("testuser", 2026, days);
        String svg = new SvgRenderer().render(data, ColorScheme.DEFAULT);

        assertTrue(svg.contains("Mon"));
        assertTrue(svg.contains("Wed"));
        assertTrue(svg.contains("Fri"));
    }

    private static int countSubstrings(String str, String sub) {
        int count = 0;
        int idx = 0;
        while ((idx = str.indexOf(sub, idx)) != -1) {
            count++;
            idx += sub.length();
        }
        return count;
    }
}
