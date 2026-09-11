package com.movieticket.ui.component;

import com.movieticket.model.Movie;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SwingConstants;
import java.awt.BorderLayout;
import javax.swing.Box;
import javax.swing.BoxLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.function.Consumer;

/**
 * 首页影片卡片：海报 + 评分/格式角标 + 片名 + 类型标签 + 直达购票按钮。
 *
 * <p>相比早期的「海报 + 片名 + 年份」，这里补齐了用户在列表页就需要的决策信息
 * （评分、类型、时长），并让「选座购票」可以在卡片上一步完成，不必先进详情页。
 * 悬停时整卡上浮并高亮描边，让可点击这件事被看见。</p>
 */
public final class MovieCardPanel extends JPanel {

    private static final int HOVER_LIFT = 4;
    private static final int PAD = 10;

    private final Movie movie;
    private final Runnable onOpenDetail;
    private final Consumer<Movie> onBuy;
    private final String badge;

    private boolean hovered;

    /**
     * @param movie        影片
     * @param badge        角标：{@code "presale"} 预售条 / {@code "gift"} 赠券角标 / {@code null} 无
     * @param onOpenDetail 点击卡片进入详情
     * @param onBuy        点击「选座购票」直接购票
     */
    public MovieCardPanel(Movie movie, String badge, Runnable onOpenDetail, Consumer<Movie> onBuy) {
        this.movie = movie;
        this.badge = badge;
        this.onOpenDetail = onOpenDetail;
        this.onBuy = onBuy;

        setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
        setOpaque(false);
        setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        setBorder(new javax.swing.border.EmptyBorder(PAD, PAD, PAD, PAD));

        add(posterBlock());
        add(Box.createVerticalStrut(11));
        add(titleLabel());
        add(Box.createVerticalStrut(6));
        add(tagRow());
        add(Box.createVerticalStrut(11));
        add(buyButton());
        add(Box.createVerticalGlue());

        installHover();
    }

    /** 海报区：图片 + 左下评分 + 右下格式 + 悬停播放遮罩。 */
    private JPanel posterBlock() {
        JPanel holder = new JPanel(new BorderLayout()) {
            @Override
            protected void paintComponent(Graphics graphics) {
                Graphics2D g2 = (Graphics2D) graphics.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(Ui.LINE);
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), 20, 20);
                g2.dispose();
                super.paintComponent(graphics);
            }
        };
        holder.setOpaque(false);
        holder.setAlignmentX(Component.LEFT_ALIGNMENT);

        PosterPanel poster = new PosterPanel(movie, 168, 227, badge);
        holder.add(poster, BorderLayout.CENTER);

        JPanel overlay = new JPanel(null);
        overlay.setOpaque(false);
        JLabel score = chip("★ " + String.format(java.util.Locale.ROOT, "%.1f", movie.rating()),
                new Color(0xFF, 0xB4, 0x00));
        score.setBounds(8, 190, 54, 22);
        overlay.add(score);
        JLabel format = chip(movie.format(), new Color(0xC9, 0xC9, 0xC9));
        format.setBounds(132, 190, 36, 22);
        overlay.add(format);
        holder.add(overlay, BorderLayout.SOUTH);

        return holder;
    }

    /** 半透明小标签，用于海报上的评分与格式。 */
    private static JLabel chip(String text, Color foreground) {
        JLabel label = new JLabel(text, SwingConstants.CENTER) {
            @Override
            protected void paintComponent(Graphics graphics) {
                Graphics2D g2 = (Graphics2D) graphics.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(new Color(0, 0, 0, 200));
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), 12, 12);
                g2.dispose();
                super.paintComponent(graphics);
            }
        };
        label.setOpaque(false);
        label.setForeground(foreground);
        label.setFont(Ui.font(Font.BOLD, 12));
        return label;
    }

    private JLabel titleLabel() {
        JLabel title = new JLabel(ellipsize(movie.title(), 12));
        title.setFont(Ui.font(Font.BOLD, 14));
        title.setForeground(Ui.INK);
        title.setAlignmentX(Component.LEFT_ALIGNMENT);
        title.setToolTipText(movie.title());
        return title;
    }

    /** 类型 / 时长 / 年份 三个小标签。 */
    private JPanel tagRow() {
        JPanel row = new JPanel();
        row.setOpaque(false);
        row.setLayout(new BoxLayout(row, BoxLayout.X_AXIS));
        row.setAlignmentX(Component.LEFT_ALIGNMENT);
        String year = movie.releaseDate().length() >= 4 ? movie.releaseDate().substring(0, 4) : movie.releaseDate();
        row.add(tag(movie.genre()));
        row.add(Box.createHorizontalStrut(6));
        row.add(tag(movie.durationMinutes() + " 分钟"));
        row.add(Box.createHorizontalStrut(6));
        row.add(tag(year));
        row.add(Box.createHorizontalGlue());
        return row;
    }

    private static JLabel tag(String text) {
        JLabel label = new JLabel(text, SwingConstants.CENTER) {
            @Override
            protected void paintComponent(Graphics graphics) {
                Graphics2D g2 = (Graphics2D) graphics.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(new Color(255, 255, 255, 14));
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), 8, 8);
                g2.dispose();
                super.paintComponent(graphics);
            }
        };
        label.setOpaque(false);
        label.setForeground(Ui.MUTED);
        label.setFont(Ui.font(Font.PLAIN, 11));
        label.setBorder(BorderFactory.createEmptyBorder(3, 7, 3, 7));
        return label;
    }

    private JButton buyButton() {
        JButton button = new JButton("选座购票") {
            @Override
            protected void paintComponent(Graphics graphics) {
                Graphics2D g2 = (Graphics2D) graphics.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(hovered ? Ui.PRIMARY_HOVER : Ui.PRIMARY);
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), 16, 16);
                g2.dispose();
                super.paintComponent(graphics);
            }
        };
        button.setOpaque(false);
        button.setContentAreaFilled(false);
        button.setBorderPainted(false);
        button.setFocusPainted(false);
        button.setForeground(Ui.CANVAS);
        button.setFont(Ui.font(Font.BOLD, 13));
        button.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        button.setAlignmentX(Component.LEFT_ALIGNMENT);
        button.setMaximumSize(new Dimension(Integer.MAX_VALUE, 34));
        button.setPreferredSize(new Dimension(148, 34));
        button.addActionListener(event -> onBuy.accept(movie));
        return button;
    }

    /** 悬停：整卡上浮 + 描边高亮 + 播放按钮浮现。 */
    private void installHover() {
        addMouseListener(new MouseAdapter() {
            @Override
            public void mouseEntered(MouseEvent event) {
                hovered = true;
                repaint();
            }

            @Override
            public void mouseExited(MouseEvent event) {
                hovered = false;
                repaint();
            }

            @Override
            public void mouseClicked(MouseEvent event) {
                onOpenDetail.run();
            }
        });
    }

    @Override
    protected void paintComponent(Graphics graphics) {
        Graphics2D g2 = (Graphics2D) graphics.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        int lift = hovered ? HOVER_LIFT : 0;
        int width = getWidth();
        int height = getHeight();
        g2.setColor(hovered ? Ui.PRIMARY_SOFT : Ui.SURFACE);
        g2.fillRoundRect(0, 0, width, Math.max(0, height - HOVER_LIFT), 28, 28);
        g2.setColor(hovered ? Ui.PRIMARY : Ui.LINE);
        g2.drawRoundRect(lift, lift, width - 1, Math.max(0, height - HOVER_LIFT - 1), 28, 28);
        g2.dispose();
        super.paintComponent(graphics);
    }

    /** 卡片在网格中的固定占位尺寸。 */
    public static Dimension preferredCell() {
        return new Dimension(188, 358);
    }

    private static String ellipsize(String text, int max) {
        return text.length() <= max ? text : text.substring(0, max - 1) + "…";
    }
}
