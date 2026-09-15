package com.movieticket.ui.component;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;
import javax.swing.border.EmptyBorder;
import java.awt.BasicStroke;
import java.awt.BorderLayout;
import java.awt.CardLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.RenderingHints;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

public final class Ui {

    public static final Color CANVAS = new Color(0x12, 0x12, 0x12);
    public static final Color SURFACE = new Color(0x1E, 0x1E, 0x1E);
    public static final Color INK = new Color(0xFF, 0xFF, 0xFF);
    public static final Color INK_SOFT = new Color(0xE7, 0xE8, 0xE8);
    public static final Color MUTED = new Color(0x9E, 0x9E, 0x9E);
    public static final Color LINE = new Color(0x2A, 0x2A, 0x2A);
    public static final Color PRIMARY = new Color(0xFF, 0x73, 0x00);
    public static final Color PRIMARY_HOVER = new Color(0xE0, 0x65, 0x00);
    public static final Color PRIMARY_SOFT = new Color(0x33, 0x29, 0x1F);
    public static final Color LINK = new Color(0x34, 0x78, 0xC1);
    public static final Color PURPLE = new Color(0x9C, 0x4E, 0xB0);
    public static final Color SUCCESS = new Color(0x16, 0xA3, 0x4A);
    public static final Color WARNING = new Color(0xD9, 0x77, 0x06);
    public static final Color GOLD = new Color(0xBE, 0x8A, 0x2A);
    public static final Color SILVER = new Color(0xD6, 0xDB, 0xE2);
    public static final Color PLATINUM = new Color(0x67, 0xC6, 0xC1);
    public static final Color DIAMOND = new Color(0x7F, 0xB4, 0xFF);
    public static final Color DANGER = new Color(0xDC, 0x26, 0x26);
    /** 评分高亮用的金色，深色底上比 GOLD 更醒目。 */
    public static final Color SCORE = new Color(0xFF, 0xB4, 0x00);
    /** 搜索框、chip 悬停等次级填充面。 */
    public static final Color RAISED = new Color(0x2A, 0x2A, 0x2A);
    /** 悬停时比 RAISED 再亮一档的填充面。 */
    public static final Color RAISED_HOVER = new Color(0x4A, 0x4A, 0x4A);

    /** 选座图：银幕装饰、已售/可选座位的暗色系，SeatMapPanel 与图例共用。 */
    public static final Color SCREEN_FILL = new Color(255, 255, 255, 18);
    public static final Color SCREEN_EDGE = new Color(255, 255, 255, 36);
    public static final Color SOLD = new Color(0x33, 0x38, 0x3F);
    public static final Color SOLD_EDGE = new Color(0x44, 0x4A, 0x52);
    public static final Color SEAT_EDGE = new Color(0x55, 0x5B, 0x63);

    /** @deprecated 与 {@link #PRIMARY} 同值，保留仅为兼容历史调用。 */
    @Deprecated
    public static final Color ORANGE = PRIMARY;
    /** @deprecated 与 {@link #LINE} 同值，保留仅为兼容历史调用。 */
    @Deprecated
    public static final Color DISABLED = LINE;

    public static final Font FONT = font(Font.PLAIN, 14);
    public static final Font FONT_MEDIUM = font(Font.BOLD, 14);

    private Ui() {
    }

    public static Font font(int style, int size) {
        return new Font("Microsoft YaHei", style, size);
    }

    public static void installDefaults() {
        UIManager.put("Label.font", FONT);
        UIManager.put("Label.foreground", INK);
        UIManager.put("Panel.background", CANVAS);
        UIManager.put("Button.font", FONT);
        UIManager.put("TextField.font", FONT);
        UIManager.put("TextField.background", SURFACE);
        UIManager.put("TextField.foreground", INK);
        UIManager.put("TextField.caretForeground", INK);
        UIManager.put("PasswordField.font", FONT);
        UIManager.put("ComboBox.font", FONT);
        UIManager.put("ComboBox.background", SURFACE);
        UIManager.put("ComboBox.foreground", INK);
        UIManager.put("Table.font", font(Font.PLAIN, 14));
        UIManager.put("TableHeader.font", font(Font.BOLD, 13));
        UIManager.put("TableHeader.foreground", MUTED);
        UIManager.put("TableHeader.background", SURFACE);
        UIManager.put("Table.foreground", INK_SOFT);
        UIManager.put("Table.background", SURFACE);
        UIManager.put("Table.selectionBackground", new Color(0x2A, 0x2A, 0x2A));
        UIManager.put("Table.selectionForeground", INK);
        UIManager.put("Table.gridColor", LINE);
        UIManager.put("ScrollPane.border", BorderFactory.createEmptyBorder());
        UIManager.put("Viewport.background", CANVAS);
    }

    public static JLabel label(String text) {
        JLabel label = new JLabel(text);
        label.setForeground(INK);
        return label;
    }

    public static JLabel label(String text, Color color) {
        JLabel label = label(text);
        label.setForeground(color);
        return label;
    }

    public static JLabel subtle(String text) {
        JLabel label = label(text, MUTED);
        return label;
    }

    public static JLabel heading(String text, int size) {
        JLabel label = label(text, INK);
        label.setFont(font(Font.BOLD, size));
        return label;
    }

    public static JLabel fieldLabel(String text) {
        JLabel label = label(text, INK_SOFT);
        label.setFont(font(Font.BOLD, 13));
        return label;
    }

    public static JButton button(String text, Style style) {
        StyledButton button = new StyledButton(text, style);
        return button;
    }

    public static JButton primary(String text) {
        return button(text, Style.PRIMARY);
    }

    public static JButton secondary(String text) {
        return button(text, Style.SECONDARY);
    }

    public static JButton textButton(String text, Color color) {
        return button(text, Style.TEXT.withForeground(color));
    }

    public static JButton danger(String text) {
        return button(text, Style.TEXT.withForeground(DANGER));
    }

    public static JPanel panel(Color background) {
        JPanel panel = new JPanel();
        panel.setBackground(background);
        return panel;
    }

    public static RoundedPanel card() {
        return new RoundedPanel(SURFACE, 10);
    }

    public static RoundedPanel card(Color color, int radius) {
        return new RoundedPanel(color, radius);
    }

    public static void addSpacing(JComponent component, int width, int height) {
        component.setPreferredSize(new Dimension(width, height));
    }

    /**
     * 会员等级的主题色。配色属于表现层，故放在这里而非模型枚举中。
     *
     * @param level 会员等级
     * @return 该等级的主题色
     */
    public static Color tierColor(com.movieticket.model.MembershipLevel level) {
        return switch (level) {
            case NORMAL -> PRIMARY;
            case SILVER -> SILVER;
            case GOLD -> GOLD;
            case PLATINUM -> PLATINUM;
            case DIAMOND -> DIAMOND;
        };
    }

    /**
     * 会员等级卡的柔和底色，与 {@link #tierColor} 同色系，适配深色主题。
     *
     * @param level 会员等级
     * @return 卡片背景色
     */
    public static Color tierSoftColor(com.movieticket.model.MembershipLevel level) {
        return switch (level) {
            case NORMAL -> SURFACE;
            case SILVER -> new Color(0x24, 0x26, 0x2A);
            case GOLD -> new Color(0x2B, 0x24, 0x14);
            case PLATINUM -> new Color(0x14, 0x26, 0x2A);
            case DIAMOND -> new Color(0x14, 0x1F, 0x2E);
        };
    }

    public static JScrollPane scroll(JComponent content) {
        JScrollPane scroll = new JScrollPane(content);
        scroll.setBorder(BorderFactory.createLineBorder(LINE));
        scroll.getVerticalScrollBar().setUnitIncrement(16);
        return scroll;
    }

    public static JScrollPane scrollNoBorder(JComponent content) {
        JScrollPane scroll = new JScrollPane(content);
        scroll.setBorder(BorderFactory.createEmptyBorder());
        scroll.getVerticalScrollBar().setUnitIncrement(16);
        return scroll;
    }

    public static void configureTable(JTable table) {
        table.setRowHeight(34);
        table.setShowVerticalLines(false);
        table.setShowHorizontalLines(true);
        table.setFillsViewportHeight(true);
        table.setSelectionMode(javax.swing.ListSelectionModel.SINGLE_SELECTION);
        table.getTableHeader().setReorderingAllowed(false);
        table.setBorder(BorderFactory.createEmptyBorder(0, 8, 0, 8));
    }

    public static void info(Component parent, String title, String message) {
        JOptionPane.showMessageDialog(parent, message, title, JOptionPane.INFORMATION_MESSAGE);
    }

    public static void error(Component parent, String title, String message) {
        JOptionPane.showMessageDialog(parent, message, title, JOptionPane.ERROR_MESSAGE);
    }

    public static boolean confirm(Component parent, String title, String message) {
        return JOptionPane.showConfirmDialog(parent, message, title,
                JOptionPane.OK_CANCEL_OPTION, JOptionPane.WARNING_MESSAGE) == JOptionPane.OK_OPTION;
    }

    public static boolean showFormDialog(Component parent, String title, JComponent content, int width, int height) {
        java.awt.Window owner = SwingUtilities.getWindowAncestor(parent);
        JDialog dialog = new JDialog(owner, title, JDialog.ModalityType.APPLICATION_MODAL);
        dialog.setDefaultCloseOperation(JDialog.DISPOSE_ON_CLOSE);
        dialog.setResizable(false);

        JPanel root = new JPanel(new BorderLayout());
        root.setBackground(SURFACE);
        root.add(content, BorderLayout.CENTER);

        JPanel footer = new JPanel(new java.awt.FlowLayout(java.awt.FlowLayout.RIGHT, 8, 10));
        footer.setBackground(SURFACE);
        JButton cancel = secondary("取消");
        JButton ok = primary("确定");
        footer.add(cancel);
        footer.add(ok);
        root.add(footer, BorderLayout.SOUTH);

        boolean[] accepted = {false};
        cancel.addActionListener(event -> dialog.dispose());
        ok.addActionListener(event -> {
            accepted[0] = true;
            dialog.dispose();
        });
        dialog.setContentPane(root);
        dialog.pack();
        dialog.setSize(Math.max(dialog.getWidth(), width), Math.max(dialog.getHeight(), height));
        dialog.setLocationRelativeTo(owner);
        dialog.setVisible(true);
        return accepted[0];
    }

    public static String statusText(String status) {
        return switch (status) {
            case "UNPAID" -> "待支付";
            case "PAID" -> "已出票";
            case "CANCELED" -> "已取消";
            default -> status;
        };
    }

    /**
     * 空状态面板：列表/表格无数据时的居中占位，替代一片空白。
     *
     * <p>文案分两行：{@code title} 是结论（如「暂无数据」），
     * {@code hint} 是原因或下一步提示；{@code action} 可为空。</p>
     *
     * @param title  主文案
     * @param hint   副文案，可为 null
     * @param action 可选的操作按钮，可为 null
     * @return 可直接放进 CENTER 的占位面板
     */
    public static JPanel emptyState(String title, String hint, JComponent action) {
        JPanel panel = new JPanel(new GridBagLayout());
        panel.setBackground(SURFACE);

        JPanel box = new JPanel();
        box.setOpaque(false);
        box.setLayout(new BoxLayout(box, BoxLayout.Y_AXIS));

        JLabel glyph = new JLabel() {
            @Override
            protected void paintComponent(Graphics graphics) {
                Graphics2D g2 = (Graphics2D) graphics.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
                        RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(new Color(0x4A, 0x4A, 0x4A));
                g2.setStroke(new BasicStroke(2f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
                int x = 6;
                int y = 6;
                int w = getWidth() - 12;
                int h = getHeight() - 12;
                // 文档轮廓
                g2.drawRoundRect(x, y, w, h, 4, 4);
                // 内容行
                int lineX = x + (int) (w * 0.22);
                int lineW = (int) (w * 0.56);
                for (int i = 0; i < 3; i++) {
                    int ly = y + (int) (h * (0.3 + i * 0.19));
                    g2.drawLine(lineX, ly, lineX + (i == 2 ? lineW / 2 : lineW), ly);
                }
                g2.dispose();
            }
        };
        glyph.setPreferredSize(new Dimension(52, 52));
        glyph.setMinimumSize(new Dimension(52, 52));
        glyph.setMaximumSize(new Dimension(52, 52));
        glyph.setAlignmentX(Component.CENTER_ALIGNMENT);
        box.add(glyph);
        box.add(Box.createVerticalStrut(12));

        JLabel titleLabel = label(title, INK_SOFT);
        titleLabel.setFont(font(Font.BOLD, 15));
        titleLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
        box.add(titleLabel);

        if (hint != null && !hint.isBlank()) {
            box.add(Box.createVerticalStrut(7));
            JLabel hintLabel = label(hint, MUTED);
            hintLabel.setFont(font(Font.PLAIN, 13));
            hintLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
            box.add(hintLabel);
        }
        if (action != null) {
            box.add(Box.createVerticalStrut(16));
            action.setAlignmentX(Component.CENTER_ALIGNMENT);
            box.add(action);
        }
        panel.add(box);
        return panel;
    }

    /**
     * 表格 + 空状态的双层容器。数据为空时自动显示空状态面板。
     *
     * <p>Swing 的 {@link JTable} 在模型为空时只渲染表头，下方是一片空白，
     * 用户会误以为页面坏了。这里用 {@link CardLayout} 在「表格」与「空状态」
     * 之间切换，让占位信息有地方显示。</p>
     *
     * <p>表头行始终可见：空状态面板只占据表体区域，标题栏保留在上方。</p>
     */
    public static final class TableCard extends JPanel {
        private final CardLayout layout = new CardLayout();
        private final JTable table;
        private final JPanel emptyHolder = new JPanel(new BorderLayout());

        private static final String CARD_TABLE = "table";
        private static final String CARD_EMPTY = "empty";

        /**
         * @param table    数据表
         * @param scroller 已包裹该表的滚动容器（{@link #scroll} 的产物）
         */
        public TableCard(JTable table, JComponent scroller) {
            super();
            this.table = table;
            setLayout(layout);
            setBackground(SURFACE);
            emptyHolder.setBackground(SURFACE);
            add(scroller, CARD_TABLE);
            add(emptyHolder, CARD_EMPTY);
        }

        /**
         * 根据当前行数切换展示。行数为 0 时显示空状态。
         *
         * @param emptyTitle 主文案
         * @param emptyHint  副文案，可为 null
         * @param action     可选操作按钮，可为 null
         */
        public void sync(String emptyTitle, String emptyHint, JComponent action) {
            if (table.getRowCount() > 0) {
                layout.show(this, CARD_TABLE);
                return;
            }
            emptyHolder.removeAll();
            emptyHolder.add(emptyState(emptyTitle, emptyHint, action), BorderLayout.CENTER);
            emptyHolder.revalidate();
            emptyHolder.repaint();
            layout.show(this, CARD_EMPTY);
        }

        public void sync(String emptyTitle, String emptyHint) {
            sync(emptyTitle, emptyHint, null);
        }
    }

    /**
     * 为导航项安装「选中 / 悬停」的双重视觉反馈。
     *
     * <p>选中态用主色文字 + {@link #PRIMARY_SOFT} 底色；悬停态只改文字色，
     * 不抢选中态的视觉权重。传入的 {@code applyState} 回调由调用方负责
     * 真正修改组件（文字色、底色、指示条），本方法只管状态机与鼠标事件。</p>
     */
    public static final class NavSelection {
        private final JComponent[] items;
        private final java.util.function.BiConsumer<JComponent, Boolean> applyState;
        /** -1 表示「尚未选中任何项」，这样首次 {@link #select(int)} 一定会真正施加选中态。 */
        private int selectedIndex = -1;

        /**
         * @param items      所有导航项，顺序即索引顺序
         * @param applyState (组件, 是否高亮) -> 如何呈现
         */
        public NavSelection(JComponent[] items,
                            java.util.function.BiConsumer<JComponent, Boolean> applyState) {
            this.items = items.clone();
            this.applyState = applyState;
            for (int i = 0; i < this.items.length; i++) {
                final int index = i;
                JComponent item = this.items[i];
                item.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
                item.addMouseListener(new MouseAdapter() {
                    @Override
                    public void mouseEntered(MouseEvent event) {
                        if (index != selectedIndex) {
                            applyState.accept(item, true);
                        }
                    }

                    @Override
                    public void mouseExited(MouseEvent event) {
                        if (index != selectedIndex) {
                            applyState.accept(item, false);
                        }
                    }
                });
            }
        }

        /** 切换选中项；越界不做任何事，重复选中同一项则跳过。 */
        public void select(int index) {
            if (index < 0 || index >= items.length || index == selectedIndex) {
                return;
            }
            if (selectedIndex >= 0) {
                applyState.accept(items[selectedIndex], false);
            }
            selectedIndex = index;
            applyState.accept(items[index], true);
        }

        public int selectedIndex() {
            return selectedIndex;
        }
    }

    public static final class Style {
        public static final Style PRIMARY =
                new Style(Ui.PRIMARY, SURFACE, PRIMARY_HOVER, false);
        public static final Style SECONDARY =
                new Style(SURFACE, INK, new Color(0x2A, 0x2A, 0x2A), true);
        public static final Style TEXT =
                new Style(new Color(0, 0, 0, 0), INK, new Color(0, 0, 0, 0), false);

        private final Color background;
        private final Color foreground;
        private final Color hover;
        private final boolean border;

        private Style(Color background, Color foreground, Color hover, boolean border) {
            this.background = background;
            this.foreground = foreground;
            this.hover = hover;
            this.border = border;
        }

        public Style withForeground(Color color) {
            return new Style(background, color, hover, border);
        }
    }

    public static final class RoundedPanel extends JPanel {
        private final int radius;
        private Color fill;

        public RoundedPanel(Color fill, int radius) {
            this.fill = fill;
            this.radius = radius;
            setOpaque(false);
        }

        /** 允许动态切换卡片底色（如会员等级变化时）。 */
        public void setFill(Color color) {
            this.fill = color;
            repaint();
        }

        @Override
        protected void paintComponent(Graphics graphics) {
            Graphics2D g2 = (Graphics2D) graphics.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setColor(fill);
            g2.fillRoundRect(0, 0, getWidth(), getHeight(), radius * 2, radius * 2);
            g2.dispose();
            super.paintComponent(graphics);
        }
    }

    public static final class StyledButton extends JButton {
        private final Style style;

        public StyledButton(String text, Style style) {
            super(text);
            this.style = style;
            setOpaque(false);
            setContentAreaFilled(false);
            setBorderPainted(false);
            setFocusPainted(false);
            setForeground(style.foreground);
            setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
            setBorder(new EmptyBorder(9, 18, 9, 18));
            addMouseListener(new MouseAdapter() {
                @Override
                public void mouseEntered(MouseEvent event) {
                    if (isEnabled()) {
                        setBackground(style.hover);
                        repaint();
                    }
                }

                @Override
                public void mouseExited(MouseEvent event) {
                    setBackground(style.background);
                    repaint();
                }
            });
        }

        @Override
        protected void paintComponent(Graphics graphics) {
            Graphics2D g2 = (Graphics2D) graphics.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            Color fill = style.background;
            if (!isEnabled()) {
                fill = DISABLED;
            }
            g2.setColor(fill);
            int radius = Math.min(10, getHeight() / 2);
            g2.fillRoundRect(0, 0, getWidth(), getHeight(), radius * 2, radius * 2);
            if (style.border) {
                g2.setColor(LINE);
                g2.setStroke(new BasicStroke(1f));
                g2.drawRoundRect(0, 0, getWidth() - 1, getHeight() - 1, radius * 2, radius * 2);
            }
            g2.dispose();
            if (!isEnabled()) {
                setForeground(Color.WHITE);
            } else {
                setForeground(style.foreground);
            }
            super.paintComponent(graphics);
        }
    }
}
