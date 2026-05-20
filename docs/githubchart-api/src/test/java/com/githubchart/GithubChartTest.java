package com.githubchart;

import org.junit.jupiter.api.Test;
import java.time.LocalDate;
import java.util.Arrays;
import static org.junit.jupiter.api.Assertions.*;

class GithubChartTest {

    @Test
    void shouldRenderWithDefaultColorScheme() {
        TestChart chart = new TestChart();
        String svg = GithubChart.forUser("testuser", chart).render();
        assertNotNull(svg);
        assertTrue(svg.contains("<svg"));
        assertTrue(svg.contains("#1e6823")); // default dark green
    }

    @Test
    void shouldRenderWithCustomColorScheme() {
        TestChart chart = new TestChart();
        String svg = GithubChart.forUser("testuser", chart)
                .withColorScheme(ColorScheme.HALLOWEEN)
                .render();
        assertNotNull(svg);
        assertTrue(svg.contains("#03001C")); // halloween dark
    }

    @Test
    void shouldRenderWithHexColorScheme() {
        TestChart chart = new TestChart();
        String svg = GithubChart.forUser("testuser", chart)
                .withColorScheme(ColorScheme.fromHex("409ba5"))
                .render();
        assertNotNull(svg);
        assertTrue(svg.contains("svg"));
    }

    @Test
    void shouldPassUsernameToScraper() {
        TestChart chart = new TestChart();
        GithubChart.forUser("octocat", chart).render();
        assertEquals("octocat", chart.lastUsername);
    }

    static class TestChart extends GithubChart {
        String lastUsername;
        TestChart() { super(null, null); }
        @Override
        ContributionData fetchData(String username) {
            this.lastUsername = username;
            return new ContributionData(username, 2026,
                    Arrays.asList(
                            new ContributionDay(LocalDate.of(2026, 5, 9), 3, 1),
                            new ContributionDay(LocalDate.of(2026, 5, 10), 7, 3),
                            new ContributionDay(LocalDate.of(2026, 5, 11), 15, 4)));
        }
    }
}
