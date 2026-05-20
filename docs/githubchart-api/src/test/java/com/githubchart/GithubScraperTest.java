package com.githubchart;

import org.junit.jupiter.api.Test;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.time.LocalDate;
import static org.junit.jupiter.api.Assertions.*;

class GithubScraperTest {

    @Test
    void shouldParseContributionsFromHtml() throws IOException {
        String html = new String(Files.readAllBytes(
                Paths.get("src/test/resources/github-profile-sample.html")));

        TestableScraper scraper = new TestableScraper(html);
        ContributionData data = scraper.fetch("testuser");

        assertEquals("testuser", data.getUsername());
        assertEquals(3, data.getDays().size());

        ContributionDay first = data.getDays().get(0);
        assertEquals(LocalDate.of(2026, 5, 9), first.getDate());
        assertEquals(3, first.getCount());
        assertEquals(1, first.getLevel());

        ContributionDay last = data.getDays().get(2);
        assertEquals(LocalDate.of(2026, 5, 11), last.getDate());
        assertEquals(0, last.getCount());
        assertEquals(0, last.getLevel());

        assertEquals(10, data.getTotalContributions());
        assertEquals(7, data.getMaxCount());
    }

    @Test
    void shouldThrowExceptionForEmptyHtml() {
        TestableScraper scraper = new TestableScraper("<html></html>");
        assertThrows(GithubChartException.class, () -> scraper.fetch("nobody"));
    }

    /** Testable scraper that returns fixed HTML instead of hitting the network. */
    static class TestableScraper extends GithubScraper {
        private final String html;
        TestableScraper(String html) { this.html = html; }
        @Override
        protected String fetchHtml(String username) { return html; }
    }
}
