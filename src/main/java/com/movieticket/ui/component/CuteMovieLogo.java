package com.movieticket.ui.component;

import javax.swing.JPanel;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.GradientPaint;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.RenderingHints;
import java.awt.geom.Path2D;

public final class CuteMovieLogo extends JPanel {

    public CuteMovieLogo(int size) {
        setPreferredSize(new Dimension(size, size));
        setMinimumSize(new Dimension(size, size));
        setMaximumSize(new Dimension(size, size));
        setOpaque(false);
    }

    @Override
    protected void paintComponent(Graphics graphics) {
        super.paintComponent(graphics);
        Graphics2D g2 = (Graphics2D) graphics.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        int width = getWidth();
        int height = getHeight();

        drawLightBeams(g2, width, height);
        drawScatterStars(g2, width, height);

        int bodyWidth = (int) (width * 0.66);
        int bodyHeight = (int) (height * 0.62);
        int bodyX = (width - bodyWidth) / 2;
        int bodyY = (int) (height * 0.23);
        g2.setPaint(new GradientPaint(
                bodyX, bodyY, new Color(0xE3, 0x18, 0x36),
                bodyX + bodyWidth, bodyY + bodyHeight, new Color(0xFF, 0x73, 0x00)));
        g2.fillRoundRect(bodyX, bodyY, bodyWidth, bodyHeight, 32, 32);

        drawFilmHoles(g2, bodyX, bodyY, bodyWidth, bodyHeight);

        int centerX = width / 2;

        int eyeY = bodyY + (int) (bodyHeight * 0.24);
        int eyeRadius = (int) (width * 0.085);
        drawEye(g2, centerX - (int) (width * 0.15), eyeY, eyeRadius);
        drawEye(g2, centerX + (int) (width * 0.15), eyeY, eyeRadius);

        g2.setColor(Color.WHITE);
        g2.setStroke(new BasicStroke(5f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        g2.drawArc(centerX - (int) (width * 0.08), eyeY + eyeRadius + (int) (width * 0.01),
                (int) (width * 0.16), (int) (width * 0.09), 200, 140);

        g2.setColor(new Color(255, 255, 255, 72));
        g2.fillOval(centerX - (int) (width * 0.24), eyeY + eyeRadius + (int) (width * 0.05),
                (int) (width * 0.11), (int) (width * 0.05));
        g2.fillOval(centerX + (int) (width * 0.13), eyeY + eyeRadius + (int) (width * 0.05),
                (int) (width * 0.11), (int) (width * 0.05));

        g2.setColor(Color.WHITE);
        g2.setFont(new Font("Microsoft YaHei", Font.BOLD, Math.max(24, width / 9)));
        String brand = "光影电影";
        FontMetrics metrics = g2.getFontMetrics();
        g2.drawString(brand, (width - metrics.stringWidth(brand)) / 2, bodyY + bodyHeight - 22);

        g2.dispose();
    }

    private static void drawLightBeams(Graphics2D g2, int width, int height) {
        int[] topXs = {(int) (width * 0.28), (int) (width * 0.72)};
        for (int topX : topXs) {
            Path2D beam = new Path2D.Double();
            beam.moveTo(topX - 22, 0);
            beam.lineTo(topX + 22, 0);
            beam.lineTo(topX + (int) (width * 0.20), height);
            beam.lineTo(topX - (int) (width * 0.20), height);
            beam.closePath();
            g2.setPaint(new GradientPaint(
                    0, 0, new Color(255, 209, 102, 82),
                    0, height, new Color(255, 209, 102, 0)));
            g2.fill(beam);
        }
    }

    private static void drawFilmHoles(Graphics2D g2, int x, int y, int width, int height) {
        g2.setColor(new Color(255, 255, 255, 165));
        int holeRadius = Math.max(3, width / 34);
        int leftX = x + holeRadius * 2;
        int rightX = x + width - holeRadius * 2;
        for (int i = 0; i < 4; i++) {
            int holeY = y + height / 4 + i * (height / 5);
            g2.fillOval(leftX - holeRadius, holeY - holeRadius, holeRadius * 2, holeRadius * 2);
            g2.fillOval(rightX - holeRadius, holeY - holeRadius, holeRadius * 2, holeRadius * 2);
        }
    }

    private static void drawEye(Graphics2D g2, int x, int y, int radius) {
        g2.setColor(Color.WHITE);
        g2.fillOval(x - radius, y - radius, radius * 2, radius * 2);
        g2.setColor(new Color(0x2A, 0x1E, 0x2E));
        int pupil = (int) (radius * 0.52);
        g2.fillOval(x - pupil / 2, y - pupil / 2, pupil, pupil);
        g2.setColor(Color.WHITE);
        int glint = (int) (radius * 0.20);
        g2.fillOval(x - pupil / 2 - glint / 2, y - pupil / 2 - glint / 2, glint, glint);
    }

    private static void drawScatterStars(Graphics2D g2, int width, int height) {
        drawStar(g2, (int) (width * 0.17), (int) (height * 0.16), 8, new Color(0xFF, 0xD1, 0x66));
        drawStar(g2, (int) (width * 0.84), (int) (height * 0.24), 11, new Color(0xFF, 0xD1, 0x66));
        drawStar(g2, (int) (width * 0.12), (int) (height * 0.62), 7, new Color(255, 255, 255, 150));
        drawStar(g2, (int) (width * 0.88), (int) (height * 0.72), 7, new Color(255, 255, 255, 150));
    }

    private static void drawStar(Graphics2D g2, int centerX, int centerY, int radius, Color color) {
        Path2D star = new Path2D.Double();
        for (int i = 0; i < 10; i++) {
            double angle = -Math.PI / 2 + i * Math.PI / 5;
            double r = i % 2 == 0 ? radius : radius * 0.42;
            double x = centerX + Math.cos(angle) * r;
            double y = centerY + Math.sin(angle) * r;
            if (i == 0) {
                star.moveTo(x, y);
            } else {
                star.lineTo(x, y);
            }
        }
        star.closePath();
        g2.setColor(color);
        g2.fill(star);
    }
}
