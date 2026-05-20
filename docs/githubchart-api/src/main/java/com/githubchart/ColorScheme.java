package com.githubchart;

public class ColorScheme {
    public static final ColorScheme DEFAULT = new ColorScheme(
            "#eeeeee", "#d6e685", "#8cc665", "#44a340", "#1e6823");
    public static final ColorScheme HALLOWEEN = new ColorScheme(
            "#EEEEEE", "#FFEE4A", "#FFC501", "#FE9600", "#03001C");
    public static final ColorScheme TEAL = new ColorScheme(
            "#EEEEEE", "#7FFFD4", "#76EEC6", "#66CDAA", "#458B74");

    private final String[] colors;

    public ColorScheme(String... colors) {
        if (colors.length != 5) {
            throw new IllegalArgumentException("ColorScheme requires exactly 5 colors");
        }
        this.colors = colors.clone();
    }

    public String[] getColors() {
        return colors.clone();
    }

    public static ColorScheme fromHex(String hex) {
        String h = hex.replace("#", "");
        return new ColorScheme(
                "#EEEEEE",
                lighten(h, 0.3),
                lighten(h, 0.2),
                "#" + h,
                darken(h, 0.8));
    }

    public static String darken(String hex, double amount) {
        return manipulate(hex, amount, false);
    }

    public static String lighten(String hex, double amount) {
        return manipulate(hex, amount, true);
    }

    private static String manipulate(String hex, double amount, boolean lighten) {
        String h = hex.replace("#", "");
        int r = Integer.parseInt(h.substring(0, 2), 16);
        int g = Integer.parseInt(h.substring(2, 4), 16);
        int b = Integer.parseInt(h.substring(4, 6), 16);

        if (lighten) {
            r = (int) Math.round((r + 255) * amount);
            g = (int) Math.round((g + 255) * amount);
            b = (int) Math.round((b + 255) * amount);
        } else {
            r = (int) Math.round(r * amount);
            g = (int) Math.round(g * amount);
            b = (int) Math.round(b * amount);
        }

        r = Math.min(255, Math.max(0, r));
        g = Math.min(255, Math.max(0, g));
        b = Math.min(255, Math.max(0, b));

        return String.format("#%02x%02x%02x", r, g, b);
    }
}
