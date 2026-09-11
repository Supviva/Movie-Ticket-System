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
import com.movieticket.model.TicketOrderItem;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.regex.Pattern;

public final class DemoMovieTicketService {

    private static final DateTimeFormatter DATE_TIME =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm", Locale.ROOT);
    private static final Pattern SEAT_PATTERN = Pattern.compile("^([A-Z])(\\d+)$");

    private final Map<String, String> passwords = new LinkedHashMap<>();
    private final Map<String, Account> accounts = new LinkedHashMap<>();
    private final Map<String, Movie> movies = new LinkedHashMap<>();
    private final Map<String, CinemaHall> halls = new LinkedHashMap<>();
    private final Map<String, ShowSession> sessions = new LinkedHashMap<>();
    private final Map<String, Set<String>> soldSeats = new HashMap<>();
    private final Map<String, List<CartItem>> carts = new HashMap<>();
    private final Map<String, TicketOrder> orders = new LinkedHashMap<>();

    private final AtomicInteger cartSequence = new AtomicInteger(100);
    private final AtomicInteger orderSequence = new AtomicInteger(1000);
    private final AtomicInteger movieSequence = new AtomicInteger(36);
    private final AtomicInteger hallSequence = new AtomicInteger(3);
    private final AtomicInteger sessionSequence = new AtomicInteger(9);
    private final AtomicInteger customerSequence = new AtomicInteger(1);

    public DemoMovieTicketService() {
        seedAccounts();
        seedMovies();
        seedHalls();
        seedSessions();
    }

    private void seedAccounts() {
        addAccount("admin", "管理员", "13800000001", "123456", Role.ADMIN, true, MembershipLevel.NORMAL);
        addAccount("customer", "演示用户", "13800138000", "123456", Role.CUSTOMER, true, MembershipLevel.SILVER);
        addAccount("chen", "陈一诺", "13911110001", "123456", Role.CUSTOMER, true, MembershipLevel.GOLD);
        addAccount("lin", "林远", "13911110002", "123456", Role.CUSTOMER, true, MembershipLevel.NORMAL);
        addAccount("zhou", "周晴", "13911110003", "123456", Role.CUSTOMER, true, MembershipLevel.SILVER);
        addAccount("xu", "徐舟", "13911110004", "123456", Role.CUSTOMER, false, MembershipLevel.NORMAL);
    }

    private void addAccount(String username, String displayName, String phone, String password,
                            Role role, boolean enabled, MembershipLevel membership) {
        String id = role == Role.ADMIN ? "A-" + username : "U-" + String.format("%03d", customerSequence.getAndIncrement());
        accounts.put(id, new Account(id, username, displayName, phone, role, enabled, membership));
        passwords.put(id, password);
    }

    private void seedMovies() {
        movies.put("M001", new Movie("M001", "泰坦尼克号", "爱情", "詹姆斯·卡梅隆", "莱昂纳多·迪卡普里奥、凯特·温斯莱特",
                194, "2D", "1997-12-19", "1912年，穷画家杰克与富家少女罗丝在泰坦尼克号上坠入爱河，巨轮撞上冰山沉没，留下了一段刻骨铭心的爱情绝唱。",
                9.5, 0));
        movies.put("M002", new Movie("M002", "阿凡达", "科幻", "詹姆斯·卡梅隆", "萨姆·沃辛顿、佐伊·索尔达娜",
                162, "IMAX", "2009-12-18", "前海军陆战队员杰克前往潘多拉星球，操控“阿凡达”分身融入纳美族，在人类与纳美人的冲突中重新认识生命的意义。",
                8.8, 1));
        movies.put("M003", new Movie("M003", "盗梦空间", "悬疑", "克里斯托弗·诺兰", "莱昂纳多·迪卡普里奥、玛丽昂·歌迪亚",
                148, "2D", "2010-07-16", "造梦师柯布带领团队潜入他人梦境窃取机密，层层嵌套的梦境让现实与虚幻的边界逐渐模糊。",
                9.4, 2));
        movies.put("M004", new Movie("M004", "肖申克的救赎", "剧情", "弗兰克·德拉邦特", "蒂姆·罗宾斯、摩根·弗里曼",
                142, "2D", "1994-09-23", "银行家安迪被冤判入狱，在肖申克监狱中用希望与智慧救赎自己，也照亮了狱中好友瑞德的人生。",
                9.7, 3));
        movies.put("M005", new Movie("M005", "千与千寻", "动画", "宫崎骏", "配音：柊瑠美、入野自由",
                125, "2D", "2001-07-20", "少女千寻随父母误入神灵世界，父母因贪吃变成猪，她在汤屋打工并帮助白龙找回名字，最终救回父母回到人间。",
                9.4, 4));
        movies.put("M006", new Movie("M006", "大话西游之大圣娶亲", "喜剧", "刘镇伟", "周星驰、朱茵",
                95, "2D", "1995-02-04", "至尊宝为救白晶晶穿越时空，却与紫霞仙子相遇相爱，“爱你一万年”的告白成为一代人的经典记忆。",
                9.2, 5));
        movies.put("M009", new Movie("M009", "夏日蝉鸣", "青春", "林夏", "唐果、苏屿",
                101, "2D", "2026-09-24", "毕业前的最后一个夏天，五个少年用一场公路旅行告别青春，也重新找到各自的方向。",
                8.4, 2));
        // 高热度商业大片：评分亮眼、题材偏动作/奇幻，用于首页「正在热映」分区
        movies.put("M019", new Movie("M019", "复仇者联盟", "动作", "乔斯·韦登", "小罗伯特·唐尼、克里斯·埃文斯",
                143, "IMAX", "2012-05-05", "邪神洛基携外星大军入侵地球，六位超级英雄首次集结，为守护人类而并肩作战。",
                9.3, 0));
        movies.put("M020", new Movie("M020", "复仇者联盟2：奥创纪元", "动作", "乔斯·韦登", "小罗伯特·唐尼、克里斯·海姆斯沃斯",
                141, "IMAX", "2015-05-12", "托尼·斯塔克启动的维和计划失控，诞生了人工智能奥创，复仇者联盟必须再度集结阻止人类灭绝。",
                8.9, 1));
        movies.put("M021", new Movie("M021", "复仇者联盟3：无限战争", "动作", "安东尼·罗素", "小罗伯特·唐尼、克里斯·海姆斯沃斯",
                149, "IMAX", "2018-05-11", "灭霸为集齐六颗无限宝石席卷宇宙，复仇者联盟倾尽全力仍未阻止那一声响指。",
                9.1, 2));
        movies.put("M022", new Movie("M022", "复仇者联盟4：终局之战", "动作", "安东尼·罗素", "小罗伯特·唐尼、克里斯·埃文斯",
                181, "IMAX", "2019-04-24", "幸存的复仇者穿越时间线夺回无限宝石，为逝去的战友与半个宇宙发起最后一战。",
                9.4, 3));
        movies.put("M027", new Movie("M027", "星际穿越", "科幻", "克里斯托弗·诺兰", "马修·麦康纳、安妮·海瑟薇",
                169, "IMAX", "2014-11-12", "地球濒临枯竭，宇航员穿越虫洞为人类寻找新家园，而时间在相对论中悄然流逝。",
                9.2, 2));
        // 待映大片：档期在次年及以后，供首页「即将上映」分区使用
        movies.put("M029", new Movie("M029", "复仇者联盟5：康之王朝", "动作", "德斯汀·克里顿", "小罗伯特·唐尼、安东尼·麦凯",
                156, "IMAX", "2027-05-07", "漫威电影宇宙新纪元开启，多元宇宙的裂缝彻底撕裂，复仇者联盟必须面对来自时间尽头的征服者康。",
                9.6, 4));
        movies.put("M030", new Movie("M030", "复仇者联盟6：秘密战争", "动作", "安东尼·罗素", "克里斯·海姆斯沃斯、布丽·拉尔森",
                172, "IMAX", "2027-11-12", "平行宇宙正面相撞，所有已知的复仇者被迫在同一战场上集结，为整个多元宇宙的存续而战。",
                9.5, 5));
        movies.put("M031", new Movie("M031", "阿凡达3：火与烬", "科幻", "詹姆斯·卡梅隆", "萨姆·沃辛顿、佐伊·索尔达娜",
                195, "3D", "2027-12-17", "潘多拉的海洋深处沉睡着一支灰烬部族，杰克一家必须在火与水的对峙中守护纳美人的未来。",
                9.4, 0));
        movies.put("M034", new Movie("M034", "碟中谍8：最终清算", "动作", "克里斯托弗·麦奎里", "汤姆·克鲁斯、海莉·阿特维尔",
                164, "IMAX", "2027-05-21", "智体即将全面接管全球系统，伊森·亨特必须在最后一役中做出比生命更重的抉择。",
                9.3, 3));
    }

    private void seedHalls() {
        halls.put("H01", new CinemaHall("H01", "1号杜比厅", "杜比全景声", 8, 12));
        halls.put("H02", new CinemaHall("H02", "2号激光厅", "激光放映", 6, 10));
        halls.put("H03", new CinemaHall("H03", "3号IMAX厅", "IMAX", 9, 14));
    }

    private void seedSessions() {
        // 全部影片从明天起每日滚动排片，一直持续到本月最后一天（9 月底），
        // 保证「正在热映」与「即将上映」的每一部片都有可购票的场次。
        List<String> order = List.of(
                "M001", "M002", "M003", "M004", "M005", "M006",
                "M009", "M019", "M020", "M021", "M022", "M027",
                "M029", "M030", "M031", "M034");

        String[] halls = {"H01", "H02", "H03"};
        int[][] slots = {{14, 0}, {16, 30}, {19, 30}};

        LocalDate today = LocalDate.now();
        int totalDays = today.lengthOfMonth() - today.getDayOfMonth(); // 距月底天数（9 月为 19）

        for (int day = 1; day <= totalDays; day++) {
            for (int slot = 0; slot < slots.length; slot++) {
                String movieId = order.get((day - 1 + slot) % order.size());
                String hallId = halls[(day + slot) % halls.length];
                int[] time = slots[slot];
                addSessionRow(movieId, hallId, day, time[0], time[1], basePrice(movieId));
            }
        }
        markSeedsSold();
    }

    /** 每部影片的基准票价（单位：分），IMAX/3D 大片定价更高。 */
    private int basePrice(String movieId) {
        return switch (movieId) {
            case "M001" -> 4600;
            case "M002" -> 6200;
            case "M003" -> 3900;
            case "M004" -> 4500;
            case "M005" -> 4100;
            case "M006" -> 4800;
            case "M009" -> 3800;
            case "M019" -> 5600;
            case "M020" -> 5800;
            case "M021" -> 6000;
            case "M022" -> 6200;
            case "M027" -> 6800;
            case "M029" -> 7800;
            case "M030" -> 8200;
            case "M031" -> 8900;
            case "M034" -> 7500;
            default -> 5000;
        };
    }

    private void addSessionRow(String movieId, String hallId, int days, int hour, int minute, int priceFen) {
        Movie movie = movies.get(movieId);
        String start = LocalDate.now().plusDays(days).atTime(hour, minute).format(DATE_TIME);
        String end = LocalDateTime.parse(start, DATE_TIME)
                .plusMinutes(movie.durationMinutes())
                .format(DATE_TIME);
        String id = String.format(Locale.ROOT, "S%03d", sessionSequence.getAndIncrement());
        sessions.put(id, new ShowSession(id, movieId, hallId, start, end, priceFen));
        soldSeats.put(id, new HashSet<>());
    }

    private void markSeedsSold() {
        int index = 0;
        for (Map.Entry<String, ShowSession> entry : sessions.entrySet()) {
            CinemaHall hall = halls.get(entry.getValue().hallId());
            Set<String> sold = new HashSet<>();
            for (int row = 0; row < hall.rows(); row++) {
                for (int col = 0; col < hall.cols(); col++) {
                    int mix = (row * 7 + col * 3 + index) % 9;
                    if (mix == 0 || (row == hall.rows() / 2 && col == hall.cols() / 2)) {
                        sold.add(seatKey(row, col));
                    }
                }
            }
            soldSeats.put(entry.getKey(), sold);
            index++;
        }
    }

    // Accounts ---------------------------------------------------------------

    public synchronized Account login(String username, String password) {
        Account account = findAccountByUsername(username);
        if (account == null || !passwordMatches(account.id(), password)) {
            return null;
        }
        if (!account.enabled()) {
            throw new DemoException("该账号已被禁用，请联系管理员");
        }
        return account;
    }

    public synchronized Account register(String username, String displayName, String phone, String password) {
        validateUsername(username);
        if (findAccountByUsername(username) != null) {
            throw new DemoException("用户名已存在");
        }
        validatePassword(password);
        validatePhone(phone);
        validateDisplayName(displayName);

        String id = "U-" + String.format(Locale.ROOT, "%03d", customerSequence.getAndIncrement());
        Account account = new Account(id, username, displayName, phone, Role.CUSTOMER, true, MembershipLevel.NORMAL);
        accounts.put(id, account);
        passwords.put(id, password);
        return account;
    }

    public synchronized Account addCustomer(String username, String displayName, String phone, String password) {
        if (findAccountByUsername(username) != null) {
            throw new DemoException("用户名已存在");
        }
        return register(username, displayName, phone, password);
    }

    public synchronized List<Account> customers(String keyword) {
        String normalized = keyword == null ? "" : keyword.trim().toLowerCase(Locale.ROOT);
        return accounts.values().stream()
                .filter(account -> account.role() == Role.CUSTOMER)
                .filter(account -> normalized.isEmpty()
                        || account.username().toLowerCase(Locale.ROOT).contains(normalized)
                        || account.displayName().contains(keyword.trim())
                        || account.phone().contains(normalized))
                .sorted(Comparator.comparing(Account::id))
                .toList();
    }

    public synchronized Account account(String accountId) {
        Account account = accounts.get(accountId);
        if (account == null) {
            throw new DemoException("账号不存在");
        }
        return account;
    }

    public synchronized Account updateCustomerProfile(String accountId, String displayName,
                                                      String phone, boolean enabled) {
        Account existing = account(accountId);
        if (existing.role() != Role.CUSTOMER) {
            throw new DemoException("不能编辑管理员账号");
        }
        validateDisplayName(displayName);
        validatePhone(phone);
        Account updated = existing.withProfile(displayName, phone, enabled);
        accounts.put(accountId, updated);
        return updated;
    }

    /**
     * 后台手动调整会员等级。
     *
     * <p>这是运营入口，允许跨越门槛直接定档，也允许降档。前台用户侧的等级
     * 不通过这里变更，而是由 {@link #payOrder(String)} 按累计消费自动升级。</p>
     */
    public synchronized Account setMembership(String accountId, MembershipLevel level) {
        Account existing = account(accountId);
        if (existing.role() != Role.CUSTOMER) {
            throw new DemoException("管理员账号没有会员等级");
        }
        if (level == null) {
            level = MembershipLevel.NORMAL;
        }
        Account updated = existing.withMembership(level);
        accounts.put(accountId, updated);
        return updated;
    }

    /** 该账号的累计消费金额（分），只统计已出票订单。 */
    public synchronized int spentFen(String accountId) {
        account(accountId);
        return accumulateSpent(accountId);
    }

    private int accumulateSpent(String accountId) {
        return orders.values().stream()
                .filter(order -> order.userId().equals(accountId) && "PAID".equals(order.status()))
                .mapToInt(TicketOrder::payableFen)
                .sum();
    }

    public synchronized boolean disableCustomer(String accountId) {
        updateCustomerProfile(accountId, account(accountId).displayName(), account(accountId).phone(), false);
        return true;
    }

    // Movies / halls / sessions ----------------------------------------------

    public synchronized List<Movie> movies() {
        return movies.values().stream()
                .sorted(Comparator.comparing(Movie::id))
                .toList();
    }

    public synchronized List<Movie> searchMovies(String keyword) {
        String normalized = keyword == null ? "" : keyword.trim().toLowerCase(Locale.ROOT);
        return movies.values().stream()
                .filter(movie -> normalized.isEmpty()
                        || movie.title().toLowerCase(Locale.ROOT).contains(normalized)
                        || movie.genre().contains(keyword.trim())
                        || movie.director().toLowerCase(Locale.ROOT).contains(normalized)
                        || movie.starring().toLowerCase(Locale.ROOT).contains(normalized))
                .sorted(Comparator.comparing(Movie::id))
                .toList();
    }

    public synchronized Movie movie(String movieId) {
        Movie movie = movies.get(movieId);
        if (movie == null) {
            throw new DemoException("影片不存在");
        }
        return movie;
    }

    public synchronized Movie addMovie(Movie movie) {
        validateMovie(movie);
        String id = movie.id() == null || movie.id().isBlank()
                ? String.format(Locale.ROOT, "M%03d", movieSequence.incrementAndGet())
                : movie.id();
        if (movies.containsKey(id)) {
            throw new DemoException("影片编号已存在");
        }
        Movie saved = new Movie(id, movie.title(), movie.genre(), movie.director(), movie.starring(),
                movie.durationMinutes(), movie.format(), movie.releaseDate(), movie.synopsis(),
                movie.rating(), movie.posterPalette());
        movies.put(id, saved);
        return saved;
    }

    public synchronized Movie updateMovie(Movie movie) {
        if (!movies.containsKey(movie.id())) {
            throw new DemoException("影片不存在");
        }
        validateMovie(movie);
        movies.put(movie.id(), movie);
        return movie;
    }

    public synchronized void deleteMovie(String movieId) {
        if (!movies.containsKey(movieId)) {
            throw new DemoException("影片不存在");
        }
        if (sessions.values().stream().anyMatch(session -> session.movieId().equals(movieId))) {
            throw new DemoException("该影片仍有关联场次，不能删除");
        }
        if (orders.values().stream().anyMatch(order -> orderContainsMovie(order, movieId))) {
            throw new DemoException("该影片已有历史订单，不能删除");
        }
        movies.remove(movieId);
    }

    private boolean orderContainsMovie(TicketOrder order, String movieId) {
        return order.items().stream().anyMatch(item -> {
            ShowSession session = sessions.get(item.sessionId());
            return session != null && session.movieId().equals(movieId);
        });
    }

    public synchronized List<CinemaHall> halls() {
        return halls.values().stream()
                .sorted(Comparator.comparing(CinemaHall::id))
                .toList();
    }

    public synchronized CinemaHall hall(String hallId) {
        CinemaHall hall = halls.get(hallId);
        if (hall == null) {
            throw new DemoException("影厅不存在");
        }
        return hall;
    }

    public synchronized CinemaHall addHall(CinemaHall hall) {
        validateHall(hall);
        String id = hall.id() == null || hall.id().isBlank()
                ? String.format(Locale.ROOT, "H%02d", hallSequence.incrementAndGet())
                : hall.id();
        if (halls.containsKey(id)) {
            throw new DemoException("影厅编号已存在");
        }
        CinemaHall saved = new CinemaHall(id, hall.name(), hall.specs(), hall.rows(), hall.cols());
        halls.put(id, saved);
        return saved;
    }

    public synchronized CinemaHall updateHall(CinemaHall hall) {
        if (!halls.containsKey(hall.id())) {
            throw new DemoException("影厅不存在");
        }
        validateHall(hall);
        halls.put(hall.id(), hall);
        return hall;
    }

    public synchronized void deleteHall(String hallId) {
        if (!halls.containsKey(hallId)) {
            throw new DemoException("影厅不存在");
        }
        if (sessions.values().stream().anyMatch(session -> session.hallId().equals(hallId))) {
            throw new DemoException("该影厅仍有关联场次，不能删除");
        }
        halls.remove(hallId);
    }

    public synchronized List<ShowSession> sessionsForMovie(String movieId) {
        movie(movieId);
        return sessions.values().stream()
                .filter(session -> session.movieId().equals(movieId))
                .sorted(Comparator.comparing(ShowSession::startTime))
                .toList();
    }

    public synchronized List<ShowSession> allSessions() {
        return sessions.values().stream()
                .sorted(Comparator.comparing(ShowSession::startTime))
                .toList();
    }

    public synchronized ShowSession session(String sessionId) {
        ShowSession session = sessions.get(sessionId);
        if (session == null) {
            throw new DemoException("场次不存在");
        }
        return session;
    }

    public synchronized ShowSession addSession(String movieId, String hallId, String startTime, int priceFen) {
        movie(movieId);
        hall(hallId);
        LocalDateTime start;
        try {
            start = LocalDateTime.parse(startTime, DATE_TIME);
        } catch (Exception ex) {
            throw new DemoException("开始时间格式应为 yyyy-MM-dd HH:mm");
        }
        if (priceFen < 0) {
            throw new DemoException("票价不能为负数");
        }
        String id = String.format(Locale.ROOT, "S%03d", sessionSequence.incrementAndGet());
        Movie movie = movie(movieId);
        String end = start.plusMinutes(movie.durationMinutes()).format(DATE_TIME);
        ShowSession session = new ShowSession(id, movieId, hallId, start.format(DATE_TIME), end, priceFen);
        sessions.put(id, session);
        soldSeats.put(id, new HashSet<>());
        return session;
    }

    public synchronized ShowSession updateSession(String sessionId, String movieId, String hallId,
                                                  String startTime, int priceFen) {
        ShowSession existing = session(sessionId);
        if (orders.values().stream().anyMatch(order -> orderContainsSession(order, sessionId))) {
            throw new DemoException("该场次已有订单，不能修改");
        }
        movie(movieId);
        hall(hallId);
        LocalDateTime start;
        try {
            start = LocalDateTime.parse(startTime, DATE_TIME);
        } catch (Exception ex) {
            throw new DemoException("开始时间格式应为 yyyy-MM-dd HH:mm");
        }
        if (priceFen < 0) {
            throw new DemoException("票价不能为负数");
        }
        String end = start.plusMinutes(movie(movieId).durationMinutes()).format(DATE_TIME);
        ShowSession updated = new ShowSession(existing.id(), movieId, hallId,
                start.format(DATE_TIME), end, priceFen);
        sessions.put(existing.id(), updated);
        soldSeats.put(existing.id(), new HashSet<>());
        return updated;
    }

    private boolean orderContainsSession(TicketOrder order, String sessionId) {
        return order.items().stream().anyMatch(item -> item.sessionId().equals(sessionId));
    }

    public synchronized void deleteSession(String sessionId) {
        ShowSession existing = session(sessionId);
        if (orders.values().stream().anyMatch(order -> orderContainsSession(order, sessionId))) {
            throw new DemoException("该场次已有订单，不能删除");
        }
        sessions.remove(existing.id());
        soldSeats.remove(existing.id());
    }

    // Seats and cart ---------------------------------------------------------

    public synchronized SeatPlan seatPlan(String sessionId) {
        ShowSession showSession = session(sessionId);
        return new SeatPlan(hall(showSession.hallId()), showSession,
                new TreeSet<>(soldSeats.getOrDefault(sessionId, Set.of())));
    }

    public synchronized void addToCart(String userId, String sessionId, Set<String> requestedSeats) {
        Account account = account(userId);
        ShowSession showSession = session(sessionId);
        CinemaHall hall = hall(showSession.hallId());
        Set<String> seats = normalizeSeats(hall, showSession, requestedSeats);
        Set<String> sold = soldSeats.getOrDefault(sessionId, Set.of());
        if (seats.stream().anyMatch(sold::contains)) {
            throw new DemoException("所选座位包含已售座位，请重新选择");
        }

        List<CartItem> userCart = carts.computeIfAbsent(userId, ignored -> new ArrayList<>());
        Set<String> allExistingSeats = new HashSet<>();
        for (CartItem item : userCart) {
            if (item.sessionId().equals(sessionId)) {
                allExistingSeats.addAll(item.seats());
            }
        }
        if (seats.stream().anyMatch(allExistingSeats::contains)) {
            throw new DemoException("同一座位不能重复加入购物车");
        }
        Movie movie = movie(showSession.movieId());
        String id = "C" + cartSequence.getAndIncrement();
        userCart.add(new CartItem(id, sessionId, movie.title(), hall.name(),
                showSession.startTime(), showSession.endTime(), showSession.priceFen(), seats));
    }

    private Set<String> normalizeSeats(CinemaHall hall, ShowSession showSession, Set<String> requestedSeats) {
        if (requestedSeats == null || requestedSeats.isEmpty()) {
            throw new DemoException("请至少选择一个座位");
        }
        if (requestedSeats.size() > 6) {
            throw new DemoException("单次最多选择6个座位");
        }
        TreeSet<String> normalized = new TreeSet<>();
        for (String seat : requestedSeats) {
            var matcher = SEAT_PATTERN.matcher(seat.trim().toUpperCase(Locale.ROOT));
            if (!matcher.matches()) {
                throw new DemoException("座位格式不正确：" + seat);
            }
            int row = matcher.group(1).charAt(0) - 'A';
            int col = Integer.parseInt(matcher.group(2)) - 1;
            if (row < 0 || row >= hall.rows() || col < 0 || col >= hall.cols()) {
                throw new DemoException("座位超出" + hall.name() + "的范围：" + seat);
            }
            normalized.add(seatKey(row, col));
        }
        return normalized;
    }

    public synchronized List<CartItem> cartItems(String userId) {
        account(userId);
        return List.copyOf(carts.getOrDefault(userId, List.of()));
    }

    public synchronized int cartCount(String userId) {
        return carts.getOrDefault(userId, List.of()).size();
    }

    public synchronized void removeCartItem(String userId, String cartItemId) {
        List<CartItem> userCart = carts.getOrDefault(userId, new ArrayList<>());
        userCart.removeIf(item -> item.id().equals(cartItemId));
    }

    public synchronized void clearCart(String userId) {
        carts.put(userId, new ArrayList<>());
    }

    public synchronized TicketOrder buySeatsNow(String userId, String sessionId, Set<String> seats) {
        Account account = account(userId);
        ShowSession showSession = session(sessionId);
        CinemaHall hall = hall(showSession.hallId());
        Set<String> normalized = normalizeSeats(hall, showSession, seats);
        verifyNotSold(showSession, normalized);
        Movie movie = movie(showSession.movieId());
        String itemId = "OI" + cartSequence.incrementAndGet();
        TicketOrderItem item = new TicketOrderItem(itemId, sessionId, movie.title(), hall.name(),
                showSession.startTime(), showSession.endTime(), showSession.priceFen(), normalized);
        return placeOrder(account, List.of(item));
    }

    public synchronized TicketOrder checkoutCart(String userId) {
        Account account = account(userId);
        List<CartItem> items = carts.getOrDefault(userId, List.of());
        if (items.isEmpty()) {
            throw new DemoException("购物车为空");
        }
        List<TicketOrderItem> orderItems = new ArrayList<>();
        for (CartItem cartItem : items) {
            ShowSession showSession = session(cartItem.sessionId());
            verifyNotSold(showSession, cartItem.seats());
            orderItems.add(new TicketOrderItem("OI" + cartSequence.incrementAndGet(),
                    cartItem.sessionId(), cartItem.movieTitle(), cartItem.hallName(),
                    cartItem.startTime(), cartItem.endTime(), cartItem.unitPriceFen(), cartItem.seats()));
        }
        TicketOrder order = placeOrder(account, orderItems);
        carts.put(userId, new ArrayList<>());
        return order;
    }

    private void verifyNotSold(ShowSession showSession, Set<String> seats) {
        Set<String> sold = soldSeats.getOrDefault(showSession.id(), Set.of());
        if (seats.stream().anyMatch(sold::contains)) {
            throw new DemoException("所选座位已被他人购买，请重新选择");
        }
    }

    private TicketOrder placeOrder(Account account, List<TicketOrderItem> orderItems) {
        int original = orderItems.stream().mapToInt(TicketOrderItem::lineTotalFen).sum();
        int payable = Money.applyDiscount(original, account.membership().discountBasisPoints());
        String id = String.format(Locale.ROOT, "T%06d", orderSequence.incrementAndGet());
        TicketOrder order = new TicketOrder(id, account.id(), account.username(), List.copyOf(orderItems),
                original, original - payable, payable, account.membership().label(),
                "UNPAID", "", LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss", Locale.ROOT)));
        orders.put(id, order);
        for (TicketOrderItem item : orderItems) {
            soldSeats.computeIfAbsent(item.sessionId(), ignored -> new HashSet<>()).addAll(item.seats());
        }
        return order;
    }

    /**
     * 支付订单。
     *
     * <p>出票成功后按累计消费金额重算会员等级——只升不降，等级完全由真实消费决定，
     * 前端无法直接改档。返回的 {@link TicketOrder} 已带上本次升级信息，
     * 供界面提示「恭喜升级」。</p>
     */
    public synchronized TicketOrder payOrder(String orderId) {
        TicketOrder order = requireOrder(orderId);
        if (!"UNPAID".equals(order.status())) {
            throw new DemoException("订单不是待支付状态");
        }
        String code = "TKT-" + order.id() + "-"
                + String.format(Locale.ROOT, "%04d", ThreadLocalRandom.current().nextInt(10000));
        TicketOrder paid = new TicketOrder(order.id(), order.userId(), order.username(), order.items(),
                order.originalFen(), order.discountFen(), order.payableFen(), order.membershipLabel(),
                "PAID", code, order.createdAt());
        orders.put(orderId, paid);
        return applyAutomaticUpgrade(paid);
    }

    /**
     * 累计消费达标即自动升级，一次消费跨越多个档位也直接跳到对应档。
     *
     * <p>只升不降：后台手动定过更高档位的账号，不会因为消费额不够而被自动降回来。</p>
     */
    private TicketOrder applyAutomaticUpgrade(TicketOrder order) {
        Account account = accounts.get(order.userId());
        if (account == null || account.role() != Role.CUSTOMER) {
            return order;
        }
        MembershipLevel current = account.membership();
        MembershipLevel derived = MembershipLevel.forSpending(accumulateSpent(order.userId()));
        if (derived.ordinal() <= current.ordinal()) {
            return order;
        }
        accounts.put(account.id(), account.withMembership(derived));
        return order.withUpgrade(current, derived);
    }

    public synchronized TicketOrder cancelOrder(String orderId) {
        TicketOrder order = requireOrder(orderId);
        if ("CANCELED".equals(order.status())) {
            return order;
        }
        for (TicketOrderItem item : order.items()) {
            Set<String> sold = soldSeats.getOrDefault(item.sessionId(), Set.of());
            Set<String> remaining = new LinkedHashSet<>(sold);
            remaining.removeAll(item.seats());
            soldSeats.put(item.sessionId(), remaining);
        }
        TicketOrder canceled = new TicketOrder(order.id(), order.userId(), order.username(), order.items(),
                order.originalFen(), order.discountFen(), order.payableFen(), order.membershipLabel(),
                "CANCELED", order.ticketCode(), order.createdAt());
        orders.put(orderId, canceled);
        return canceled;
    }

    public synchronized List<TicketOrder> ordersForUser(String userId) {
        account(userId);
        return orders.values().stream()
                .filter(order -> order.userId().equals(userId))
                .sorted(Comparator.comparing(TicketOrder::createdAt).reversed())
                .toList();
    }

    public synchronized List<TicketOrder> allOrders() {
        return orders.values().stream()
                .sorted(Comparator.comparing(TicketOrder::createdAt).reversed())
                .toList();
    }

    public synchronized TicketOrder order(String orderId) {
        return requireOrder(orderId);
    }

    private TicketOrder requireOrder(String orderId) {
        TicketOrder order = orders.get(orderId);
        if (order == null) {
            throw new DemoException("订单不存在");
        }
        return order;
    }

    // Validation helpers -----------------------------------------------------

    private Account findAccountByUsername(String username) {
        for (Account account : accounts.values()) {
            if (account.username().equalsIgnoreCase(username)) {
                return account;
            }
        }
        return null;
    }

    private boolean passwordMatches(String accountId, String password) {
        return passwords.containsKey(accountId) && passwords.get(accountId).equals(password);
    }

    private void validateMovie(Movie movie) {
        requireText(movie.title(), "请输入影片名称");
        requireText(movie.genre(), "请输入影片类型");
        requireText(movie.director(), "请输入导演");
        if (movie.durationMinutes() <= 0) {
            throw new DemoException("影片时长必须大于0");
        }
        if (movie.rating() < 0 || movie.rating() > 10) {
            throw new DemoException("评分应在0到10之间");
        }
    }

    private void validateHall(CinemaHall hall) {
        requireText(hall.name(), "请输入影厅名称");
        requireText(hall.specs(), "请输入影厅规格");
        if (hall.rows() <= 0 || hall.cols() <= 0) {
            throw new DemoException("影厅行列数必须大于0");
        }
    }

    private void validateUsername(String username) {
        requireText(username, "请输入用户名");
        if (username.length() < 3 || username.length() > 20) {
            throw new DemoException("用户名长度应为3到20个字符");
        }
        if (!username.matches("[A-Za-z0-9_]+")) {
            throw new DemoException("用户名只能包含字母、数字和下划线");
        }
    }

    private void validatePassword(String password) {
        if (password == null || password.length() < 6 || password.length() > 20) {
            throw new DemoException("密码长度应为6到20位");
        }
    }

    private void validatePhone(String phone) {
        if (phone == null || !phone.matches("1\\d{10}")) {
            throw new DemoException("请输入11位手机号");
        }
    }

    private void validateDisplayName(String displayName) {
        requireText(displayName, "请输入姓名");
        if (displayName.trim().length() < 2 || displayName.trim().length() > 20) {
            throw new DemoException("姓名长度应为2到20个字符");
        }
    }

    private void requireText(String value, String message) {
        if (value == null || value.isBlank()) {
            throw new DemoException(message);
        }
    }

    private static String seatKey(int row, int col) {
        return String.format(Locale.ROOT, "%c%d", 'A' + row, col + 1);
    }
}
