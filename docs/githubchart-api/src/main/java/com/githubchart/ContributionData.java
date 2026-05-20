package com.githubchart;

import java.util.Collections;
import java.util.List;

public class ContributionData {
    private final String username;
    private final int year;
    private final List<ContributionDay> days;
    private final int totalContributions;
    private final int maxCount;

    public ContributionData(String username, int year, List<ContributionDay> days) {
        this.username = username;
        this.year = year;
        this.days = Collections.unmodifiableList(days);
        int total = 0;
        int max = 0;
        for (ContributionDay d : days) {
            total += d.getCount();
            if (d.getCount() > max) max = d.getCount();
        }
        this.totalContributions = total;
        this.maxCount = max;
    }

    public String getUsername() { return username; }
    public int getYear() { return year; }
    public List<ContributionDay> getDays() { return days; }
    public int getTotalContributions() { return totalContributions; }
    public int getMaxCount() { return maxCount; }
}
