package com.githubchart;

public class GithubChart {
    private final GithubScraper scraper;
    private final String username;
    private ColorScheme colorScheme = ColorScheme.DEFAULT;

    // Package-private constructor for testing with mock scraper
    GithubChart(String username, GithubScraper scraper) {
        this.username = username;
        this.scraper = scraper;
    }

    // Public API
    public static GithubChart forUser(String username) {
        return new GithubChart(username, new GithubScraper());
    }

    // Test hook - allows injecting pre-built data without a scraper
    static GithubChart forUser(String username, GithubChart testHarness) {
        return new GithubChart(username, (GithubScraper) null) {
            @Override
            ContributionData fetchData(String u) {
                return testHarness.fetchData(u);
            }
        };
    }

    public GithubChart withColorScheme(ColorScheme scheme) {
        this.colorScheme = scheme;
        return this;
    }

    public String render() {
        ContributionData data = fetchData(username);
        return new SvgRenderer().render(data, colorScheme);
    }

    ContributionData fetchData(String username) {
        return scraper.fetch(username);
    }
}
