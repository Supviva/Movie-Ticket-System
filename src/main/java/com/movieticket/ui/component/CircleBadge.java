package com.movieticket.ui.component;

import javax.swing.JPanel;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;

public final class CircleBadge extends JPanel {

    private Color fill;
    private final NavIcon icon;

    public CircleBadge(Color fill, NavIcon.Kind kind, Color iconColor, int size) {
        this.fill = fill;
        this.icon = new NavIcon(kind, size - 16, iconColor);
        setPreferredSize(new Dimension(size, size));
        setMinimumSize(new Dimension(size, size));
        setMaximumSize(new Dimension(size, size));
        setOpaque(false);
    }

    /** 允许在会员等级切换时动态更新底色。 */
    public void setFill(Color fill) {
        this.fill = fill;
        repaint();
    }

    @Override
    protected void paintComponent(Graphics graphics) {
        Graphics2D g2 = (Graphics2D) graphics.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2.setColor(fill);
        g2.fillOval(0, 0, getWidth(), getHeight());
        g2.dispose();
        int offset = 8;
        icon.setBounds(offset, offset, icon.getPreferredSize().width, icon.getPreferredSize().height);
        icon.paintComponent(graphics);
    }
}
