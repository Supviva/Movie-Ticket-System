package com.movieticket.ui.component;

import com.movieticket.model.CinemaHall;

import javax.swing.JPanel;
import javax.swing.ToolTipManager;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.Set;
import java.util.TreeSet;
import java.util.function.Consumer;

public final class SeatMapPanel extends JPanel {

    private static final int CELL = 30;
    private static final int GAP = 7;
    private static final int TOP = 74;
    private static final int LEFT = 44;
    /** 单次最多可选座位数。 */
    private static final int MAX_SEATS = 6;

    private final CinemaHall hall;
    private final Set<String> soldSeats;
    private final Set<String> selectedSeats = new TreeSet<>();
    private final Runnable onChange;
    /** 被拒绝的操作（点已售座/超上限）通过它给出行内提示，可为 null。 */
    private final Consumer<String> onFeedback;
    private int hoverRow = -1;
    private int hoverCol = -1;

    public SeatMapPanel(CinemaHall hall, Set<String> soldSeats,
                        Runnable onChange, Consumer<String> onFeedback) {
        this.hall = hall;
        this.soldSeats = soldSeats;
        this.onChange = onChange;
        this.onFeedback = onFeedback;
        setOpaque(false);
        setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        ToolTipManager.sharedInstance().registerComponent(this);
        addMouseListener(new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent event) {
                handleClick(event.getX(), event.getY());
            }

            @Override
            public void mouseExited(MouseEvent event) {
                setHover(-1, -1);
            }
        });
        addMouseMotionListener(new java.awt.event.MouseMotionAdapter() {
            @Override
            public void mouseMoved(MouseEvent event) {
                int[] hit = seatAt(event.getX(), event.getY());
                int row = hit == null ? -1 : hit[0];
                int col = hit == null ? -1 : hit[1];
                setHover(row, col);
            }
        });
    }

    /** 更新悬停座位，仅在变化时重绘，避免鼠标乱动时反复刷屏。 */
    private void setHover(int row, int col) {
        if (hoverRow != row || hoverCol != col) {
            hoverRow = row;
            hoverCol = col;
            repaint();
        }
    }

    private void handleClick(int x, int y) {
        int[] hit = seatAt(x, y);
        if (hit == null) {
            return;
        }
        int row = hit[0];
        int col = hit[1];
        String seat = seatKey(row, col);
        if (soldSeats.contains(seat)) {
            feedback(seat + " 已售出，请换一个座位");
            return;
        }
        if (selectedSeats.contains(seat)) {
            selectedSeats.remove(seat);
        } else if (selectedSeats.size() < MAX_SEATS) {
            selectedSeats.add(seat);
        } else {
            feedback("一次最多可选 " + MAX_SEATS + " 个座位");
            return;
        }
        onChange.run();
        repaint();
    }

    private void feedback(String message) {
        if (onFeedback != null) {
            onFeedback.accept(message);
        }
    }

    /** 按悬停位置给出座位状态提示。 */
    @Override
    public String getToolTipText(MouseEvent event) {
        int[] hit = seatAt(event.getX(), event.getY());
        if (hit == null) {
            return null;
        }
        String seat = seatKey(hit[0], hit[1]);
        if (soldSeats.contains(seat)) {
            return seat + " · 已售";
        }
        return selectedSeats.contains(seat) ? seat + " · 已选择" : seat + " · 可选";
    }

    /**
     * 命中测试：返回座位 (row, col)；落在座位间隙或画布外时返回 null。
     *
     * <p>不能只用整数除法定位——除法会把 GAP 间隙取整到左侧邻座，
     * 导致想点空隙却误选了旁边的座位。所以算出行列后还要验证
     * 坐标确实落在该座位的矩形内。</p>
     */
    private int[] seatAt(int x, int y) {
        int col = (x - LEFT) / (CELL + GAP);
        int row = (y - TOP) / (CELL + GAP);
        if (row < 0 || row >= hall.rows() || col < 0 || col >= hall.cols()) {
            return null;
        }
        if (x >= xForCol(col) + CELL || y >= yForRow(row) + CELL) {
            return null;
        }
        return new int[]{row, col};
    }

    /** 座位 (row, col) 的中心像素，供测试与命中判定共用。 */
    public static java.awt.Point seatCenter(int row, int col) {
        return new java.awt.Point(
                LEFT + col * (CELL + GAP) + CELL / 2,
                TOP + row * (CELL + GAP) + CELL / 2);
    }

    public Set<String> selectedSeats() {
        return new TreeSet<>(selectedSeats);
    }

    public void clearSelection() {
        selectedSeats.clear();
        repaint();
    }

    @Override
    public Dimension getPreferredSize() {
        int width = LEFT + hall.cols() * CELL + (hall.cols() - 1) * GAP + 34;
        int height = TOP + hall.rows() * CELL + (hall.rows() - 1) * GAP + 24;
        return new Dimension(width, height);
    }

    @Override
    protected void paintComponent(Graphics graphics) {
        super.paintComponent(graphics);
        Graphics2D g2 = (Graphics2D) graphics.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2.setFont(new Font("Microsoft YaHei", Font.PLAIN, 12));

        int screenWidth = hall.cols() * CELL + (hall.cols() - 1) * GAP;
        // 发光银幕：半透明亮带 + 向座位区渐隐的光束，替代原来的亮白块
        g2.setColor(Ui.SCREEN_FILL);
        g2.fillRoundRect(LEFT - 20, 14, screenWidth + 40, 36, 14, 14);
        g2.setColor(Ui.SCREEN_EDGE);
        g2.setStroke(new BasicStroke(1f));
        g2.drawRoundRect(LEFT - 20, 14, screenWidth + 40, 36, 14, 14);
        g2.setPaint(new java.awt.GradientPaint(0, 50, Ui.SCREEN_FILL,
                0, 110, new Color(255, 255, 255, 0)));
        g2.fillRect(LEFT - 20, 50, screenWidth + 40, 60);
        g2.setColor(Ui.MUTED);
        g2.drawString("银幕", LEFT + screenWidth / 2 - 18, 38);

        for (int row = 0; row < hall.rows(); row++) {
            String rowLabel = String.valueOf((char) ('A' + row));
            g2.setColor(Ui.MUTED);
            g2.drawString(rowLabel, 14, yForRow(row) + CELL / 2 + 4);
            for (int col = 0; col < hall.cols(); col++) {
                int x = xForCol(col);
                int y = yForRow(row);
                String seat = seatKey(row, col);
                Color fill;
                Color stroke;
                if (soldSeats.contains(seat)) {
                    fill = Ui.SOLD;
                    stroke = Ui.SOLD_EDGE;
                } else if (selectedSeats.contains(seat)) {
                    fill = Ui.PRIMARY;
                    stroke = Ui.PRIMARY_HOVER;
                } else {
                    fill = Ui.SURFACE;
                    stroke = Ui.SEAT_EDGE;
                }
                g2.setColor(fill);
                g2.fillRoundRect(x, y, CELL, CELL, 8, 8);
                g2.setColor(stroke);
                g2.drawRoundRect(x, y, CELL, CELL, 8, 8);
                // 悬停的可选座位加主色描边；已售座不高亮（反正点不了）
                if (row == hoverRow && col == hoverCol && !soldSeats.contains(seat)) {
                    g2.setColor(Ui.PRIMARY_HOVER);
                    g2.setStroke(new BasicStroke(2f));
                    g2.drawRoundRect(x - 1, y - 1, CELL + 2, CELL + 2, 9, 9);
                }
            }
        }
        g2.dispose();
    }

    private int xForCol(int col) {
        return LEFT + col * (CELL + GAP);
    }

    private int yForRow(int row) {
        return TOP + row * (CELL + GAP);
    }

    private static String seatKey(int row, int col) {
        return String.valueOf((char) ('A' + row)) + (col + 1);
    }
}
