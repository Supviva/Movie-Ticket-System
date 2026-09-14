package com.movieticket.backend.service;

import com.movieticket.backend.dao.AccountDao;
import com.movieticket.backend.dao.CartDao;
import com.movieticket.backend.dao.CinemaHallDao;
import com.movieticket.backend.dao.MovieDao;
import com.movieticket.backend.dao.OrderDao;
import com.movieticket.backend.dao.SeatDao;
import com.movieticket.backend.dao.ShowSessionDao;
import com.movieticket.backend.db.Db;
import com.movieticket.backend.db.ServiceException;
import com.movieticket.backend.model.Account;
import com.movieticket.backend.model.CartItem;
import com.movieticket.backend.model.CinemaHall;
import com.movieticket.backend.model.MembershipLevel;
import com.movieticket.backend.model.MembershipStats;
import com.movieticket.backend.model.Money;
import com.movieticket.backend.model.Movie;
import com.movieticket.backend.model.Role;
import com.movieticket.backend.model.SeatPlan;
import com.movieticket.backend.model.ShowSession;
import com.movieticket.backend.model.TicketOrder;
import com.movieticket.backend.model.TicketOrderItem;

import java.sql.Connection;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.TreeSet;
import java.util.concurrent.ThreadLocalRandom;
import java.util.regex.Pattern;

/**
 * 票务业务服务（数据库版）。
 *
 * <p>这是原内存版 {@code DemoMovieTicketService} 的持久化改写：对外暴露的方法签名与业务规则
 * 完全一致，但所有数据都落到 MySQL。前端只需把这个类替换掉原先的 Service 即可。</p>
 *
 * <p><b>事务边界</b>：写操作（注册、下单、支付、取消、增删改）都包在
 * {@link Db#inTransaction} 中，保证「扣座位 + 建订单 + 升级会员」这些步骤要么全成功、
 * 要么全回滚，不会出现座位被占却没有订单的脏数据。</p>
 *
 * <p><b>并发安全</b>：选座下单前会带 {@code FOR UPDATE} 锁定相关座位行，
 * 两个用户同时抢同一座位时只有一个能成功，从数据库层面杜绝超卖。</p>
 */
public class MovieTicketService {

    private static final DateTimeFormatter DATE_TIME =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm", Locale.ROOT);
    private static final DateTimeFormatter DATE_TIME_FULL =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss", Locale.ROOT);
    private static final Pattern SEAT_PATTERN = Pattern.compile("^([A-Z])(\\d+)$");
    private static final int MAX_SEATS_PER_ORDER = 6;

    private final AccountDao accountDao = new AccountDao();
    private final MovieDao movieDao = new MovieDao();
    private final CinemaHallDao hallDao = new CinemaHallDao();
    private final ShowSessionDao sessionDao = new ShowSessionDao();
    private final SeatDao seatDao = new SeatDao();
    private final CartDao cartDao = new CartDao();
    private final OrderDao orderDao = new OrderDao();

    // ========================================================================
    // 账号与登录
    // ========================================================================

    /** 登录。用户名或口令不对返回 null；账号被禁用抛业务异常。 */
    public Account login(String username, String password) {
        return Db.inTransaction(conn -> {
            Account account = accountDao.findByUsername(conn, username);
            if (account == null) {
                return null;
            }
            String storedHash = accountDao.findPasswordHash(conn, account.id());
            if (!PasswordHasher.matches(password, storedHash)) {
                return null;
            }
            if (!account.enabled()) {
                throw new ServiceException("该账号已被禁用，请联系管理员");
            }
            return account;
        });
    }

    /** 顾客自助注册。 */
    public Account register(String username, String displayName, String phone, String password) {
        validateUsername(username);
        validatePassword(password);
        validatePhone(phone);
        validateDisplayName(displayName);

        return Db.inTransaction(conn -> {
            if (accountDao.findByUsername(conn, username) != null) {
                throw new ServiceException("用户名已存在");
            }
            String id = nextCustomerId(conn);
            Account account = new Account(id, username, displayName, phone,
                    Role.CUSTOMER, true, MembershipLevel.NORMAL);
            accountDao.insert(conn, account, PasswordHasher.hash(password));
            return account;
        });
    }

    /** 后台新增顾客。 */
    public Account addCustomer(String username, String displayName, String phone, String password) {
        return register(username, displayName, phone, password);
    }

    /** 顾客列表，keyword 可匹配用户名/姓名/手机号。 */
    public List<Account> customers(String keyword) {
        return Db.inTransaction(conn -> accountDao.findCustomers(conn, keyword));
    }

    /** 按主键取账号，不存在抛异常。 */
    public Account account(String accountId) {
        return Db.inTransaction(conn -> {
            Account account = accountDao.findById(conn, accountId);
            if (account == null) {
                throw new ServiceException("账号不存在");
            }
            return account;
        });
    }

    /** 后台修改顾客资料。 */
    public Account updateCustomerProfile(String accountId, String displayName,
                                         String phone, boolean enabled) {
        validateDisplayName(displayName);
        validatePhone(phone);
        return Db.inTransaction(conn -> {
            Account existing = accountDao.findById(conn, accountId);
            if (existing == null) {
                throw new ServiceException("账号不存在");
            }
            if (existing.role() != Role.CUSTOMER) {
                throw new ServiceException("不能编辑管理员账号");
            }
            accountDao.updateProfile(conn, accountId, displayName, phone, enabled);
            return accountDao.findById(conn, accountId);
        });
    }

    /** 后台禁用顾客。 */
    public boolean disableCustomer(String accountId) {
        Account existing = account(accountId);
        updateCustomerProfile(accountId, existing.displayName(), existing.phone(), false);
        return true;
    }

    /**
     * 后台手动调整会员等级（运营入口）。
     *
     * <p>允许跨档直定，也允许降档。前台用户的等级不走这里，
     * 而是在 {@link #payOrder(String)} 中按累计消费自动升级。</p>
     */
    public Account setMembership(String accountId, MembershipLevel level) {
        return Db.inTransaction(conn -> {
            Account existing = accountDao.findById(conn, accountId);
            if (existing == null) {
                throw new ServiceException("账号不存在");
            }
            if (existing.role() != Role.CUSTOMER) {
                throw new ServiceException("管理员账号没有会员等级");
            }
            accountDao.updateMembership(conn, accountId, level == null ? MembershipLevel.NORMAL : level);
            return accountDao.findById(conn, accountId);
        });
    }

    /** 累计消费金额（分），只统计已支付订单。 */
    public int spentFen(String accountId) {
        return Db.inTransaction(conn -> {
            if (accountDao.findById(conn, accountId) == null) {
                throw new ServiceException("账号不存在");
            }
            return orderDao.sumPaidAmount(conn, accountId);
        });
    }

    /** 会员页数据汇总。 */
    public MembershipStats membershipStats(String accountId) {
        return Db.inTransaction(conn -> {
            if (accountDao.findById(conn, accountId) == null) {
                throw new ServiceException("账号不存在");
            }
            return new MembershipStats(
                    orderDao.sumPaidAmount(conn, accountId),
                    orderDao.sumPaidDiscount(conn, accountId),
                    orderDao.countPaidOrders(conn, accountId),
                    orderDao.countPaidSeats(conn, accountId));
        });
    }

    // ========================================================================
    // 影片
    // ========================================================================

    public List<Movie> movies() {
        return Db.inTransaction(movieDao::findAll);
    }

    public List<Movie> searchMovies(String keyword) {
        return Db.inTransaction(conn -> movieDao.search(conn, keyword));
    }

    public Movie movie(String movieId) {
        return Db.inTransaction(conn -> requireMovie(conn, movieId));
    }

    public Movie addMovie(Movie movie) {
        validateMovie(movie);
        return Db.inTransaction(conn -> {
            String id = (movie.id() == null || movie.id().isBlank())
                    ? String.format(Locale.ROOT, "M%03d", movieDao.maxMovieSequence(conn) + 1)
                    : movie.id();
            if (movieDao.findById(conn, id) != null) {
                throw new ServiceException("影片编号已存在");
            }
            Movie saved = new Movie(id, movie.title(), movie.genre(), movie.director(), movie.starring(),
                    movie.durationMinutes(), movie.format(), movie.releaseDate(), movie.synopsis(),
                    movie.rating(), movie.posterPalette(), movie.posterPath());
            movieDao.insert(conn, saved);
            return saved;
        });
    }

    public Movie updateMovie(Movie movie) {
        validateMovie(movie);
        return Db.inTransaction(conn -> {
            if (movieDao.findById(conn, movie.id()) == null) {
                throw new ServiceException("影片不存在");
            }
            movieDao.update(conn, movie);
            return movie;
        });
    }

    public void deleteMovie(String movieId) {
        Db.runInTransaction(conn -> {
            if (movieDao.findById(conn, movieId) == null) {
                throw new ServiceException("影片不存在");
            }
            if (!sessionDao.findByMovie(conn, movieId).isEmpty()) {
                throw new ServiceException("该影片仍有关联场次，不能删除");
            }
            movieDao.delete(conn, movieId);
        });
    }

    // ========================================================================
    // 影厅
    // ========================================================================

    public List<CinemaHall> halls() {
        return Db.inTransaction(hallDao::findAll);
    }

    public CinemaHall hall(String hallId) {
        return Db.inTransaction(conn -> requireHall(conn, hallId));
    }

    /** 新增影厅，并自动按行列生成座位。 */
    public CinemaHall addHall(CinemaHall hall) {
        validateHall(hall);
        return Db.inTransaction(conn -> {
            String id = (hall.id() == null || hall.id().isBlank())
                    ? String.format(Locale.ROOT, "H%02d", hallDao.maxHallSequence(conn) + 1)
                    : hall.id();
            if (hallDao.findById(conn, id) != null) {
                throw new ServiceException("影厅编号已存在");
            }
            CinemaHall saved = new CinemaHall(id, hall.name(), hall.specs(), hall.rows(), hall.cols());
            hallDao.insert(conn, saved);
            hallDao.generateSeats(conn, saved);
            return saved;
        });
    }

    public CinemaHall updateHall(CinemaHall hall) {
        validateHall(hall);
        return Db.inTransaction(conn -> {
            if (hallDao.findById(conn, hall.id()) == null) {
                throw new ServiceException("影厅不存在");
            }
            hallDao.update(conn, hall);
            hallDao.generateSeats(conn, hall);
            return hall;
        });
    }

    public void deleteHall(String hallId) {
        Db.runInTransaction(conn -> {
            if (hallDao.findById(conn, hallId) == null) {
                throw new ServiceException("影厅不存在");
            }
            boolean hasSession = sessionDao.findAll(conn).stream()
                    .anyMatch(s -> s.hallId().equals(hallId));
            if (hasSession) {
                throw new ServiceException("该影厅仍有关联场次，不能删除");
            }
            hallDao.delete(conn, hallId);
        });
    }

    // ========================================================================
    // 场次
    // ========================================================================

    public List<ShowSession> sessionsForMovie(String movieId) {
        return Db.inTransaction(conn -> {
            requireMovie(conn, movieId);
            return sessionDao.findByMovie(conn, movieId);
        });
    }

    public List<ShowSession> allSessions() {
        return Db.inTransaction(sessionDao::findAll);
    }

    public ShowSession session(String sessionId) {
        return Db.inTransaction(conn -> requireSession(conn, sessionId));
    }

    /** 新增场次；散场时间按影片时长自动推算。 */
    public ShowSession addSession(String movieId, String hallId, String startTime, int priceFen) {
        if (priceFen < 0) {
            throw new ServiceException("票价不能为负数");
        }
        LocalDateTime start = parseTime(startTime);
        return Db.inTransaction(conn -> {
            Movie movie = requireMovie(conn, movieId);
            requireHall(conn, hallId);
            String id = String.format(Locale.ROOT, "S%03d", sessionDao.maxSessionSequence(conn) + 1);
            ShowSession session = new ShowSession(id, movieId, hallId,
                    start.format(DATE_TIME), start.plusMinutes(movie.durationMinutes()).format(DATE_TIME), priceFen);
            sessionDao.insert(conn, session);
            return session;
        });
    }

    public ShowSession updateSession(String sessionId, String movieId, String hallId,
                                     String startTime, int priceFen) {
        if (priceFen < 0) {
            throw new ServiceException("票价不能为负数");
        }
        LocalDateTime start = parseTime(startTime);
        return Db.inTransaction(conn -> {
            requireSession(conn, sessionId);
            if (hasOrdersForSession(conn, sessionId)) {
                throw new ServiceException("该场次已有订单，不能修改");
            }
            Movie movie = requireMovie(conn, movieId);
            requireHall(conn, hallId);
            ShowSession updated = new ShowSession(sessionId, movieId, hallId,
                    start.format(DATE_TIME), start.plusMinutes(movie.durationMinutes()).format(DATE_TIME), priceFen);
            sessionDao.update(conn, updated);
            return updated;
        });
    }

    public void deleteSession(String sessionId) {
        Db.runInTransaction(conn -> {
            requireSession(conn, sessionId);
            if (hasOrdersForSession(conn, sessionId)) {
                throw new ServiceException("该场次已有订单，不能删除");
            }
            sessionDao.delete(conn, sessionId);
        });
    }

    private boolean hasOrdersForSession(Connection conn, String sessionId) throws SQLException {
        String sql = "SELECT COUNT(*) FROM ticket_order_item WHERE session_id = ?";
        try (var ps = conn.prepareStatement(sql)) {
            ps.setString(1, sessionId);
            try (var rs = ps.executeQuery()) {
                return rs.next() && rs.getInt(1) > 0;
            }
        }
    }

    // ========================================================================
    // 座位与购物车
    // ========================================================================

    /** 查询某场次的座位图（影厅规格 + 已占座位）。 */
    public SeatPlan seatPlan(String sessionId) {
        return Db.inTransaction(conn -> {
            ShowSession session = requireSession(conn, sessionId);
            CinemaHall hall = requireHall(conn, session.hallId());
            return new SeatPlan(hall, session, seatDao.findSoldSeats(conn, sessionId));
        });
    }

    /** 加入购物车。校验座位格式、范围、是否已售、是否重复。 */
    public void addToCart(String userId, String sessionId, Set<String> requestedSeats) {
        Db.runInTransaction(conn -> {
            requireAccount(conn, userId);
            ShowSession session = requireSession(conn, sessionId);
            CinemaHall hall = requireHall(conn, session.hallId());
            Set<String> seats = normalizeSeats(hall, requestedSeats);

            Set<String> conflicts = seatDao.findConflictingSeats(conn, sessionId, seats);
            if (!conflicts.isEmpty()) {
                throw new ServiceException("所选座位包含已售座位，请重新选择：" + String.join("、", conflicts));
            }

            Set<String> alreadyInCart = new TreeSet<>();
            for (CartItem item : cartDao.findItems(conn, userId)) {
                if (item.sessionId().equals(sessionId)) {
                    alreadyInCart.addAll(item.seats());
                }
            }
            alreadyInCart.retainAll(seats);
            if (!alreadyInCart.isEmpty()) {
                throw new ServiceException("同一座位不能重复加入购物车：" + String.join("、", alreadyInCart));
            }

            cartDao.addSeats(conn, userId, sessionId, seats);
        });
    }

    public List<CartItem> cartItems(String userId) {
        return Db.inTransaction(conn -> {
            requireAccount(conn, userId);
            return cartDao.findItems(conn, userId);
        });
    }

    public int cartCount(String userId) {
        return Db.inTransaction(conn -> {
            requireAccount(conn, userId);
            return cartDao.countItems(conn, userId);
        });
    }

    /** 移除购物车中的一个场次条目（编号形如 C100，通过顺序定位）。 */
    public void removeCartItem(String userId, String cartItemId) {
        Db.runInTransaction(conn -> {
            List<CartItem> items = cartDao.findItems(conn, userId);
            for (CartItem item : items) {
                if (item.id().equals(cartItemId)) {
                    cartDao.removeBySession(conn, userId, item.sessionId());
                    return;
                }
            }
            throw new ServiceException("购物车条目不存在：" + cartItemId);
        });
    }

    public void clearCart(String userId) {
        Db.runInTransaction(conn -> cartDao.clear(conn, userId));
    }

    // ========================================================================
    // 下单与支付
    // ========================================================================

    /** 直接购买指定座位（不经过购物车）。 */
    public TicketOrder buySeatsNow(String userId, String sessionId, Set<String> seats) {
        return Db.inTransaction(conn -> {
            Account account = requireAccount(conn, userId);
            ShowSession session = requireSession(conn, sessionId);
            CinemaHall hall = requireHall(conn, session.hallId());
            Set<String> normalized = normalizeSeats(hall, seats);

            Set<String> conflicts = seatDao.findConflictingSeats(conn, sessionId, normalized);
            if (!conflicts.isEmpty()) {
                throw new ServiceException("所选座位已被他人购买，请重新选择：" + String.join("、", conflicts));
            }

            Movie movie = requireMovie(conn, session.movieId());
            TicketOrderItem item = new TicketOrderItem(sessionId, movie.title(), hall.name(),
                    session.startTime(), session.endTime(), session.priceFen(), normalized);
            return placeOrder(conn, account, List.of(item));
        });
    }

    /** 结算购物车，生成订单并清空购物车。 */
    public TicketOrder checkoutCart(String userId) {
        return Db.inTransaction(conn -> {
            Account account = requireAccount(conn, userId);
            List<CartItem> cartItems = cartDao.findItems(conn, userId);
            if (cartItems.isEmpty()) {
                throw new ServiceException("购物车为空");
            }

            List<TicketOrderItem> items = new ArrayList<>();
            for (CartItem cartItem : cartItems) {
                ShowSession session = requireSession(conn, cartItem.sessionId());
                Set<String> conflicts = seatDao.findConflictingSeats(conn, cartItem.sessionId(), cartItem.seats());
                if (!conflicts.isEmpty()) {
                    throw new ServiceException("购物车中的座位已被他人购买，请重新选择："
                            + String.join("、", conflicts));
                }
                items.add(new TicketOrderItem(cartItem.sessionId(), cartItem.movieTitle(), cartItem.hallName(),
                        cartItem.startTime(), cartItem.endTime(), cartItem.unitPriceFen(), cartItem.seats()));
                // 触发一次读取以确保场次存在，同时让 FOR UPDATE 覆盖到座位行
                if (session == null) {
                    throw new ServiceException("场次不存在");
                }
            }

            TicketOrder order = placeOrder(conn, account, items);
            cartDao.clear(conn, userId);
            return order;
        });
    }

    /**
     * 创建订单：算价、按会员折扣、写主表与明细。
     *
     * <p>座位占用由「订单明细 + 状态」推导，所以这里只需写入明细；
     * 待支付订单同样计入已占座位（见视图 {@code v_sold_seats}）。</p>
     */
    private TicketOrder placeOrder(Connection conn, Account account, List<TicketOrderItem> items)
            throws SQLException {
        int original = items.stream().mapToInt(TicketOrderItem::lineTotalFen).sum();
        int payable = Money.applyDiscount(original, account.membership().discountBasisPoints());

        String id = String.format(Locale.ROOT, "T%06d", orderDao.maxOrderSequence(conn) + 1);
        TicketOrder order = new TicketOrder(id, account.id(), account.username(), items,
                original, original - payable, payable, account.membership().label(),
                TicketOrder.STATUS_UNPAID, null, LocalDateTime.now().format(DATE_TIME_FULL));

        orderDao.insertOrder(conn, order);
        orderDao.insertItems(conn, id, items);
        return order;
    }

    /**
     * 支付订单：生成出票码、置为已支付，并按累计消费自动升级会员。
     *
     * <p>升级「只升不降」：后台手动定过更高档位的账号不会因消费额不够被自动降回。
     * 返回的订单带上本次升级信息，供界面提示「恭喜升级」。</p>
     */
    public TicketOrder payOrder(String orderId) {
        return Db.inTransaction(conn -> {
            TicketOrder order = orderDao.findById(conn, orderId);
            if (order == null) {
                throw new ServiceException("订单不存在");
            }
            if (!TicketOrder.STATUS_UNPAID.equals(order.status())) {
                throw new ServiceException("订单不是待支付状态");
            }

            String code = "TKT-" + order.id() + "-"
                    + String.format(Locale.ROOT, "%04d", ThreadLocalRandom.current().nextInt(10000));
            orderDao.updateStatus(conn, orderId, TicketOrder.STATUS_PAID, code);

            TicketOrder paid = new TicketOrder(order.id(), order.userId(), order.username(), order.items(),
                    order.originalFen(), order.discountFen(), order.payableFen(), order.membershipLabel(),
                    TicketOrder.STATUS_PAID, code, order.createdAt());

            return applyAutomaticUpgrade(conn, paid);
        });
    }

    /** 累计消费达标即自动升级；一次消费跨越多个档位也直接跳到对应档。 */
    private TicketOrder applyAutomaticUpgrade(Connection conn, TicketOrder order) throws SQLException {
        Account account = accountDao.findById(conn, order.userId());
        if (account == null || account.role() != Role.CUSTOMER) {
            return order;
        }
        MembershipLevel current = account.membership();
        MembershipLevel derived = MembershipLevel.forSpending(orderDao.sumPaidAmount(conn, order.userId()));
        if (derived.ordinal() <= current.ordinal()) {
            return order;
        }
        accountDao.updateMembership(conn, account.id(), derived);
        return order.withUpgrade(current, derived);
    }

    /**
     * 取消订单：状态置为已取消，座位随之释放。
     *
     * <p>因为占座口径是「未支付 + 已支付的订单明细」，改成 CANCELED 后
     * 视图 {@code v_sold_seats} 自然不再返回这些座位，无需手工删除占用记录。</p>
     */
    public TicketOrder cancelOrder(String orderId) {
        return Db.inTransaction(conn -> {
            TicketOrder order = orderDao.findById(conn, orderId);
            if (order == null) {
                throw new ServiceException("订单不存在");
            }
            if (TicketOrder.STATUS_CANCELED.equals(order.status())) {
                return order;
            }
            orderDao.updateStatus(conn, orderId, TicketOrder.STATUS_CANCELED, order.ticketCode());
            return orderDao.findById(conn, orderId);
        });
    }

    public List<TicketOrder> ordersForUser(String userId) {
        return Db.inTransaction(conn -> {
            requireAccount(conn, userId);
            return orderDao.findByUser(conn, userId);
        });
    }

    public List<TicketOrder> allOrders() {
        return Db.inTransaction(orderDao::findAll);
    }

    public TicketOrder order(String orderId) {
        return Db.inTransaction(conn -> {
            TicketOrder order = orderDao.findById(conn, orderId);
            if (order == null) {
                throw new ServiceException("订单不存在");
            }
            return order;
        });
    }

    // ========================================================================
    // 校验与工具
    // ========================================================================

    private Account requireAccount(Connection conn, String id) throws SQLException {
        Account account = accountDao.findById(conn, id);
        if (account == null) {
            throw new ServiceException("账号不存在：" + id);
        }
        return account;
    }

    private Movie requireMovie(Connection conn, String id) throws SQLException {
        Movie movie = movieDao.findById(conn, id);
        if (movie == null) {
            throw new ServiceException("影片不存在：" + id);
        }
        return movie;
    }

    private CinemaHall requireHall(Connection conn, String id) throws SQLException {
        CinemaHall hall = hallDao.findById(conn, id);
        if (hall == null) {
            throw new ServiceException("影厅不存在：" + id);
        }
        return hall;
    }

    private ShowSession requireSession(Connection conn, String id) throws SQLException {
        ShowSession session = sessionDao.findById(conn, id);
        if (session == null) {
            throw new ServiceException("场次不存在：" + id);
        }
        return session;
    }

    private String nextCustomerId(Connection conn) throws SQLException {
        return String.format(Locale.ROOT, "U-%03d", accountDao.maxCustomerSequence(conn) + 1);
    }

    /** 座位号规范化：统一大写、校验格式与是否越界、去重，并限制单次最多 6 座。 */
    private Set<String> normalizeSeats(CinemaHall hall, Set<String> requestedSeats) {
        if (requestedSeats == null || requestedSeats.isEmpty()) {
            throw new ServiceException("请至少选择一个座位");
        }
        if (requestedSeats.size() > MAX_SEATS_PER_ORDER) {
            throw new ServiceException("单次最多选择" + MAX_SEATS_PER_ORDER + "个座位");
        }
        Set<String> normalized = new TreeSet<>();
        for (String seat : requestedSeats) {
            var matcher = SEAT_PATTERN.matcher(seat.trim().toUpperCase(Locale.ROOT));
            if (!matcher.matches()) {
                throw new ServiceException("座位格式不正确：" + seat);
            }
            int row = matcher.group(1).charAt(0) - 'A';
            int col = Integer.parseInt(matcher.group(2)) - 1;
            if (row < 0 || row >= hall.rows() || col < 0 || col >= hall.cols()) {
                throw new ServiceException("座位超出" + hall.name() + "的范围：" + seat);
            }
            normalized.add(CinemaHallDao.seatKey(row, col));
        }
        return normalized;
    }

    private LocalDateTime parseTime(String text) {
        try {
            return ShowSessionDao.parse(text);
        } catch (IllegalArgumentException e) {
            throw new ServiceException(e.getMessage());
        }
    }

    private void validateMovie(Movie movie) {
        requireText(movie.title(), "请输入影片名称");
        requireText(movie.genre(), "请输入影片类型");
        requireText(movie.director(), "请输入导演");
        if (movie.durationMinutes() <= 0) {
            throw new ServiceException("影片时长必须大于0");
        }
        if (movie.rating() < 0 || movie.rating() > 10) {
            throw new ServiceException("评分应在0到10之间");
        }
    }

    private void validateHall(CinemaHall hall) {
        requireText(hall.name(), "请输入影厅名称");
        requireText(hall.specs(), "请输入影厅规格");
        if (hall.rows() <= 0 || hall.cols() <= 0) {
            throw new ServiceException("影厅行列数必须大于0");
        }
    }

    private void validateUsername(String username) {
        requireText(username, "请输入用户名");
        if (username.length() < 3 || username.length() > 20) {
            throw new ServiceException("用户名长度应为3到20个字符");
        }
        if (!username.matches("[A-Za-z0-9_]+")) {
            throw new ServiceException("用户名只能包含字母、数字和下划线");
        }
    }

    private void validatePassword(String password) {
        if (password == null || password.length() < 6 || password.length() > 20) {
            throw new ServiceException("密码长度应为6到20位");
        }
    }

    private void validatePhone(String phone) {
        if (phone == null || !phone.matches("1\\d{10}")) {
            throw new ServiceException("请输入11位手机号");
        }
    }

    private void validateDisplayName(String displayName) {
        requireText(displayName, "请输入姓名");
        if (displayName.trim().length() < 2 || displayName.trim().length() > 20) {
            throw new ServiceException("姓名长度应为2到20个字符");
        }
    }

    private void requireText(String value, String message) {
        if (value == null || value.isBlank()) {
            throw new ServiceException(message);
        }
    }

    // ========================================================================
    // 演示数据重置
    // ========================================================================

    /**
     * 清理验证过程产生的数据，使验证程序可以反复运行。
     *
     * <p>只删两类东西：</p>
     * <ul>
     *   <li>注册名以 {@code demo_u} 开头的测试顾客（连同其订单，靠外键级联）</li>
     *   <li>非种子订单：即 id 不在 T001001 / T001002 之列的历史订单</li>
     * </ul>
     * <p>影片、影厅、场次、演示账号等种子数据一律不动。
     * 购物车随测试顾客级联删除，无需单独处理。</p>
     *
     * @return {@code [清理顾客数, 清理订单数]}
     */
    public int[] resetTestData() {
        return Db.inTransaction(conn -> {
            int[] result = new int[2];

            // 先删非种子订单（明细随外键级联删除），座位占用随之释放
            String deleteOrders = "DELETE FROM ticket_order"
                    + " WHERE id NOT IN ('T001001','T001002')";
            try (var ps = conn.prepareStatement(deleteOrders)) {
                result[1] = ps.executeUpdate();
            }

            // 再删测试顾客（其购物车与订单靠外键级联清理）
            String deleteAccounts = "DELETE FROM account WHERE username LIKE 'demo\\_u%'";
            try (var ps = conn.prepareStatement(deleteAccounts)) {
                result[0] = ps.executeUpdate();
            }

            return result;
        });
    }

    /** 便于外部使用：座位号生成规则。 */
    public static String seatKey(int row, int col) {
        return CinemaHallDao.seatKey(row, col);
    }

    /** 便于外部使用：把座位集合转为展示文本，如「A1、A2」。 */
    public static String joinSeats(Set<String> seats) {
        return String.join("、", new LinkedHashSet<>(seats));
    }
}
