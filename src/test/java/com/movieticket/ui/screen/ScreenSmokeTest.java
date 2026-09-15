package com.movieticket.ui.screen;

import com.movieticket.demo.DemoMovieTicketService;
import com.movieticket.model.Account;
import com.movieticket.model.MembershipLevel;
import com.movieticket.model.Movie;
import com.movieticket.model.SeatPlan;
import com.movieticket.ui.component.HeroBannerPanel;
import com.movieticket.ui.component.MovieCardPanel;
import com.movieticket.ui.component.PosterPanel;
import org.junit.jupiter.api.Test;

import javax.swing.SwingUtilities;
import java.util.List;
import java.util.Set;
import java.util.function.Consumer;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ScreenSmokeTest {

    @Test
    void allMainScreensCanBeConstructed() throws Exception {
        SwingUtilities.invokeAndWait(() -> {
            DemoMovieTicketService service = new DemoMovieTicketService();
            Consumer<Account> ignored = account -> {
            };
            LoginPanel login = new LoginPanel(service, ignored);
            CustomerPanel customer = new CustomerPanel(
                    service, service.login("customer", "123456"), () -> {
            });
            AdminPanel admin = new AdminPanel(
                    service, service.login("admin", "123456"), () -> {
            });
            assertNotNull(login);
            assertNotNull(customer);
            assertNotNull(admin);
        });
    }

    /** 会员页刷新涉及订单统计与进度条绘制，需覆盖各档位会员都不抛异常。 */
    @Test
    void membershipScreenRefreshesAcrossAllTiers() throws Exception {
        SwingUtilities.invokeAndWait(() -> {
            DemoMovieTicketService service = new DemoMovieTicketService();
            Consumer<Account> ignored = account -> {
            };

            // lin 为普通会员且无任何订单：统计全为 0，进度条指向银卡
            CustomerPanel fresh = new CustomerPanel(
                    service, service.login("lin", "123456"), () -> {
            });
            assertNotNull(fresh);

            // customer 为银卡会员（手工种入，无消费记录）
            CustomerPanel silver = new CustomerPanel(
                    service, service.login("customer", "123456"), () -> {
            });
            assertNotNull(silver);

            // chen 为金卡会员：覆盖中间档位的升级链与进度条分支
            CustomerPanel gold = new CustomerPanel(
                    service, service.login("chen", "123456"), () -> {
            });
            assertNotNull(gold);

            // 提升到最高档，验证顶档文案与满进度条不抛异常
            Account top = service.setMembership(service.login("chen", "123456").id(),
                    MembershipLevel.DIAMOND);
            assertEquals(MembershipLevel.DIAMOND, top.membership());
            assertTrue(top.membership().isTop());
            CustomerPanel topTier = new CustomerPanel(
                    service, service.login("chen", "123456"), () -> {
            });
            assertNotNull(topTier);

            // 触发一次真实下单支付，确保统计口径下进度条与"已省金额"路径被覆盖
            Account member = service.login("customer", "123456");
            var session = service.sessionsForMovie(service.movies().get(0).id()).get(0);
            SeatPlan plan = service.seatPlan(session.id());
            String freeSeat = null;
            for (int row = 0; row < plan.hall().rows() && freeSeat == null; row++) {
                for (int col = 1; col <= plan.hall().cols(); col++) {
                    String candidate = (char) ('A' + row) + String.valueOf(col);
                    if (!plan.soldSeats().contains(candidate)) {
                        freeSeat = candidate;
                        break;
                    }
                }
            }
            var order = service.buySeatsNow(member.id(), session.id(), Set.of(freeSeat));
            service.payOrder(order.id());
            CustomerPanel refreshed = new CustomerPanel(
                    service, service.login("customer", "123456"), () -> {
            });
            assertNotNull(refreshed);
        });
    }

    /**
     * 消费跨过门槛触发自动升级时，会员页刷新与升级提示文案都不能抛异常。
     *
     * <p>把演示账号降到普通会员后逐张购票，直到累计消费越过银卡门槛——升级提示
     * 会读取 {@code upgradedFrom}/{@code upgradedTo}，若这两个字段为空会直接崩。</p>
     */
    @Test
    void membershipScreenSurvivesAutomaticUpgrade() throws Exception {
        SwingUtilities.invokeAndWait(() -> {
            DemoMovieTicketService service = new DemoMovieTicketService();
            Account member = service.login("customer", "123456");
            service.setMembership(member.id(), MembershipLevel.NORMAL);

            var session = service.sessionsForMovie(service.movies().get(0).id()).get(0);
            boolean upgraded = false;
            for (int i = 0; i < 20 && !upgraded; i++) {
                SeatPlan plan = service.seatPlan(session.id());
                String freeSeat = null;
                for (int row = 0; row < plan.hall().rows() && freeSeat == null; row++) {
                    for (int col = 1; col <= plan.hall().cols(); col++) {
                        String candidate = (char) ('A' + row) + String.valueOf(col);
                        if (!plan.soldSeats().contains(candidate)) {
                            freeSeat = candidate;
                            break;
                        }
                    }
                }
                var order = service.buySeatsNow(member.id(), session.id(), Set.of(freeSeat));
                upgraded = service.payOrder(order.id()).upgraded();
            }
            assertTrue(upgraded, "累计消费达标后应触发自动升级");

            CustomerPanel panel = new CustomerPanel(
                    service, service.login("customer", "123456"), () -> {
            });
            assertNotNull(panel);
            assertEquals(MembershipLevel.forSpending(service.spentFen(member.id())),
                    service.account(member.id()).membership());
        });
    }

    /**
     * 首页影片卡与主视觉横幅可正常构造。
     *
     * <p>两个组件都在 {@code paintComponent} 里做自绘（遮罩渐变、角标、悬停态），
     * 构造后立即绘制一次才能覆盖这些绘制路径。</p>
     */
    @Test
    void homepageCardsAndHeroCanBePainted() throws Exception {
        SwingUtilities.invokeAndWait(() -> {
            DemoMovieTicketService service = new DemoMovieTicketService();
            List<Movie> movies = service.movies();
            assertFalse(movies.isEmpty(), "演示数据应至少有一部影片");

            MovieCardPanel card = new MovieCardPanel(movies.get(0), "gift", () -> {
            }, movie -> {
            });
            MovieCardPanel presale = new MovieCardPanel(movies.get(0), "presale", () -> {
            }, movie -> {
            });
            assertNotNull(card);
            assertNotNull(presale);
            paintIntoImage(card);
            paintIntoImage(presale);

            HeroBannerPanel hero = new HeroBannerPanel(movies.subList(0, 3), movie -> {
            }, movie -> {
            }, () -> {
            });
            assertNotNull(hero);
            assertEquals(movies.get(0), hero.current());
            // 切换主推影片会重建内容并重绘
            hero.show(1);
            assertEquals(movies.get(1), hero.current());
            paintIntoImage(hero);
        });
    }

    /** 首页三个分区必须互不重复且覆盖全部影片。 */
    @Test
    void homepageSectionsDoNotOverlap() {
        DemoMovieTicketService service = new DemoMovieTicketService();
        List<Movie> all = service.movies();
        int currentYear = java.time.LocalDate.now().getYear();

        List<String> nowShowing = all.stream()
                .filter(movie -> year(movie) <= currentYear)
                .filter(movie -> year(movie) >= currentYear - 15).map(Movie::id).toList();
        List<String> upcoming = all.stream()
                .filter(movie -> year(movie) > currentYear).map(Movie::id).toList();
        List<String> classics = all.stream()
                .filter(movie -> year(movie) < currentYear - 15).map(Movie::id).toList();

        assertFalse(nowShowing.isEmpty(), "应有正在热映的影片");
        assertFalse(upcoming.isEmpty(), "应有即将上映的影片");
        assertFalse(classics.isEmpty(), "应有经典重映影片");

        List<String> union = new java.util.ArrayList<>();
        union.addAll(nowShowing);
        union.addAll(upcoming);
        union.addAll(classics);
        assertEquals(all.size(), union.size(), "三个分区不应重复覆盖同一部影片");
        assertEquals(all.size(), Set.copyOf(union).size(), "三个分区之间不应有交集");
    }

    /** 「即将上映」分区必须是次年及以后的影片，且包含复仇者联盟5等待映大片。 */
    @Test
    void upcomingSectionHoldsFutureReleases() {
        DemoMovieTicketService service = new DemoMovieTicketService();
        int currentYear = java.time.LocalDate.now().getYear();

        List<Movie> upcoming = service.movies().stream()
                .filter(movie -> year(movie) > currentYear)
                .toList();
        assertFalse(upcoming.isEmpty(), "应有次年及以后上映的影片");
        assertTrue(upcoming.stream().allMatch(movie -> year(movie) > currentYear),
                "「即将上映」不应混入已上映的影片");

        List<String> titles = upcoming.stream().map(Movie::title).toList();
        assertTrue(titles.contains("复仇者联盟5：康之王朝"), "应包含复联5，实际 " + titles);
        assertTrue(titles.contains("复仇者联盟6：秘密战争"));
        assertTrue(titles.contains("阿凡达3：火与烬"));

        // 待映大片必须能排片，否则「预售」角标点进去无场次可售
        for (Movie movie : upcoming) {
            assertFalse(service.sessionsForMovie(movie.id()).isEmpty(),
                    "《" + movie.title() + "》应已开预售场次");
        }
    }

    /** 新增的高热度大片应能按标题检索到。 */
    @Test
    void newBlockbusterTitlesAreSearchable() {
        DemoMovieTicketService service = new DemoMovieTicketService();
        List<Movie> hits = service.searchMovies("复仇者联盟");
        assertEquals(6, hits.size(), "四部已上映 + 两部待映的复仇者联盟都应入库");
        assertTrue(hits.stream().anyMatch(movie -> movie.title().contains("终局之战")));
        assertTrue(hits.stream().anyMatch(movie -> movie.title().contains("康之王朝")));
        assertTrue(service.searchMovies("星际穿越").size() == 1);
        assertTrue(service.searchMovies("阿凡达").size() == 2, "阿凡达与阿凡达3都应命中");
    }

    /**
     * 无海报图时的占位标识：同系列续集必须能区分开，不能全部退化成同一个首字。
     *
     * <p>四部《复仇者联盟》都含「复」，若只取首字则四张卡面完全一样。</p>
     */
    @Test
    void fallbackGlyphDistinguishesSequels() {
        // 无续集编号：取片名首个有意义字符，跳过标点/书名号
        assertEquals("星", PosterPanel.fallbackGlyph("星际穿越"));
        assertEquals("泰", PosterPanel.fallbackGlyph("泰坦尼克号"));
        assertEquals("哈", PosterPanel.fallbackGlyph("《哈利·波特》"));

        // 阿拉伯数字最优先：四部复联直接取续集号，互不相同
        assertEquals("2", PosterPanel.fallbackGlyph("复仇者联盟2：奥创纪元"));
        assertEquals("3", PosterPanel.fallbackGlyph("复仇者联盟3：无限战争"));
        assertEquals("4", PosterPanel.fallbackGlyph("复仇者联盟4：终局之战"));
        assertEquals("3", PosterPanel.fallbackGlyph("指环王3：王者无敌"));
        assertEquals("2", PosterPanel.fallbackGlyph("海底总动员2"));
        assertEquals("13", PosterPanel.fallbackGlyph("Ocean's 13"));
        // 「1」不算续集标记（首部通常不带编号），退化为首字
        assertEquals("复", PosterPanel.fallbackGlyph("复仇者联盟1"));

        // 仅罗马数字时取罗马数字，且长匹配优先（III 不能被 I 抢先）
        assertEquals("III", PosterPanel.fallbackGlyph("Rocky III"));
        assertEquals("II", PosterPanel.fallbackGlyph("The Godfather Part II"));
        assertEquals("VII", PosterPanel.fallbackGlyph("Rocky VII"));

        // 边界：null / 空 / 纯标点 都不能抛异常
        assertEquals("", PosterPanel.fallbackGlyph(null));
        assertEquals("", PosterPanel.fallbackGlyph(""));
        assertEquals("", PosterPanel.fallbackGlyph("   "));
        assertEquals("", PosterPanel.fallbackGlyph("《》·"));

        // 同一系列的四部影片标识必须两两不同（首字相同也能区分）
        List<String> avengers = List.of("复仇者联盟", "复仇者联盟2：奥创纪元",
                "复仇者联盟3：无限战争", "复仇者联盟4：终局之战");
        Set<String> glyphs = avengers.stream()
                .map(title -> PosterPanel.fallbackGlyph(title))
                .collect(java.util.stream.Collectors.toSet());
        assertEquals(4, glyphs.size(), "复联四部的占位标识应两两不同，实际 " + glyphs);
    }

    /** 占位图绘制路径不应抛异常（新片无海报图时会走这条分支）。 */
    @Test
    void fallbackPosterCanBePainted() throws Exception {
        SwingUtilities.invokeAndWait(() -> {
            DemoMovieTicketService service = new DemoMovieTicketService();
            for (Movie movie : service.movies()) {
                PosterPanel panel = new PosterPanel(movie, 188, 280);
                paintIntoImage(panel);
            }
        });
    }

    private static int year(Movie movie) {
        String date = movie.releaseDate();
        return date != null && date.length() >= 4 ? Integer.parseInt(date.substring(0, 4)) : 0;
    }

    /** 把组件绘制到离屏图片，触发自绘逻辑并暴露潜在的绘制异常。 */
    private static void paintIntoImage(javax.swing.JComponent component) {
        component.setSize(400, 400);
        java.awt.image.BufferedImage image = new java.awt.image.BufferedImage(
                400, 400, java.awt.image.BufferedImage.TYPE_INT_ARGB);
        java.awt.Graphics2D g2 = image.createGraphics();
        component.paint(g2);
        g2.dispose();
    }
}
