package com.githubchart;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import java.io.IOException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

public class GithubScraper {

    private static final String GITHUB_URL = "https://github.com";

    public ContributionData fetch(String username) {
        try {
            String html = fetchHtml(username);
            return parseHtml(username, html);
        } catch (IOException e) {
            throw new GithubChartException("Failed to fetch GitHub profile for: " + username, e);
        }
    }

    protected String fetchHtml(String username) throws IOException {
        Document doc = Jsoup.connect(GITHUB_URL + "/" + username)
                .userAgent("Mozilla/5.0 (compatible; githubchart-java/1.0)")
                .timeout(15000)
                .get();
        if (doc.select("title").text().contains("Page not found")) {
            throw new IOException("User not found: " + username);
        }
        return doc.html();
    }

    ContributionData parseHtml(String username, String html) {
        Document doc = Jsoup.parse(html);
        Elements rects = doc.select(".js-calendar-graph-svg rect.ContributionCalendar-day");

        if (rects.isEmpty()) {
            throw new GithubChartException(
                    "No contribution data found for user: " + username
                    + ". GitHub may have changed their page structure.");
        }

        List<ContributionDay> days = new ArrayList<>();
        DateTimeFormatter fmt = DateTimeFormatter.ofPattern("yyyy-MM-dd");

        for (Element rect : rects) {
            String dateStr = rect.attr("data-date");
            String countStr = rect.attr("data-count");
            String levelStr = rect.attr("data-level");

            if (dateStr.isEmpty() || countStr.isEmpty() || levelStr.isEmpty()) {
                continue;
            }

            LocalDate date = LocalDate.parse(dateStr, fmt);
            int count = Integer.parseInt(countStr);
            int level = Integer.parseInt(levelStr);
            days.add(new ContributionDay(date, count, level));
        }

        if (days.isEmpty()) {
            throw new GithubChartException("No contribution data parsed for user: " + username);
        }

        int year = days.get(days.size() - 1).getDate().getYear();
        return new ContributionData(username, year, days);
    }
}
