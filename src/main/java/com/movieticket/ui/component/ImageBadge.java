package com.movieticket.ui.component;

import javax.imageio.ImageIO;
import javax.swing.JPanel;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.geom.Ellipse2D;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.InputStream;

/**
 * 圆形图片徽章：小食卖品卡里的圆形头像位。
 *
 * <p>优先显示真实图片（等比铺满并裁切为圆形）；图片缺失时回退为
 * 彩色圆底 + 矢量图标，保证无图资源也不会破坏布局。</p>
 */
public final class ImageBadge extends JPanel {

    private final Color fill;
    private final BufferedImage image;
    private final NavIcon icon;

    public ImageBadge(String imageResource, Color fill, NavIcon.Kind fallbackKind, int size) {
        this.fill = fill;
        this.image = loadImage(imageResource);
        this.icon = image == null ? new NavIcon(fallbackKind, size - 16, Color.WHITE) : null;
        setPreferredSize(new Dimension(size, size));
        setMinimumSize(new Dimension(size, size));
        setMaximumSize(new Dimension(size, size));
        setOpaque(false);
    }

    @Override
    protected void paintComponent(Graphics graphics) {
        Graphics2D g2 = (Graphics2D) graphics.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);

        int w = getWidth();
        int h = getHeight();
        if (image != null) {
            // 圆形裁切 + 等比铺满（cover）
            g2.setClip(new Ellipse2D.Float(0, 0, w, h));
            double scale = Math.max((double) w / image.getWidth(), (double) h / image.getHeight());
            int drawW = (int) Math.round(image.getWidth() * scale);
            int drawH = (int) Math.round(image.getHeight() * scale);
            g2.drawImage(image, (w - drawW) / 2, (h - drawH) / 2, drawW, drawH, null);
            g2.dispose();
            return;
        }

        // 回退：彩色圆底 + 矢量图标（与 CircleBadge 一致）
        g2.setColor(fill);
        g2.fillOval(0, 0, w, h);
        g2.dispose();
        if (icon != null) {
            int offset = 8;
            icon.setBounds(offset, offset, icon.getPreferredSize().width, icon.getPreferredSize().height);
            icon.paintComponent(graphics);
        }
    }

    private static BufferedImage loadImage(String resource) {
        try (InputStream in = ImageBadge.class.getResourceAsStream(resource)) {
            return in == null ? null : ImageIO.read(in);
        } catch (IOException ex) {
            return null;
        }
    }
}
