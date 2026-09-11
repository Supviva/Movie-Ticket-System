package com.movieticket.ui.component;

import com.movieticket.model.Movie;

import javax.imageio.ImageIO;
import javax.swing.JPanel;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.GradientPaint;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.geom.RoundRectangle2D;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.InputStream;

public final class PosterPanel extends JPanel {

    private static final Color[] PALETTES = {
            new Color(0x1F, 0x3A, 0x5F),
            new Color(0x29, 0x2C, 0x48),
            new Color(0x8A, 0x3A, 0x2F),
            new Color(0x24, 0x4A, 0x44),
            new Color(0x61, 0x46, 0x27),
            new Color(0x3E, 0x3E, 0x56)
    };

    private final Movie movie;
    private final BufferedImage posterImage;
    private final String badge;

    public PosterPanel(Movie movie, int width, int height) {
        this(movie, width, height, null);
    }

    public PosterPanel(Movie movie, int width, int height, String badge) {
        this.movie = movie;
        this.posterImage = loadPoster(movie.id());
        this.badge = badge;
        setPreferredSize(new Dimension(width, height));
        setMinimumSize(new Dimension(width, height));
        setOpaque(true);
    }

    @Override
    protected void paintComponent(Graphics graphics) {
        super.paintComponent(graphics);
        Graphics2D g2 = (Graphics2D) graphics.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        g2.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);

        int width = getWidth();
        int height = getHeight();
        g2.clip(new RoundRectangle2D.Float(0, 0, width, height, 16, 16));
        if (posterImage != null) {
            drawCover(g2, width, height);
        } else {
            drawFallback(g2, width, height);
        }
        drawBadge(g2, width, height);
        g2.dispose();
    }

    private void drawBadge(Graphics2D g2, int width, int height) {
        if (badge == null) {
            return;
        }
        if ("presale".equals(badge)) {
            int barHeight = 22;
            g2.setColor(new Color(0xFF, 0x73, 0x00));
            g2.fillRect(0, 0, width, barHeight);
            g2.setColor(new Color(0x12, 0x12, 0x12));
            g2.setFont(new Font("Microsoft YaHei", Font.BOLD, 11));
            String text = "预售";
            FontMetrics metrics = g2.getFontMetrics();
            g2.drawString(text, (width - metrics.stringWidth(text)) / 2, barHeight / 2 + 4);
        } else if ("gift".equals(badge)) {
            int size = 26;
            int x = width - size - 6;
            int y = 6;
            g2.setColor(new Color(0x9C, 0x4E, 0xB0));
            g2.fillRoundRect(x, y, size, size, 6, 6);
            g2.setColor(Color.WHITE);
            g2.setStroke(new BasicStroke(2f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
            g2.drawLine(x + 6, y + size / 2, x + size - 6, y + size / 2);
            g2.drawLine(x + size / 2, y + 5, x + size / 2, y + size - 5);
            g2.drawOval(x + size / 2 - 5, y + 3, 4, 4);
            g2.drawOval(x + size / 2 + 1, y + 3, 4, 4);
        }
    }

    private void drawCover(Graphics2D g2, int width, int height) {
        int imgW = posterImage.getWidth();
        int imgH = posterImage.getHeight();
        double scale = Math.max((double) width / imgW, (double) height / imgH);
        int drawW = (int) Math.round(imgW * scale);
        int drawH = (int) Math.round(imgH * scale);
        int x = (width - drawW) / 2;
        int y = (height - drawH) / 2;
        g2.drawImage(posterImage, x, y, drawW, drawH, null);
    }

    /**
     * 无海报图时的占位图。刻意保持克制的图形化风格：
     * 卡片本身已经展示片名/评分/类型/制式，这里不再重复文案，只做调色板 + 几何构图，
     * 避免文字被裁切、也不会与卡片下方的标题重复。
     */
    private void drawFallback(Graphics2D g2, int width, int height) {
        Color base = PALETTES[Math.abs(movie.posterPalette()) % PALETTES.length];
        Color light = lighten(base, 0.18);
        Color deep = darken(base, 0.28);

        // 底色：左上偏亮的斜向渐变，避免纯色平铺的廉价感
        g2.setPaint(new java.awt.GradientPaint(0, 0, light, width, height, deep));
        g2.fillRect(0, 0, width, height);

        // 若隐若现的大圆，作为视觉主体
        int diameter = (int) (Math.min(width, height) * 0.72);
        int cx = width / 2;
        int cy = (int) (height * 0.46);
        g2.setColor(new Color(255, 255, 255, 16));
        g2.fillOval(cx - diameter / 2, cy - diameter / 2, diameter, diameter);
        g2.setColor(new Color(255, 255, 255, 26));
        g2.setStroke(new BasicStroke(1.6f));
        g2.drawOval(cx - diameter / 2, cy - diameter / 2, diameter, diameter);

        // 内圈描边，形成胶片光圈感
        int inner = (int) (diameter * 0.62);
        g2.setColor(new Color(255, 255, 255, 20));
        g2.drawOval(cx - inner / 2, cy - inner / 2, inner, inner);

        // 底部渐深，让占位图与卡片底色自然衔接
        g2.setPaint(new java.awt.GradientPaint(
                0, (float) (height * 0.55), new Color(0, 0, 0, 0),
                0, height, new Color(0, 0, 0, 130)));
        g2.fillRect(0, (int) (height * 0.55), width, height - (int) (height * 0.55));

        // 中央标识：优先取片名的罗马数字/阿拉伯数字续集编号，其次取首字
        String glyph = fallbackGlyph(movie.title());
        if (!glyph.isEmpty()) {
            int fontSize = Math.max(28, Math.min(width / 4, height / 7));
            if (glyph.length() > 1) {
                // 多字符（如 "III"）需要缩小字号才能容纳
                fontSize = Math.max(20, (int) (fontSize / (glyph.length() * 0.62)));
            }
            g2.setFont(new Font("Microsoft YaHei", Font.BOLD, fontSize));
            FontMetrics metrics = g2.getFontMetrics();
            int textWidth = metrics.stringWidth(glyph);
            int baseline = cy + metrics.getAscent() / 2 - metrics.getDescent() / 2 + 2;
            g2.setColor(new Color(255, 255, 255, 235));
            g2.drawString(glyph, cx - textWidth / 2, baseline);
        }
    }

    /**
     * 占位图中央的标识文字。
     * 规则：片名里若带续集编号（中文数字「二三」、罗马数字「II」、阿拉伯数字「2」），
     * 就用编号，避免同一系列多部影片全部显示同一个首字；
     * 否则退化为片名中第一个有意义的字符。
     */
    public static String fallbackGlyph(String title) {
        if (title == null || title.isBlank()) {
            return "";
        }
        String sequel = sequelMark(title);
        if (sequel != null) {
            return sequel;
        }
        for (int i = 0; i < title.length(); i++) {
            char c = title.charAt(i);
            if (Character.isLetterOrDigit(c)) {
                return String.valueOf(c);
            }
        }
        return "";
    }

    /** 提取续集编号；识别不到返回 null。 */
    private static String sequelMark(String title) {
        // 罗马数字（长度优先，避免 "II" 被 "I" 抢先匹配）
        java.util.regex.Matcher roman = java.util.regex.Pattern
                .compile("(VIII|VII|VI|IV|IX|III|II|X|V|I)")
                .matcher(title);
        String best = null;
        while (roman.find()) {
            String hit = roman.group(1);
            if (best == null || hit.length() > best.length()) {
                best = hit;
            }
        }
        if (best != null) {
            return best;
        }
        // 中文数字：阿拉伯数字，或片名末尾的「二/三/四…之X」结构
        java.util.regex.Matcher arabic = java.util.regex.Pattern
                .compile("([2-9]|1[0-9])")
                .matcher(title);
        if (arabic.find()) {
            return arabic.group(1);
        }
        return null;
    }

    private static Color darken(Color color, double factor) {
        int r = Math.max(0, (int) (color.getRed() * (1 - factor)));
        int g = Math.max(0, (int) (color.getGreen() * (1 - factor)));
        int b = Math.max(0, (int) (color.getBlue() * (1 - factor)));
        return new Color(r, g, b);
    }

    private static BufferedImage loadPoster(String id) {
        for (String ext : new String[]{"png", "jpg"}) {
            try (InputStream in = PosterPanel.class.getResourceAsStream("/posters/" + id + "." + ext)) {
                if (in == null) {
                    continue;
                }
                BufferedImage image = ImageIO.read(in);
                if (image != null) {
                    return image;
                }
            } catch (IOException ex) {
                // 尝试下一种格式
            }
        }
        return null;
    }

    private static Color lighten(Color color, double factor) {
        int r = Math.min(255, (int) (color.getRed() + (255 - color.getRed()) * factor));
        int g = Math.min(255, (int) (color.getGreen() + (255 - color.getGreen()) * factor));
        int b = Math.min(255, (int) (color.getBlue() + (255 - color.getBlue()) * factor));
        return new Color(r, g, b);
    }
}
