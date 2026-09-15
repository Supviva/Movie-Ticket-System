package com.movieticket.ui.component;

import com.movieticket.demo.DemoMovieTicketService;
import com.movieticket.model.Movie;
import org.junit.jupiter.api.Test;

import javax.swing.SwingUtilities;
import java.awt.event.MouseEvent;
import java.awt.image.BufferedImage;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 首页主视觉自动轮播的回归测试。
 *
 * <p>与主视觉同类（{@code com.movieticket.ui.component} 包），
 * 以便直接访问 {@code nextIndex}/{@code advance} 等包内 API。</p>
 */
class HeroBannerPanelTest {

    /** 循环推进的纯函数边界：末尾折返、正常推进、单部与空列表。 */
    @Test
    void nextIndexWrapsAround() {
        assertEquals(0, HeroBannerPanel.nextIndex(5, 6), "末尾应折返第 0 部");
        assertEquals(3, HeroBannerPanel.nextIndex(2, 6));
        assertEquals(1, HeroBannerPanel.nextIndex(0, 2));
        assertEquals(0, HeroBannerPanel.nextIndex(0, 1), "单部影片推进后仍是第 0 部");
        assertEquals(0, HeroBannerPanel.nextIndex(0, 0), "空列表不应抛异常");
        assertEquals(0, HeroBannerPanel.nextIndex(7, 0), "空列表不应抛异常");
    }

    /** 多部影片自动开播，可暂停恢复；单部影片不建定时器。 */
    @Test
    void heroAutoPlaysAndPauses() throws Exception {
        SwingUtilities.invokeAndWait(() -> {
            DemoMovieTicketService service = new DemoMovieTicketService();
            List<Movie> movies = service.movies();

            HeroBannerPanel hero = new HeroBannerPanel(movies.subList(0, 6),
                    movie -> { }, movie -> { }, null);
            assertTrue(hero.isAutoPlaying(), "多部影片构造后应自动轮播");

            hero.pause();
            assertFalse(hero.isAutoPlaying(), "暂停后应停止轮播");
            hero.resume();
            assertTrue(hero.isAutoPlaying(), "恢复后应继续轮播");

            // 手动暂停后再重新挂载（切回首页触发 addNotify）不应复活
            hero.pause();
            hero.addNotify();
            assertFalse(hero.isAutoPlaying(),
                    "外部暂停后重新挂载不应被 addNotify 覆盖");

            HeroBannerPanel single = new HeroBannerPanel(movies.subList(0, 1),
                    movie -> { }, movie -> { }, null);
            assertFalse(single.isAutoPlaying(), "单部影片不应建自动轮播");
            single.pause();
            single.resume();
            assertFalse(single.isAutoPlaying(), "单部影片 resume 也应保持停止");
        });
    }

    /**
     * 悬停暂停、真正离开才恢复。
     *
     * <p>关键防抖语义：mouseExited 的坐标仍在 hero 边界内（即只是移到了
     * 子组件上）时不能恢复轮播，否则按钮间的移动会让轮播反复启停。</p>
     */
    @Test
    void heroHoverPausesWithoutJitter() throws Exception {
        SwingUtilities.invokeAndWait(() -> {
            DemoMovieTicketService service = new DemoMovieTicketService();
            List<Movie> movies = service.movies();
            HeroBannerPanel hero = new HeroBannerPanel(movies.subList(0, 6),
                    movie -> { }, movie -> { }, null);
            hero.setSize(1240, 340);

            assertTrue(hero.isAutoPlaying());

            dispatchMouse(hero, MouseEvent.MOUSE_ENTERED, 400, 100);
            assertFalse(hero.isAutoPlaying(), "悬停应暂停轮播");

            // 移到子组件上：坐标仍在 hero 内，不应恢复
            dispatchMouse(hero, MouseEvent.MOUSE_EXITED, 400, 100);
            assertFalse(hero.isAutoPlaying(), "坐标仍在边界内时 mouseExited 不应恢复轮播");

            // 真正离开：坐标在边界外
            dispatchMouse(hero, MouseEvent.MOUSE_EXITED, -20, -20);
            assertTrue(hero.isAutoPlaying(), "真正移出后应恢复轮播");

            // 重复事件不改变状态
            dispatchMouse(hero, MouseEvent.MOUSE_EXITED, -20, -20);
            assertTrue(hero.isAutoPlaying());
            dispatchMouse(hero, MouseEvent.MOUSE_ENTERED, 400, 100);
            dispatchMouse(hero, MouseEvent.MOUSE_ENTERED, 400, 100);
            assertFalse(hero.isAutoPlaying());
        });
    }

    /** advance 循环推进；末尾折返第 0 部。 */
    @Test
    void advanceCyclesThroughMovies() throws Exception {
        SwingUtilities.invokeAndWait(() -> {
            DemoMovieTicketService service = new DemoMovieTicketService();
            List<Movie> movies = service.movies();
            HeroBannerPanel hero = new HeroBannerPanel(movies.subList(0, 6),
                    movie -> { }, movie -> { }, null);

            assertEquals(0, hero.index());
            hero.advance();
            assertEquals(1, hero.index());

            hero.show(5);
            assertEquals(5, hero.index());
            hero.advance();
            assertEquals(0, hero.index(), "末尾应折返第 0 部");
            assertEquals(movies.get(0), hero.current());
        });
    }

    /** 6 部主推影片逐张切换渲染：海报卡片 + 氛围模糊层都不得抛异常。 */
    @Test
    void heroPosterCardAndAmbientRenderOnAllSlides() throws Exception {
        SwingUtilities.invokeAndWait(() -> {
            DemoMovieTicketService service = new DemoMovieTicketService();
            List<Movie> movies = service.movies();
            HeroBannerPanel hero = new HeroBannerPanel(movies.subList(0, 6),
                    movie -> { }, movie -> { }, null);
            hero.setSize(1240, 340);
            for (int i = 0; i < 6; i++) {
                hero.show(i);
                BufferedImage image = new BufferedImage(
                        1240, 340, BufferedImage.TYPE_INT_RGB);
                assertDoesNotThrow(() -> hero.paint(image.createGraphics()));
            }
        });
    }

    private static void dispatchMouse(HeroBannerPanel target, int eventId, int x, int y) {
        MouseEvent event = new MouseEvent(target, eventId,
                System.currentTimeMillis(), 0, x, y, 0, false);
        for (java.awt.event.MouseListener listener : target.getMouseListeners()) {
            if (eventId == MouseEvent.MOUSE_ENTERED) {
                listener.mouseEntered(event);
            } else if (eventId == MouseEvent.MOUSE_EXITED) {
                listener.mouseExited(event);
            }
        }
    }
}
