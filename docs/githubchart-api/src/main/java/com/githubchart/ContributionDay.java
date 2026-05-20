package com.githubchart;

import java.time.LocalDate;

public class ContributionDay {
    private final LocalDate date;
    private final int count;
    private final int level;

    public ContributionDay(LocalDate date, int count, int level) {
        this.date = date;
        this.count = count;
        this.level = level;
    }

    public LocalDate getDate() { return date; }
    public int getCount() { return count; }
    public int getLevel() { return level; }
}
