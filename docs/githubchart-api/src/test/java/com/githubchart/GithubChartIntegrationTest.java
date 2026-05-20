package com.githubchart;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Tag;
import static org.junit.jupiter.api.Assertions.*;

@Tag("integration")
class GithubChartIntegrationTest {

    @Test
    void shouldFetchAndRenderRealUser() {
        String svg = GithubChart.forUser("torvalds").render();
        assertNotNull(svg);
        assertTrue(svg.startsWith("<?xml"));
        assertTrue(svg.contains("<svg"));
        assertTrue(svg.contains("</svg>"));
        assertTrue(svg.contains("<rect"));
        System.out.println("SVG length: " + svg.length());
    }

    @Test
    void shouldThrowForNonexistentUser() {
        assertThrows(GithubChartException.class,
                () -> GithubChart.forUser("this-user-should-not-exist-xyz-123456").render());
    }
}
