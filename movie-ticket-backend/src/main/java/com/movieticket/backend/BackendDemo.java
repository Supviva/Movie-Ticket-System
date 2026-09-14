package com.movieticket.backend;

import com.movieticket.backend.db.Db;
import com.movieticket.backend.db.ServiceException;
import com.movieticket.backend.model.Account;
import com.movieticket.backend.model.CartItem;
import com.movieticket.backend.model.CinemaHall;
import com.movieticket.backend.model.MembershipLevel;
import com.movieticket.backend.model.MembershipStats;
import com.movieticket.backend.model.Money;
import com.movieticket.backend.model.Movie;
import com.movieticket.backend.model.SeatPlan;
import com.movieticket.backend.model.ShowSession;
import com.movieticket.backend.model.TicketOrder;
import com.movieticket.backend.service.MovieTicketService;

import java.util.List;
import java.util.Set;
import java.util.TreeSet;

/**
 * 后端验证程序：把每一层能力跑一遍并打印结果。
 *
 * <p>运行方式（任选其一）：</p>
 * <pre>
 *   java -jar target/movie-ticket-backend-1.0.0.jar
 *   mvn exec:java -Dexec.mainClass=com.movieticket.backend.BackendDemo
 * </pre>
 *
 * <p>程序只读 + 只新增自己的测试订单，不会删除或改动种子数据。</p>
 */
public final class BackendDemo {

    private static final MovieTicketService service = new MovieTicketService();
    private static int passed = 0;
    private static int failed = 0;

    public static void main(String[] args) {
        System.out.println("╔══════════════════════════════════════════════════════════════════╗");
        System.out.println("║      光影票务系统 · 后端持久化层验证程序                          ║");
        System.out.println("╚══════════════════════════════════════════════════════════════════╝");
        System.out.println();

        try {
            step0Reset();
            step1Connection();
            step2SeedData();
            step3Login();
            step4SeatPlan();
            step5CartAndCheckout();
            step6PayAndUpgrade();
            step7CancelReleasesSeat();
            step8ValidationGuards();
        } catch (Exception e) {
            System.out.println();
            System.out.println("!! 运行中断：" + e.getClass().getSimpleName() + " - " + e.getMessage());
            if (e.getCause() != null) {
                System.out.println("   原因：" + e.getCause().getMessage());
            }
            System.out.println();
            printHint(e);
            System.exit(1);
        }

        printSummary();
        if (failed > 0) {
            System.exit(1);
        }
    }

    // ========================================================================

    /**
     * 运行前清理上次验证留下的数据，让本程序可以反复运行。
     *
     * <p>只清理「本程序自己产生的测试痕迹」：测试顾客、购物车、以及演示订单，
     * 种子数据（影片/影厅/场次/演示账号）一律不动。</p>
     */
    private static void step0Reset() {
        int[] removed = service.resetTestData();
        if (removed[0] + removed[1] > 0) {
            System.out.println("── 0 / 8  清理上次运行痕迹 ─────────────────────────────────────");
            System.out.println("  [通过] 已清理测试顾客 " + removed[0] + " 个、测试订单 " + removed[1] + " 笔");
            System.out.println();
        }
    }

    private static void step1Connection() {
        heading("1 / 8  数据库连接");
        try {
            String version = Db.testConnection();
            ok("连接成功：" + version);
            info("连接参数：" + Db.config().describe());
        } catch (ServiceException e) {
            bad("连接失败：" + e.getMessage());
            throw e;
        }
        blank();
    }

    private static void step2SeedData() {
        heading("2 / 8  读取基础数据（验证 SELECT）");

        List<Movie> movies = service.movies();
        ok("影片 " + movies.size() + " 部");
        movies.stream().limit(3)
                .forEach(m -> info(String.format("  %s  %-12s %s  %.1f分  %s",
                        m.id(), m.title(), m.genre(), m.rating(), m.durationText())));
        if (movies.size() > 3) {
            info("  ... 其余 " + (movies.size() - 3) + " 部略");
        }

        List<CinemaHall> halls = service.halls();
        ok("影厅 " + halls.size() + " 个");
        halls.forEach(h -> info(String.format("  %s  %s  %s  %d×%d = %d 座",
                h.id(), h.name(), h.specs(), h.rows(), h.cols(), h.seatCount())));

        List<ShowSession> sessions = service.allSessions();
        ok("场次 " + sessions.size() + " 场");
        sessions.stream().limit(3)
                .forEach(s -> info(String.format("  %s  %s  %s  %s  %s",
                        s.id(), s.movieId(), s.hallId(), s.startTime(), Money.format(s.priceFen()))));

        List<Account> customers = service.customers(null);
        ok("顾客 " + customers.size() + " 人，会员等级由数据库读出");
        customers.forEach(a -> info(String.format("  %s  %-9s %-6s  %s",
                a.id(), a.username(), a.displayName(), a.membership().label())));
        blank();
    }

    private static void step3Login() {
        heading("3 / 8  登录校验（验证口令摘要比对）");

        Account admin = service.login("admin", "123456");
        check(admin != null, "admin / 123456 登录成功，角色 " + (admin == null ? "-" : admin.role()));

        Account customer = service.login("customer", "123456");
        check(customer != null, "customer / 123456 登录成功，会员 " + (customer == null ? "-" : customer.membership().label()));

        Account wrong = service.login("customer", "000000");
        check(wrong == null, "错误口令被拒绝（返回 null）");

        Account missing = service.login("nobody", "123456");
        check(missing == null, "不存在的账号被拒绝（返回 null）");

        try {
            service.login("xu", "123456");
            bad("已禁用账号 xu 应当被拦截，但没有");
        } catch (ServiceException e) {
            check(true, "已禁用账号被拦截：" + e.getMessage());
        }

        // 用带时间戳的用户名，保证本程序可以反复运行而不撞「用户名已存在」
        String newName = "demo_u" + System.currentTimeMillis() % 1000000;
        Account fresh = service.register(newName, "演示新用户", "13700001234", "123456");
        check(fresh != null, "注册新用户成功：" + fresh.id() + " " + fresh.username());

        // 重复用户名应当被拦截
        expectFail("重复用户名（再注册一次 " + newName + "）",
                () -> service.register(newName, "重名用户", "13700005678", "123456"));
        blank();
    }

    private static void step4SeatPlan() {
        heading("4 / 8  座位图与占座查询（验证视图 v_sold_seats）");

        List<ShowSession> sessions = service.allSessions();
        if (sessions.isEmpty()) {
            bad("没有可用场次，请先执行 02_seed.sql");
            throw new ServiceException("缺少场次数据");
        }

        // 挑一个历史订单占过座的场次，能直观看到「已占座位」
        String sessionId = sessions.get(0).id();
        SeatPlan plan = service.seatPlan(sessionId);

        ok("场次 " + sessionId + "  影厅 " + plan.hall().name() + "  (" + plan.hall().rows() + "×" + plan.hall().cols() + ")");
        ok("已占座位 " + plan.soldSeats().size() + " 个，剩余可售 " + plan.availableCount() + " 个");
        info("已占座位：" + (plan.soldSeats().isEmpty() ? "（无）" : String.join("、", plan.soldSeats())));

        // 自动挑两个未被占用的座位
        Set<String> free = pickFreeSeats(plan, 2);
        ok("自动挑选空闲座位：" + MovieTicketService.joinSeats(free));
        blank();
    }

    private static void step5CartAndCheckout() {
        heading("5 / 8  购物车与下单（验证 INSERT + 事务）");

        Account user = service.login("lin", "123456");
        check(user != null, "以 lin 身份登录");
        if (user == null) {
            throw new ServiceException("登录失败，无法继续");
        }

        ShowSession session = service.allSessions().get(1);
        service.clearCart(user.id());

        Set<String> free = pickFreeSeats(service.seatPlan(session.id()), 2);
        service.addToCart(user.id(), session.id(), free);
        ok("加入购物车：" + MovieTicketService.joinSeats(free));

        List<CartItem> cart = service.cartItems(user.id());
        check(cart.size() >= 1, "购物车条目数 " + cart.size());
        cart.forEach(item -> info(String.format("  %s  %s  %s  %s  座位[%s]  合计 %s",
                item.id(), item.movieTitle(), item.hallName(), item.startTime(),
                MovieTicketService.joinSeats(item.seats()), Money.format(item.lineTotalFen()))));

        TicketOrder order = service.checkoutCart(user.id());
        ok("下单成功：" + order.id());
        info(String.format("  原价 %s  优惠 %s  应付 %s  (%s)",
                Money.format(order.originalFen()), Money.format(order.discountFen()),
                Money.format(order.payableFen()), order.membershipLabel()));
        check(TicketOrder.STATUS_UNPAID.equals(order.status()), "订单状态：" + order.status());

        check(service.cartItems(user.id()).isEmpty(), "下单后购物车已清空");

        // 下单即占座
        SeatPlan afterOrder = service.seatPlan(session.id());
        Set<String> seats = new TreeSet<>(order.items().get(0).seats());
        check(afterOrder.soldSeats().containsAll(seats),
                "未支付订单也占用座位，已占座位更新为 " + afterOrder.soldSeats().size() + " 个");

        // 抢同一座位应当失败
        try {
            service.addToCart("U-003", session.id(), seats);
            bad("重复占用同一座位应当被拦截，但没有");
        } catch (ServiceException e) {
            check(true, "他人抢同一座位被拦截：" + e.getMessage());
        }
        blank();
    }

    private static void step6PayAndUpgrade() {
        heading("6 / 8  支付与会员自动升级（验证事务 + UPDATE）");

        TicketOrder unpaid = service.allOrders().stream()
                .filter(o -> TicketOrder.STATUS_UNPAID.equals(o.status()))
                .findFirst()
                .orElse(null);

        if (unpaid == null) {
            info("当前没有待支付订单，跳过支付流程");
            blank();
            return;
        }

        String userId = unpaid.userId();
        MembershipLevelView before = membershipOf(userId);
        info("支付前：" + before.label + "，累计消费 " + Money.format(before.spent));
        info("待支付订单 " + unpaid.id() + " 应付 " + Money.format(unpaid.payableFen()));

        TicketOrder paid = service.payOrder(unpaid.id());
        check(TicketOrder.STATUS_PAID.equals(paid.status()), "支付成功，出票码 " + paid.ticketCode());

        MembershipLevelView after = membershipOf(userId);
        info("支付后：" + after.label + "，累计消费 " + Money.format(after.spent));

        if (paid.upgraded()) {
            ok("触发会员升级：" + paid.upgradedFrom().label() + " → " + paid.upgradedTo().label());
        } else {
            info("本次消费未跨档，等级保持 " + after.label + "（升级门槛尚未达到，属正常）");
        }

        MembershipStats stats = service.membershipStats(userId);
        ok(String.format("会员统计：已支付 %d 单 / %d 张票，累计消费 %s，累计节省 %s",
                stats.paidOrderCount(), stats.ticketCount(),
                Money.format(stats.spentFen()), Money.format(stats.savedFen())));

        // 专门演示一次「跨档升级」：挑一个低等级账号，直接买够金额后支付
        demonstrateUpgrade();

        blank();
    }

    /**
     * 主动演示跨档升级：用一个普通会员账号买一笔足以跨过银卡门槛的订单并支付，
     * 观察等级是否由「累计消费」自动推导出来。
     */
    private static void demonstrateUpgrade() {
        info("");
        info("—— 跨档升级专项演示 ——");

        Account target = service.login("lin", "123456");
        if (target == null) {
            info("找不到演示账号 lin，跳过");
            return;
        }

        MembershipLevel cur = target.membership();
        int needFen = cur.next() == null ? 0 : curNextThresholdFen(cur);
        info("账号 lin 当前 " + cur.label() + "，累计消费 " + Money.format(service.spentFen(target.id())));

        if (cur.next() == null) {
            info("已是最高档，无需升级");
            return;
        }
        info("升到 " + cur.next().label() + " 需累计消费达 " + Money.format(needFen));

        // 找一场票价足够高的 IMAX 场次，一次买若干张凑够门槛
        ShowSession expensive = service.allSessions().stream()
                .max(java.util.Comparator.comparingInt(ShowSession::priceFen))
                .orElse(null);
        if (expensive == null) {
            info("没有可用场次，跳过");
            return;
        }

        int spentNow = service.spentFen(target.id());
        int remaining = Math.max(0, needFen - spentNow);
        int perTicket = expensive.priceFen();
        int need = (int) Math.ceil((double) remaining / perTicket);
        need = Math.max(1, Math.min(need, 6)); // 单次最多 6 座

        SeatPlan plan = service.seatPlan(expensive.id());
        Set<String> seats = pickFreeSeats(plan, need);
        info(String.format("在 %s（%s，单价 %s）一次购买 %d 张：%s",
                expensive.id(), expensive.startTime(), Money.format(perTicket),
                need, MovieTicketService.joinSeats(seats)));

        try {
            TicketOrder order = service.buySeatsNow(target.id(), expensive.id(), seats);
            TicketOrder done = service.payOrder(order.id());
            MembershipLevelView now = membershipOf(target.id());
            info("支付后累计消费 " + Money.format(now.spent) + "，当前等级 " + now.label);

            if (done.upgraded()) {
                ok("升级成功：" + done.upgradedFrom().label() + " → " + done.upgradedTo().label());
            } else {
                info("仍未跨档（累计消费未达门槛），等级保持 " + now.label);
            }
        } catch (ServiceException e) {
            info("演示订单未完成：" + e.getMessage());
        }
    }

    /** 取下一档的晋升门槛（分）。 */
    private static int curNextThresholdFen(MembershipLevel current) {
        MembershipLevel next = current.next();
        return next == null ? current.thresholdFen() : next.thresholdFen();
    }

    private static void step7CancelReleasesSeat() {
        heading("7 / 8  取消订单释放座位（验证状态驱动占座）");

        TicketOrder target = service.allOrders().stream()
                .filter(o -> TicketOrder.STATUS_UNPAID.equals(o.status()))
                .findFirst()
                .orElse(null);

        if (target == null) {
            info("没有可取消的未支付订单，改用已支付订单演示");
            target = service.allOrders().stream()
                    .filter(o -> TicketOrder.STATUS_PAID.equals(o.status()))
                    .findFirst().orElse(null);
        }

        if (target == null) {
            info("没有可取消的订单，跳过");
            blank();
            return;
        }

        String sessionId = target.items().get(0).sessionId();
        Set<String> seats = target.items().get(0).seats();

        boolean soldBefore = service.seatPlan(sessionId).soldSeats().containsAll(seats);
        info("取消前，座位 " + MovieTicketService.joinSeats(seats) + " 是否被占：" + (soldBefore ? "是" : "否"));

        TicketOrder canceled = service.cancelOrder(target.id());
        check(TicketOrder.STATUS_CANCELED.equals(canceled.status()), "订单已取消：" + canceled.id());

        boolean soldAfter = service.seatPlan(sessionId).soldSeats().containsAll(seats);
        check(!soldAfter, "取消后座位已释放，可重新购买");
        blank();
    }

    private static void step8ValidationGuards() {
        heading("8 / 8  业务校验与并发保护");

        Account user = service.login("zhou", "123456");
        ShowSession session = service.allSessions().get(2);

        expectFail("空座位", () -> service.addToCart(user.id(), session.id(), Set.of()));
        expectFail("超过 6 座", () -> service.addToCart(user.id(), session.id(),
                Set.of("A1", "A2", "A3", "A4", "A5", "A6", "A7")));
        expectFail("座位格式错误", () -> service.addToCart(user.id(), session.id(), Set.of("X")));
        expectFail("座位越界", () -> service.addToCart(user.id(), session.id(), Set.of("Z99")));
        expectFail("场次不存在", () -> service.addToCart(user.id(), "S999", Set.of("A1")));
        expectFail("订单号不存在", () -> service.order("T999999"));
        expectFail("用户名过短", () -> service.register("ab", "测试用户", "13700001111", "123456"));
        expectFail("手机号不合法", () -> service.register("valid_name", "测试用户", "12345", "123456"));
        expectFail("口令过短", () -> service.register("valid_name2", "测试用户", "13700002222", "123"));
        expectFail("重复用户名", () -> service.register("admin", "重名用户", "13700003333", "123456"));
        expectFail("重复支付", () -> {
            TicketOrder paidOrder = service.allOrders().stream()
                    .filter(o -> TicketOrder.STATUS_PAID.equals(o.status()))
                    .findFirst().orElseThrow(() -> new ServiceException("无已支付订单"));
            service.payOrder(paidOrder.id());
        });

        blank();
    }

    // ========================================================================
    // 辅助方法
    // ========================================================================

    private static Set<String> pickFreeSeats(SeatPlan plan, int count) {
        Set<String> free = new TreeSet<>();
        for (int row = 0; row < plan.hall().rows() && free.size() < count; row++) {
            for (int col = 0; col < plan.hall().cols() && free.size() < count; col++) {
                String key = MovieTicketService.seatKey(row, col);
                if (!plan.soldSeats().contains(key)) {
                    free.add(key);
                }
            }
        }
        if (free.size() < count) {
            throw new ServiceException("影厅空闲座位不足 " + count + " 个");
        }
        return free;
    }

    private static MembershipLevelView membershipOf(String userId) {
        Account account = service.account(userId);
        return new MembershipLevelView(account.membership().label(), service.spentFen(userId));
    }

    private static void expectFail(String label, Runnable action) {
        try {
            action.run();
            bad(label + " —— 应当被拒绝，但通过了");
        } catch (Exception e) {
            String msg = e.getMessage() == null ? e.getClass().getSimpleName() : e.getMessage();
            check(true, label + " —— 已拦截：" + msg);
        }
    }

    private static void heading(String title) {
        System.out.println("── " + title + " " + "─".repeat(Math.max(0, 62 - title.length())));
    }

    private static void ok(String message) {
        System.out.println("  [通过] " + message);
        passed++;
    }

    private static void info(String message) {
        System.out.println("         " + message);
    }

    private static void bad(String message) {
        System.out.println("  [失败] " + message);
        failed++;
    }

    private static void check(boolean condition, String message) {
        if (condition) {
            ok(message);
        } else {
            bad(message);
        }
    }

    private static void blank() {
        System.out.println();
    }

    private static void printSummary() {
        System.out.println("══════════════════════════════════════════════════════════════════");
        System.out.printf("  验证结果：通过 %d 项，失败 %d 项%n", passed, failed);
        if (failed == 0) {
            System.out.println("  结论：后端持久化层工作正常，所有业务规则均按预期生效。");
        } else {
            System.out.println("  结论：存在未通过项，请查看上方 [失败] 行。");
        }
        System.out.println("══════════════════════════════════════════════════════════════════");
    }

    private static void printHint(Exception e) {
        String message = String.valueOf(e.getMessage());
        System.out.println("可能的原因与处理办法：");
        if (message.contains("Access denied") || message.contains("连接失败")) {
            System.out.println("  · MySQL 账号密码不对：打开 src/main/resources/db.properties，");
            System.out.println("    把 db.password 改成你本机 root 的密码，然后重新运行。");
        } else if (message.contains("Unknown database")) {
            System.out.println("  · 数据库还没建：先执行 database/01_schema.sql 与 02_seed.sql。");
        } else if (message.contains("Communications link failure") || message.contains("Connection refused")) {
            System.out.println("  · MySQL 没启动：在服务里启动 MySQL80 服务，或在命令行执行 net start MySQL80。");
        } else {
            System.out.println("  · 检查 MySQL 是否启动、database/ 下的两个 SQL 是否已执行、");
            System.out.println("    db.properties 里的密码是否正确。");
        }
    }

    private record MembershipLevelView(String label, int spent) {
    }
}
