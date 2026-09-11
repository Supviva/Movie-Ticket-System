package com.movieticket.ui.component;

import javax.swing.JComponent;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.geom.Path2D;

public final class NavIcon extends JComponent {

    public enum Kind {
        FILM, TICKET, STAR, CART, USER, SEARCH, CHEVRON_LEFT, CHEVRON_RIGHT, GIFT,
        POPCORN, COLA, FRIES, ICE_CREAM, HOTDOG
    }

    private final Kind kind;
    private Color color;

    public NavIcon(Kind kind, int size, Color color) {
        this.kind = kind;
        this.color = color;
        setPreferredSize(new Dimension(size, size));
        setMinimumSize(new Dimension(size, size));
        setMaximumSize(new Dimension(size, size));
        setOpaque(false);
    }

    /** 运行期改色，供导航选中/悬停状态切换使用。 */
    public void setColor(Color color) {
        if (color != null && !color.equals(this.color)) {
            this.color = color;
            repaint();
        }
    }

    public Color color() {
        return color;
    }

    @Override
    public void paintComponent(Graphics graphics) {
        Graphics2D g2 = (Graphics2D) graphics.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2.setColor(color);
        g2.setStroke(new BasicStroke(2f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        int w = getWidth();
        int h = getHeight();
        int pad = Math.max(1, w / 9);
        switch (kind) {
            case FILM -> drawFilm(g2, w, h, pad);
            case TICKET -> drawTicket(g2, w, h, pad);
            case STAR -> drawStar(g2, w / 2, h / 2, (w - pad * 2) / 2);
            case CART -> drawCart(g2, w, h, pad);
            case USER -> drawUser(g2, w, h, pad);
            case SEARCH -> drawSearch(g2, w, h, pad);
            case CHEVRON_LEFT -> drawChevron(g2, w, h, pad, true);
            case CHEVRON_RIGHT -> drawChevron(g2, w, h, pad, false);
            case GIFT -> drawGift(g2, w, h, pad);
            case POPCORN -> drawPopcorn(g2, w, h, pad);
            case COLA -> drawCola(g2, w, h, pad);
            case FRIES -> drawFries(g2, w, h, pad);
            case ICE_CREAM -> drawIceCream(g2, w, h, pad);
            case HOTDOG -> drawHotdog(g2, w, h, pad);
        }
        g2.dispose();
    }

    private void drawFilm(Graphics2D g2, int w, int h, int pad) {
        int top = pad + 3;
        g2.drawRect(pad, top, w - pad * 2, h - top - pad);
        g2.drawLine(pad, top - 2, w - pad, top - 2);
        int flapTop = pad;
        g2.drawLine(pad, flapTop, pad + 2, top);
        g2.drawLine(w / 3, flapTop, w / 3 + 2, top);
        g2.drawLine(2 * w / 3, flapTop, 2 * w / 3 + 2, top);
        int boxW = (w - pad * 2 - 6) / 3;
        for (int i = 0; i < 3; i++) {
            g2.fillRect(pad + 3 + i * (boxW + 3), top + 5, boxW, 4);
        }
    }

    private void drawTicket(Graphics2D g2, int w, int h, int pad) {
        int top = pad + 2;
        g2.drawRoundRect(pad, top, w - pad * 2, h - top - pad - 1, 6, 6);
        g2.drawArc(pad - 3, h / 2 - 3, 6, 6, 90, 180);
        g2.drawArc(w - pad - 3, h / 2 - 3, 6, 6, -90, 180);
        g2.drawLine(pad + 5, h / 2, w - pad - 5, h / 2);
    }

    private void drawStar(Graphics2D g2, int cx, int cy, int radius) {
        Path2D star = new Path2D.Double();
        for (int i = 0; i < 10; i++) {
            double angle = -Math.PI / 2 + i * Math.PI / 5;
            double r = i % 2 == 0 ? radius : radius * 0.45;
            double x = cx + Math.cos(angle) * r;
            double y = cy + Math.sin(angle) * r;
            if (i == 0) {
                star.moveTo(x, y);
            } else {
                star.lineTo(x, y);
            }
        }
        star.closePath();
        g2.draw(star);
    }

    private void drawCart(Graphics2D g2, int w, int h, int pad) {
        g2.drawLine(pad, pad + 1, pad + 4, pad + 1);
        g2.drawLine(pad + 4, pad + 1, pad + 7, h - pad - 5);
        g2.drawLine(pad + 7, h - pad - 5, w - pad - 1, h - pad - 5);
        g2.drawLine(pad + 5, pad + 4, w - pad, pad + 4);
        g2.drawLine(w - pad, pad + 4, w - pad - 2, h - pad - 5);
        g2.drawOval(pad + 8, h - pad - 3, 4, 4);
        g2.drawOval(w - pad - 8, h - pad - 3, 4, 4);
    }

    private void drawUser(Graphics2D g2, int w, int h, int pad) {
        int headR = (w - pad * 2) / 4;
        g2.drawOval(w / 2 - headR, pad + 1, headR * 2, headR * 2);
        g2.drawArc(pad, h / 2, w - pad * 2, h, 20, 140);
    }

    private void drawSearch(Graphics2D g2, int w, int h, int pad) {
        int r = (w - pad * 2) / 2 - 2;
        g2.drawOval(pad, pad, r * 2, r * 2);
        g2.drawLine(pad + r * 2 - 1, pad + r * 2 - 1, w - pad, h - pad);
    }

    private void drawChevron(Graphics2D g2, int w, int h, int pad, boolean left) {
        int mid = h / 2;
        if (left) {
            g2.drawLine(w - pad, pad + 1, pad + 2, mid);
            g2.drawLine(pad + 2, mid, w - pad, h - pad - 1);
        } else {
            g2.drawLine(pad, pad + 1, w - pad - 2, mid);
            g2.drawLine(w - pad - 2, mid, pad, h - pad - 1);
        }
    }

    private void drawGift(Graphics2D g2, int w, int h, int pad) {
        int boxTop = pad + 5;
        g2.drawRect(pad + 1, boxTop, w - pad * 2 - 2, h - boxTop - pad);
        g2.drawLine(pad + 1, boxTop + 3, w - pad - 1, boxTop + 3);
        g2.drawLine(w / 2, boxTop, w / 2, h - pad);
        g2.drawOval(w / 2 - 6, pad, 5, 5);
        g2.drawOval(w / 2 + 1, pad, 5, 5);
    }

    private void drawPopcorn(Graphics2D g2, int w, int h, int pad) {
        int bottom = h - pad;
        g2.drawLine(pad + 1, bottom - 8, w - pad - 1, bottom - 8);
        g2.drawLine(pad + 2, bottom - 8, pad + 5, bottom);
        g2.drawLine(w - pad - 2, bottom - 8, w - pad - 5, bottom);
        for (int i = 0; i < 3; i++) {
            int x = pad + 3 + i * ((w - pad * 2 - 6) / 3);
            g2.drawOval(x, pad, 9, 9);
            g2.drawOval(x + 3, pad + 5, 9, 9);
        }
    }

    private void drawCola(Graphics2D g2, int w, int h, int pad) {
        int top = pad + 4;
        int bottom = h - pad;
        g2.drawRect(pad + 2, top, w - pad * 2 - 4, 6);
        g2.drawLine(pad + 3, top + 6, pad + 6, bottom);
        g2.drawLine(w - pad - 3, top + 6, w - pad - 6, bottom);
        g2.drawLine(pad + 6, bottom, w - pad - 6, bottom);
        g2.drawLine(w / 2 + 3, top + 6, w / 2 - 2, top - 3);
    }

    private void drawFries(Graphics2D g2, int w, int h, int pad) {
        int top = pad + 5;
        int bottom = h - pad;
        g2.drawLine(pad + 3, bottom - 8, w - pad - 3, bottom - 8);
        g2.drawLine(pad + 3, bottom - 8, pad + 6, bottom);
        g2.drawLine(w - pad - 3, bottom - 8, w - pad - 6, bottom);
        for (int i = 0; i < 4; i++) {
            int x = pad + 4 + i * ((w - pad * 2 - 8) / 4);
            g2.drawLine(x, bottom - 8, x, top);
        }
    }

    private void drawIceCream(Graphics2D g2, int w, int h, int pad) {
        int cx = w / 2;
        g2.drawOval(cx - 8, pad, 16, 16);
        g2.drawLine(cx - 8, pad + 9, cx, h - pad - 1);
        g2.drawLine(cx + 8, pad + 9, cx, h - pad - 1);
        g2.drawLine(cx, pad + 9, cx, h - pad - 1);
    }

    private void drawHotdog(Graphics2D g2, int w, int h, int pad) {
        int mid = h / 2;
        g2.drawArc(pad, mid - 6, w - pad * 2, 12, 0, 180);
        g2.drawArc(pad, mid - 6, w - pad * 2, 12, 0, -180);
        g2.drawLine(pad + 2, mid - 6, pad + 4, mid + 6);
        g2.drawLine(w - pad - 2, mid - 6, w - pad - 4, mid + 6);
    }
}
