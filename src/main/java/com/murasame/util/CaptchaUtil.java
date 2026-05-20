package com.murasame.util;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.util.concurrent.ThreadLocalRandom;

public class CaptchaUtil {

    private static final String CHARS = "ABCDEFGHJKLMNPQRSTUVWXYZabcdefghjkmnpqrstuvwxyz23456789";
    private static final int WIDTH = 120;
    private static final int HEIGHT = 40;
    private static final int CODE_LEN = 4;

    public static String generateCode() {
        StringBuilder sb = new StringBuilder(CODE_LEN);
        for (int i = 0; i < CODE_LEN; i++) {
            sb.append(CHARS.charAt(ThreadLocalRandom.current().nextInt(CHARS.length())));
        }
        return sb.toString();
    }

    public static BufferedImage generateImage(String code) {
        BufferedImage image = new BufferedImage(WIDTH, HEIGHT, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = image.createGraphics();

        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        // Background
        int bgR = 240 + ThreadLocalRandom.current().nextInt(16);
        int bgG = 240 + ThreadLocalRandom.current().nextInt(16);
        int bgB = 240 + ThreadLocalRandom.current().nextInt(16);
        g.setColor(new Color(bgR, bgG, bgB));
        g.fillRect(0, 0, WIDTH, HEIGHT);

        // Interference lines
        for (int i = 0; i < 5; i++) {
            int r = ThreadLocalRandom.current().nextInt(180);
            int gr = ThreadLocalRandom.current().nextInt(180);
            int b = ThreadLocalRandom.current().nextInt(180);
            g.setColor(new Color(r, gr, b, 80 + ThreadLocalRandom.current().nextInt(100)));
            int x1 = ThreadLocalRandom.current().nextInt(WIDTH);
            int y1 = ThreadLocalRandom.current().nextInt(HEIGHT);
            int x2 = ThreadLocalRandom.current().nextInt(WIDTH);
            int y2 = ThreadLocalRandom.current().nextInt(HEIGHT);
            g.drawLine(x1, y1, x2, y2);
        }

        // Noise dots
        for (int i = 0; i < 40; i++) {
            int x = ThreadLocalRandom.current().nextInt(WIDTH);
            int y = ThreadLocalRandom.current().nextInt(HEIGHT);
            int r = ThreadLocalRandom.current().nextInt(200);
            int gr = ThreadLocalRandom.current().nextInt(200);
            int b = ThreadLocalRandom.current().nextInt(200);
            g.setColor(new Color(r, gr, b, 80));
            g.fillRect(x, y, 1, 1);
        }

        // Draw characters
        int[] xPositions = {8, 32, 56, 80};
        for (int i = 0; i < code.length(); i++) {
            String ch = String.valueOf(code.charAt(i));
            int fontSize = 22 + ThreadLocalRandom.current().nextInt(5);
            int yOffset = ThreadLocalRandom.current().nextInt(-3, 4);

            Font font = new Font("Arial", Font.BOLD, fontSize);
            g.setFont(font);

            int r = 20 + ThreadLocalRandom.current().nextInt(80);
            int gr = 20 + ThreadLocalRandom.current().nextInt(80);
            int b = 20 + ThreadLocalRandom.current().nextInt(80);
            g.setColor(new Color(r, gr, b));

            double angle = Math.toRadians(ThreadLocalRandom.current().nextInt(-25, 26));
            double cx = xPositions[i] + fontSize * 0.35;
            double cy = 28 + yOffset;
            g.rotate(angle, cx, cy);
            g.drawString(ch, xPositions[i], 28 + yOffset);
            g.rotate(-angle, cx, cy);
        }

        g.dispose();
        return image;
    }
}
