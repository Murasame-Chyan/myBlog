package com.githubchart;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;

public class SvgRenderer {

    private static final int CELL_SIZE = 11;
    private static final int CELL_STEP = 13; // CELL_SIZE + gap(2)

    public String render(ContributionData data, ColorScheme scheme) {
        StringBuilder svg = new StringBuilder();
        int totalWeeks = 53;
        int totalDays = 7;
        int totalWidth = totalWeeks * CELL_STEP;
        int totalHeight = totalDays * CELL_STEP;
        int leftPad = 30;

        svg.append("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n");
        svg.append(String.format(
                "<svg xmlns=\"http://www.w3.org/2000/svg\" viewBox=\"0 0 %d %d\" width=\"%d\" height=\"%d\">\n",
                leftPad + totalWidth + 10, totalHeight + 30,
                leftPad + totalWidth + 10, totalHeight + 30));
        svg.append(String.format(
                "  <title>%s's %d GitHub contributions</title>\n",
                data.getUsername(), data.getYear()));
        svg.append("<style>\n");
        svg.append("  text { font-family: -apple-system,BlinkMacSystemFont,'Segoe UI',Helvetica,Arial,sans-serif; font-size: 9px; fill: #586069; }\n");
        svg.append("</style>\n");

        // Month labels
        String[] months = {"Jan", "Feb", "Mar", "Apr", "May", "Jun",
                           "Jul", "Aug", "Sep", "Oct", "Nov", "Dec"};
        for (int m = 0; m < 12; m++) {
            int weekIdx = weekOfYear(LocalDate.of(data.getYear(), m + 1, 1));
            if (weekIdx >= 0 && weekIdx < totalWeeks) {
                svg.append(String.format(
                        "<text x=\"%d\" y=\"10\">%s</text>\n",
                        leftPad + weekIdx * CELL_STEP, months[m]));
            }
        }

        // Day labels
        String[] days = {"", "Mon", "", "Wed", "", "Fri", ""};
        for (int d = 0; d < 7; d++) {
            if (!days[d].isEmpty()) {
                svg.append(String.format(
                        "<text x=\"0\" y=\"%d\">%s</text>\n",
                        25 + d * CELL_STEP + 5, days[d]));
            }
        }

        // Contribution rects
        List<ContributionDay> dayList = data.getDays();
        for (ContributionDay day : dayList) {
            LocalDate date = day.getDate();
            int col = weekOfYear(date);
            int row = dayOfWeekIndex(date.getDayOfWeek());

            if (col < 0 || col >= totalWeeks) continue;

            String fill = scheme.getColors()[day.getLevel()];
            int x = leftPad + col * CELL_STEP;
            int y = 20 + row * CELL_STEP;

            svg.append(String.format(
                    "<rect x=\"%d\" y=\"%d\" width=\"%d\" height=\"%d\" fill=\"%s\" rx=\"2\" ry=\"2\">\n",
                    x, y, CELL_SIZE, CELL_SIZE, fill));
            svg.append(String.format(
                    "  <title>%d contributions on %s</title>\n",
                    day.getCount(), date.format(DateTimeFormatter.ISO_LOCAL_DATE)));
            svg.append("</rect>\n");
        }

        svg.append("</svg>");
        return svg.toString();
    }

    static int dayOfWeekIndex(DayOfWeek dow) {
        // GitHub weeks start on Sunday; map Sun=0, Mon=1, ..., Sat=6
        return dow.getValue() % 7; // Sunday (value 7) → 0
    }

    static int weekOfYear(LocalDate date) {
        // Week number starting from Jan 1 = week 0
        LocalDate yearStart = LocalDate.of(date.getYear(), 1, 1);
        int dayOfYear = date.getDayOfYear() - 1;
        int startOffset = dayOfWeekIndex(yearStart.getDayOfWeek());
        return (dayOfYear + startOffset) / 7;
    }
}
