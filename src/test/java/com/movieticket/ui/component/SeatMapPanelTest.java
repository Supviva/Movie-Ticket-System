package com.movieticket.ui.component;

import com.movieticket.model.CinemaHall;
import org.junit.jupiter.api.Test;

import javax.swing.SwingUtilities;
import java.awt.event.MouseEvent;
import java.awt.image.BufferedImage;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** 选座图的绘制与交互回归测试。 */
class SeatMapPanelTest {

    /** 座位图绘制路径不应抛异常（含银幕亮带与三种座位态）。 */
    @Test
    void seatMapCanBePainted() throws Exception {
        SwingUtilities.invokeAndWait(() -> {
            CinemaHall hall = new CinemaHall("T01", "测试厅", "2D", 8, 12);
            SeatMapPanel panel = new SeatMapPanel(hall, Set.of("A1", "B2"), () -> {
            }, null);
            panel.setSize(panel.getPreferredSize());
            assertDoesNotThrow(() -> {
                BufferedImage image = new BufferedImage(
                        panel.getWidth(), panel.getHeight(), BufferedImage.TYPE_INT_RGB);
                panel.paint(image.createGraphics());
            });
        });
    }

    /**
     * 点击两个座位之间的间隙不能误选邻座，点座位中心正常选中。
     *
     * <p>旧实现用整数除法定位行列，GAP 间隙会被取整到左侧邻座——
     * 想点空隙却选中了旁边的座位。</p>
     */
    @Test
    void gapClickDoesNotSelectNeighbor() throws Exception {
        SwingUtilities.invokeAndWait(() -> {
            CinemaHall hall = new CinemaHall("T01", "测试厅", "2D", 5, 8);
            SeatMapPanel panel = new SeatMapPanel(hall, Set.of(), () -> {
            }, null);
            panel.setSize(panel.getPreferredSize());

            // A1 与 A2 之间的间隙：座位中心 + 半格 + 半间隙
            java.awt.Point a1 = SeatMapPanel.seatCenter(0, 0);
            java.awt.Point gap = new java.awt.Point(a1.x + 15 + 3, a1.y);
            click(panel, gap.x, gap.y);
            assertTrue(panel.selectedSeats().isEmpty(),
                    "点座位间隙不应选中任何座位");

            // 行间隙同样不应命中
            java.awt.Point a2 = SeatMapPanel.seatCenter(0, 1);
            click(panel, a2.x, a2.y + 15 + 3);
            assertTrue(panel.selectedSeats().isEmpty());

            // 点座位中心正常选中
            java.awt.Point center = SeatMapPanel.seatCenter(0, 1);
            click(panel, center.x, center.y);
            assertEquals(java.util.Set.of("A2"), panel.selectedSeats());
        });
    }

    private static void click(SeatMapPanel panel, int x, int y) {
        MouseEvent event = new MouseEvent(panel, MouseEvent.MOUSE_PRESSED,
                System.currentTimeMillis(), 0, x, y, 1, false);
        for (java.awt.event.MouseListener listener : panel.getMouseListeners()) {
            listener.mousePressed(event);
        }
    }

    /** 点已售座位：选中不变，且通过回调给出「已售出」提示。 */
    @Test
    void soldSeatClickReportsFeedback() throws Exception {
        SwingUtilities.invokeAndWait(() -> {
            CinemaHall hall = new CinemaHall("T01", "测试厅", "2D", 5, 8);
            List<String> feedbacks = new java.util.ArrayList<>();
            SeatMapPanel panel = new SeatMapPanel(hall, Set.of("A3"), () -> {
            }, feedbacks::add);
            panel.setSize(panel.getPreferredSize());

            java.awt.Point sold = SeatMapPanel.seatCenter(0, 2);
            click(panel, sold.x, sold.y);
            assertEquals(java.util.Set.of(), panel.selectedSeats(), "已售座不应被选中");
            assertEquals(1, feedbacks.size());
            assertTrue(feedbacks.get(0).contains("已售出"), "提示应说明座位已售，实际 " + feedbacks);

            // 间隙点击不应触发提示（用户只是点歪了）
            click(panel, sold.x + 18, sold.y);
            assertEquals(1, feedbacks.size(), "点间隙不应触发提示");
        });
    }

    /** 选满 6 座后再点第 7 个：提示上限，已选不变。 */
    @Test
    void sixSeatLimitReportsFeedback() throws Exception {
        SwingUtilities.invokeAndWait(() -> {
            CinemaHall hall = new CinemaHall("T01", "测试厅", "2D", 5, 8);
            List<String> feedbacks = new java.util.ArrayList<>();
            SeatMapPanel panel = new SeatMapPanel(hall, Set.of(), () -> {
            }, feedbacks::add);
            panel.setSize(panel.getPreferredSize());

            for (int col = 0; col < 6; col++) {
                java.awt.Point center = SeatMapPanel.seatCenter(0, col);
                click(panel, center.x, center.y);
            }
            assertEquals(6, panel.selectedSeats().size());

            java.awt.Point seventh = SeatMapPanel.seatCenter(1, 0);
            click(panel, seventh.x, seventh.y);
            assertEquals(6, panel.selectedSeats().size(), "超出上限不应追加");
            assertEquals(1, feedbacks.size());
            assertTrue(feedbacks.get(0).contains("最多"), "提示应说明选择上限，实际 " + feedbacks);
        });
    }

    /** 悬停给出行内 tooltip（含座位号与状态），且不改变选中。 */
    @Test
    void hoverShowsTooltipWithoutSideEffect() throws Exception {
        SwingUtilities.invokeAndWait(() -> {
            CinemaHall hall = new CinemaHall("T01", "测试厅", "2D", 5, 8);
            SeatMapPanel panel = new SeatMapPanel(hall, Set.of("A1"), () -> {
            }, null);
            panel.setSize(panel.getPreferredSize());

            java.awt.Point free = SeatMapPanel.seatCenter(0, 2);
            MouseEvent move = new MouseEvent(panel, MouseEvent.MOUSE_MOVED,
                    System.currentTimeMillis(), 0, free.x, free.y, 0, false);
            for (java.awt.event.MouseMotionListener listener : panel.getMouseMotionListeners()) {
                listener.mouseMoved(move);
            }
            assertEquals(java.util.Set.of(), panel.selectedSeats(), "悬停不应改变选中");
            String tooltip = panel.getToolTipText(move);
            assertTrue(tooltip != null && tooltip.startsWith("A3") && tooltip.contains("可选"),
                    "悬停空座应提示座位号与可选，实际 " + tooltip);

            MouseEvent sold = new MouseEvent(panel, MouseEvent.MOUSE_MOVED,
                    System.currentTimeMillis(), 0,
                    SeatMapPanel.seatCenter(0, 0).x, SeatMapPanel.seatCenter(0, 0).y, 0, false);
            assertTrue(panel.getToolTipText(sold).contains("已售"));

            // 移出画布清空悬停
            MouseEvent exit = new MouseEvent(panel, MouseEvent.MOUSE_EXITED,
                    System.currentTimeMillis(), 0, -20, -20, 0, false);
            for (java.awt.event.MouseListener listener : panel.getMouseListeners()) {
                listener.mouseExited(exit);
            }
            assertEquals(java.util.Set.of(), panel.selectedSeats());
        });
    }
}
