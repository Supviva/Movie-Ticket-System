package com.movieticket.demo;

import com.movieticket.model.Account;
import com.movieticket.model.CartItem;
import com.movieticket.model.CinemaHall;
import com.movieticket.model.MembershipLevel;
import com.movieticket.model.Money;
import com.movieticket.model.Movie;
import com.movieticket.model.Role;
import com.movieticket.model.SeatPlan;
import com.movieticket.model.ShowSession;
import com.movieticket.model.TicketOrder;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DemoMovieTicketServiceTest {

    private DemoMovieTicketService service;

    @BeforeEach
    void setUp() {
        service = new DemoMovieTicketService();
    }

    @Test
    void seededAccountsCanLogin() {
        Account customer = service.login("customer", "123456");
        assertNotNull(customer);
        assertEquals(Role.CUSTOMER, customer.role());

        Account admin = service.login("admin", "123456");
        assertNotNull(admin);
        assertEquals(Role.ADMIN, admin.role());
    }

    @Test
    void disabledAccountCannotLogin() {
        assertThrows(DemoException.class, () -> service.login("xu", "123456"));
    }

    @Test
    void registrationValidatesAndStartsAsNormalMember() {
        Account registered = service.register("newuser", "新用户", "13866668888", "abc123");
        assertEquals(MembershipLevel.NORMAL, registered.membership());
        assertEquals(Role.CUSTOMER, registered.role());
        assertThrows(DemoException.class,
                () -> service.register("newuser", "重复用户", "13866668889", "abc123"));
    }

    @Test
    void duplicateSeatCannotBeAddedToCartTwice() {
        Account customer = service.login("customer", "123456");
        ShowSession session = service.sessionsForMovie("M001").getFirst();
        Set<String> seats = availableSeats(service, session, 2);
        service.addToCart(customer.id(), session.id(), seats);
        assertThrows(DemoException.class,
                () -> service.addToCart(customer.id(), session.id(), seats));
    }

    @Test
    void cartCanBeRemovedAndCleared() {
        Account customer = service.login("customer", "123456");
        ShowSession session = service.sessionsForMovie("M001").getFirst();
        Set<String> seats = availableSeats(service, session, 2);
        service.addToCart(customer.id(), session.id(), seats);
        CartItem item = service.cartItems(customer.id()).getFirst();
        service.removeCartItem(customer.id(), item.id());
        assertTrue(service.cartItems(customer.id()).isEmpty());

        service.addToCart(customer.id(), session.id(), Set.of("B1"));
        service.clearCart(customer.id());
        assertEquals(0, service.cartCount(customer.id()));
    }

    @Test
    void checkoutProducesPaidOrderWithDiscountAndLocksSeats() {
        Account customer = service.login("customer", "123456");
        ShowSession session = service.sessionsForMovie("M001").getFirst();
        Set<String> seats = availableSeats(service, session, 2);
        service.addToCart(customer.id(), session.id(), seats);
        TicketOrder order = service.checkoutCart(customer.id());
        assertTrue(service.cartItems(customer.id()).isEmpty());
        assertTrue(order.discountFen() > 0);
        assertTrue(order.payableFen() < order.originalFen());
        assertEquals(order.originalFen() - order.discountFen(), order.payableFen());

        assertThrows(DemoException.class,
                () -> service.buySeatsNow(customer.id(), session.id(),
                        Set.of(seats.iterator().next(), firstAvailableSeat(service, session))));

        TicketOrder paid = service.payOrder(order.id());
        assertEquals("PAID", paid.status());
        assertFalse(paid.ticketCode().isBlank());
    }

    @Test
    void cancelOrderReleasesSeats() {
        Account customer = service.login("customer", "123456");
        ShowSession session = service.sessionsForMovie("M001").getFirst();
        Set<String> seats = availableSeats(service, session, 2);
        TicketOrder order = service.buySeatsNow(customer.id(), session.id(), seats);
        TicketOrder canceled = service.cancelOrder(order.id());
        assertEquals("CANCELED", canceled.status());

        String released = seats.iterator().next();
        TicketOrder second = service.buySeatsNow(customer.id(), session.id(), Set.of(released));
        assertNotNull(second);
        assertTrue(second.originalFen() > 0);
    }

    @Test
    void seatOutOfRangeIsRejected() {
        Account customer = service.login("customer", "123456");
        ShowSession session = service.sessionsForMovie("M001").getFirst();
        assertThrows(DemoException.class,
                () -> service.buySeatsNow(customer.id(), session.id(), Set.of("J99")));
    }

    @Test
    void adminCanManageCustomers() {
        Account admin = service.login("admin", "123456");
        Account added = service.addCustomer("demo_admin_user", "临时顾客", "13899990001", "abc123");
        List<Account> matches = service.customers("临时顾客");
        assertTrue(matches.stream().anyMatch(account -> account.id().equals(added.id())));

        Account updated = service.updateCustomerProfile(added.id(), "临时顾客改", "13899990002", false);
        assertFalse(updated.enabled());
        assertThrows(DemoException.class, () -> service.login("demo_admin_user", "abc123"));

        Account gold = service.setMembership(added.id(), MembershipLevel.GOLD);
        assertEquals(MembershipLevel.GOLD, gold.membership());
    }

    @Test
    void movieHallAndSessionCrudKeepsReferentialIntegrity() {
        Movie movie = new Movie("", "测试电影", "剧情", "测试导演", "主演甲",
                100, "2D", "2026-12-01", "用于测试的新影片。", 8.0, 1);
        Movie saved = service.addMovie(movie);
        service.deleteMovie(saved.id());
        assertThrows(DemoException.class, () -> service.movie(saved.id()));

        CinemaHall hall = new CinemaHall("", "测试厅", "标准", 5, 8);
        CinemaHall savedHall = service.addHall(hall);
        service.deleteHall(savedHall.id());
        assertThrows(DemoException.class, () -> service.hall(savedHall.id()));

        Movie linked = service.addMovie(new Movie("", "有场次电影", "动作", "导演", "演员",
                90, "2D", "2026-12-02", "用于测试场次的影片。", 7.0, 2));
        CinemaHall linkedHall = service.addHall(new CinemaHall("", "关联厅", "标准", 4, 6));
        ShowSession session = service.addSession(linked.id(), linkedHall.id(), "2026-12-10 14:00", 3000);
        assertThrows(DemoException.class, () -> service.deleteMovie(linked.id()));
        assertThrows(DemoException.class, () -> service.deleteHall(linkedHall.id()));
        service.deleteSession(session.id());
        service.deleteMovie(linked.id());
        service.deleteHall(linkedHall.id());
    }

    @Test
    void moneyDiscountIsRounded() {
        assertEquals(950, Money.applyDiscount(1000, 950));
        assertEquals(880, Money.applyDiscount(1000, 880));
        assertEquals(1000, Money.applyDiscount(1000, 1000));
    }

    // ---------- 消费驱动的自动升级 ----------

    @Test
    void payingOrderAddsToAccumulatedSpending() {
        Account customer = service.login("customer", "123456");
        assertEquals(0, service.spentFen(customer.id()), "演示账号无种子订单，起始累计消费为 0");

        MemberOrder paid = payOneTicket(customer, "M001");
        int after = service.spentFen(customer.id());
        assertEquals(paid.order().payableFen(), after, "已支付订单金额应计入累计消费");

        // 单张票金额远低于银卡门槛，等级不应凭空跳档
        assertTrue(after < MembershipLevel.SILVER.thresholdFen());
        assertEquals(customer.membership(), service.account(customer.id()).membership(),
                "消费不足以升级时等级保持不变");
    }

    @Test
    void repeatedSpendingReadsAreSideEffectFree() {
        Account customer = service.login("customer", "123456");
        int before = service.spentFen(customer.id());
        payOneTicket(customer, "M001");
        int once = service.spentFen(customer.id());
        assertTrue(once > before);
        // 重复读取不应产生副作用
        assertEquals(once, service.spentFen(customer.id()));
        assertEquals(once, service.spentFen(customer.id()));
    }

    @Test
    void levelMatchesSpendingOnceSpendingCatchesUpWithSeededTier() {
        Account customer = service.login("customer", "123456");
        // 演示账号被种为银卡但消费为 0；攒够银卡门槛后，消费与等级应完全对齐
        MembershipLevel seeded = customer.membership();
        while (service.spentFen(customer.id()) < seeded.thresholdFen()) {
            payOneTicketAnySession(customer);
        }
        MembershipLevel now = service.account(customer.id()).membership();
        assertTrue(now.ordinal() >= seeded.ordinal(), "消费达标后等级不应低于原档");
        assertEquals(MembershipLevel.forSpending(service.spentFen(customer.id())), now,
                "消费追上原档后，等级应等于累计消费推导结果");
    }

    @Test
    void membershipNeverDropsBelowCurrentTier() {
        Account customer = service.login("customer", "123456");
        // 后台把演示账号直接提到铂金，远高于其消费额
        service.setMembership(customer.id(), MembershipLevel.PLATINUM);
        assertEquals(MembershipLevel.PLATINUM, service.account(customer.id()).membership());

        payOneTicket(customer, "M001");
        assertEquals(MembershipLevel.PLATINUM, service.account(customer.id()).membership(),
                "自动升级只升不降，不应因消费不足被降档");
    }

    @Test
    void crossingThresholdUpgradesAndReportsFromTo() {
        Account customer = service.login("customer", "123456");
        // 演示账号初始为银卡但无消费记录；后台直接降到普通，便于从零观察
        service.setMembership(customer.id(), MembershipLevel.NORMAL);
        assertEquals(0, service.spentFen(customer.id()), "新会话无历史消费");

        // 一张票 39~65 元，攒够 200 元银卡门槛必然要买几场
        TicketOrder first = payOneTicketAnySession(customer);
        assertEquals(MembershipLevel.NORMAL, service.account(customer.id()).membership(),
                "首张票消费额不足以达到银卡门槛");

        TicketOrder upgraded = null;
        for (int i = 0; i < 20 && upgraded == null; i++) {
            TicketOrder candidate = payOneTicketAnySession(customer);
            if (candidate.upgraded()) {
                upgraded = candidate;
            }
        }
        assertNotNull(upgraded, "累计消费达到银卡门槛后应自动升级");

        int spent = service.spentFen(customer.id());
        assertEquals(MembershipLevel.NORMAL, upgraded.upgradedFrom(), "升级前应是普通会员");
        assertEquals(MembershipLevel.forSpending(spent), upgraded.upgradedTo(),
                "升级应直接落到累计消费对应的档位");
        assertTrue(upgraded.upgradedTo().ordinal() > MembershipLevel.NORMAL.ordinal());
        assertTrue(first.payableFen() > 0);
    }

    @Test
    void repeatedSpendingAdvancesThroughEveryTier() {
        Account customer = service.login("customer", "123456");
        service.setMembership(customer.id(), MembershipLevel.NORMAL);

        MembershipLevel highest = MembershipLevel.NORMAL;
        // 反复买票消费，等级应随累计金额单调不减，最终到达钻石
        for (int i = 0; i < 500 && highest != MembershipLevel.DIAMOND; i++) {
            payOneTicketAnySession(customer);
            MembershipLevel now = service.account(customer.id()).membership();
            assertTrue(now.ordinal() >= highest.ordinal(), "等级不应回退");
            highest = now;
        }
        assertEquals(MembershipLevel.DIAMOND, highest, "持续消费应能爬到最高档");
        assertTrue(service.spentFen(customer.id()) >= MembershipLevel.DIAMOND.thresholdFen());
    }

    @Test
    void cancelingOrderDoesNotCountTowardMembership() {
        Account customer = service.login("customer", "123456");
        int before = service.spentFen(customer.id());
        ShowSession session = service.sessionsForMovie("M001").getFirst();
        Set<String> seats = availableSeats(service, session, 1);
        TicketOrder order = service.buySeatsNow(customer.id(), session.id(), seats);
        service.cancelOrder(order.id());
        assertEquals(before, service.spentFen(customer.id()), "取消的订单不应计入累计消费");
    }

    @Test
    void unpaidOrderDoesNotTriggerUpgrade() {
        Account fresh = service.register("upgrade_probe", "升级探针", "13877778888", "abc123");
        assertEquals(MembershipLevel.NORMAL, fresh.membership());
        ShowSession session = service.sessionsForMovie("M001").getFirst();
        Set<String> seats = availableSeats(service, session, 1);
        TicketOrder order = service.buySeatsNow(fresh.id(), session.id(), seats);
        assertEquals("UNPAID", order.status());
        assertFalse(order.upgraded());
        assertEquals(MembershipLevel.NORMAL, service.account(fresh.id()).membership());

        TicketOrder paid = service.payOrder(order.id());
        assertEquals(service.spentFen(fresh.id()), paid.payableFen());
    }

    /** 买一张票并支付，返回支付后的订单与用户。 */
    private MemberOrder payOneTicket(Account account, String movieId) {
        ShowSession session = service.sessionsForMovie(movieId).getFirst();
        Set<String> seats = availableSeats(service, session, 1);
        TicketOrder order = service.buySeatsNow(account.id(), session.id(), seats);
        return new MemberOrder(account, service.payOrder(order.id()));
    }

    /**
     * 买一张票并支付，座位在全部场次间轮转。
     *
     * <p>批量刷消费的测试会消耗大量座位，固定某个场次会很快把影厅买空，
     * 因此这里按场次顺序取下一个还有余位的场次。</p>
     */
    private TicketOrder payOneTicketAnySession(Account account) {
        for (ShowSession session : allSessions()) {
            Set<String> seats = availableSeats(service, session, 1);
            if (seats.isEmpty()) {
                continue;
            }
            TicketOrder order = service.buySeatsNow(account.id(), session.id(), seats);
            return service.payOrder(order.id());
        }
        throw new IllegalStateException("所有场次座位已售罄，无法继续测试");
    }

    /** 遍历全部影片的全部场次，顺序稳定。 */
    private List<ShowSession> allSessions() {
        List<ShowSession> sessions = new java.util.ArrayList<>();
        for (Movie movie : service.movies()) {
            sessions.addAll(service.sessionsForMovie(movie.id()));
        }
        return sessions;
    }

    private record MemberOrder(Account account, TicketOrder order) {
    }

    private static Set<String> availableSeats(DemoMovieTicketService service, ShowSession session, int count) {
        SeatPlan plan = service.seatPlan(session.id());
        java.util.TreeSet<String> seats = new java.util.TreeSet<>();
        for (int row = 0; row < plan.hall().rows() && seats.size() < count; row++) {
            for (int col = 0; col < plan.hall().cols() && seats.size() < count; col++) {
                String key = String.valueOf((char) ('A' + row)) + (col + 1);
                if (!plan.soldSeats().contains(key)) {
                    seats.add(key);
                }
            }
        }
        return seats;
    }

    private static String firstAvailableSeat(DemoMovieTicketService service, ShowSession session) {
        SeatPlan plan = service.seatPlan(session.id());
        for (int row = 0; row < plan.hall().rows(); row++) {
            for (int col = 0; col < plan.hall().cols(); col++) {
                String key = String.valueOf((char) ('A' + row)) + (col + 1);
                if (!plan.soldSeats().contains(key)) {
                    return key;
                }
            }
        }
        throw new IllegalStateException("影厅没有可用座位");
    }
}
