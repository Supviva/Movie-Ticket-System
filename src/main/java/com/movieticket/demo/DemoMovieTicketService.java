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
        movies.put("M007", new Movie("M007", "星际旅人", "科幻", "陈默", "陆远、林晓",
                128, "IMAX", "2026-09-18", "人类首艘深空移民船在途中收到来自未知文明的应答信号，一次返航与抵达的抉择就此展开。",
                8.9, 0));
        movies.put("M008", new Movie("M008", "雨夜追凶", "悬疑", "周野", "秦声、白露",
                112, "2D", "2026-09-21", "暴雨封锁的旧城区接连发生失踪案，刑警必须在雨停之前找出藏在人群中的真凶。",
                8.6, 1));
        movies.put("M009", new Movie("M009", "夏日蝉鸣", "青春", "林夏", "唐果、苏屿",
                101, "2D", "2026-09-24", "毕业前的最后一个夏天，五个少年用一场公路旅行告别青春，也重新找到各自的方向。",
                8.4, 2));
        movies.put("M010", new Movie("M010", "风起长安", "古装", "顾青", "萧然、沈雪",
                135, "IMAX", "2026-09-27", "长安城一夜之间风起云涌，一名失忆剑客卷入朝堂与江湖的漩涡。",
                8.7, 3));
        movies.put("M011", new Movie("M011", "深海来客", "科幻", "方舟", "韩森、苏晴",
                119, "3D", "2026-09-30", "马里亚纳海沟深处传来规律敲击声，一支科考队发现了不该存在于地球深处的遗迹。",
                9.0, 4));
        movies.put("M012", new Movie("M012", "最后一班地铁", "剧情", "许文", "江临、赵一",
                97, "2D", "2026-10-02", "凌晨末班地铁上，几名陌生乘客在短暂的车程中交换了彼此不愿启齿的往事。",
                8.5, 5));
        movies.put("M013", new Movie("M013", "暗夜低语", "悬疑", "何深", "周铭、苏叶",
                105, "2D", "2026-09-19", "深夜电台的听众不断接到同一串数字，主持人发现它们指向一桩被掩埋的旧案。",
                8.2, 0));
        movies.put("M014", new Movie("M014", "云端行者", "冒险", "陆一鸣", "高飞、林鹿",
                118, "IMAX", "2026-09-22", "高山救援队接到一次不可能的营救任务，他们必须在暴风雪来临前抵达山顶。",
                8.8, 1));
        movies.put("M015", new Movie("M015", "绿洲计划", "科幻", "李青", "方舟、白露",
                126, "3D", "2026-09-25", "地球进入荒漠时代，一支科考队前往地下绿洲寻找人类最后的净土。",
                8.6, 2));
        movies.put("M016", new Movie("M016", "时光信箱", "爱情", "苏晚", "江临、陈曦",
                98, "2D", "2026-09-28", "一封寄错年份的情书穿越了十年时光，让两个陌生人在时间的缝隙里相遇。",
                8.9, 3));
        movies.put("M017", new Movie("M017", "风之谷的猫", "动画", "蒋未", "配音：唐果、米粒",
                92, "2D", "2026-10-01", "一只会说话的猫带着小女孩穿过风之谷，寻找她遗失的童年记忆。",
                9.1, 4));
        movies.put("M018", new Movie("M018", "极夜列车", "悬疑", "秦声", "韩森、白露",
                109, "2D", "2026-10-05", "极夜将至的边境列车上，六名乘客必须在天亮前找出藏在车厢里的真相。",
                8.7, 5));
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
        movies.put("M023", new Movie("M023", "钢铁侠", "动作", "乔恩·费儒", "小罗伯特·唐尼、格温妮丝·帕特洛",
                126, "2D", "2008-05-02", "军火商托尼·斯塔克被绑架后在山洞中打造出第一套战甲，从此踏上守护世界的道路。",
                8.8, 4));
        movies.put("M024", new Movie("M024", "指环王3：王者无敌", "奇幻", "彼得·杰克逊", "伊利亚·伍德、维果·莫腾森",
                201, "IMAX", "2004-03-12", "中土世界的最后一战打响，佛罗多带着魔戒逼近末日火山，人类的命运悬于一线。",
                9.5, 5));
        movies.put("M025", new Movie("M025", "加勒比海盗", "动作", "戈尔·维宾斯基", "约翰尼·德普、奥兰多·布鲁姆",
                143, "2D", "2003-07-09", "杰克船长为夺回心爱的黑珍珠号，卷入一场关于诅咒金币与海盗宿命的冒险。",
                8.9, 0));
        movies.put("M026", new Movie("M026", "哈利·波特与魔法石", "奇幻", "克里斯·哥伦布", "丹尼尔·雷德克里夫、艾玛·沃特森",
                152, "IMAX", "2001-11-16", "十一岁生日那天，哈利得知自己是一名巫师，由此踏入霍格沃茨魔法学校的大门。",
                9.2, 1));
        movies.put("M027", new Movie("M027", "星际穿越", "科幻", "克里斯托弗·诺兰", "马修·麦康纳、安妮·海瑟薇",
                169, "IMAX", "2014-11-12", "地球濒临枯竭，宇航员穿越虫洞为人类寻找新家园，而时间在相对论中悄然流逝。",
                9.2, 2));
        movies.put("M028", new Movie("M028", "蝙蝠侠：黑暗骑士", "动作", "克里斯托弗·诺兰", "克里斯蒂安·贝尔、希斯·莱杰",
                152, "2D", "2008-07-18", "小丑以混乱为信仰向哥谭宣战，蝙蝠侠被迫在正义与底线之间做出抉择。",
                9.3, 3));
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
        movies.put("M032", new Movie("M032", "蜘蛛侠4：崭新的一天", "动作", "德斯汀·克里顿", "汤姆·霍兰德、赞达亚",
                142, "IMAX", "2027-07-30", "被世人遗忘身份的彼得·帕克从零开始，在纽约的街头重新定义什么才叫英雄。",
                9.2, 1));
        movies.put("M033", new Movie("M033", "神奇四侠：第一步", "科幻", "马特·沙克曼", "佩德罗·帕斯卡、凡妮莎·柯比",
                138, "IMAX", "2027-02-13", "里德·理查兹率领家人踏上宇宙射线实验之旅，归来时他们拥有了超能力，也背负了守护地球的责任。",
                8.9, 2));
        movies.put("M034", new Movie("M034", "碟中谍8：最终清算", "动作", "克里斯托弗·麦奎里", "汤姆·克鲁斯、海莉·阿特维尔",
                164, "IMAX", "2027-05-21", "智体即将全面接管全球系统，伊森·亨特必须在最后一役中做出比生命更重的抉择。",
                9.3, 3));
        movies.put("M035", new Movie("M035", "沙丘3：救世主", "科幻", "丹尼斯·维伦纽瓦", "提莫西·查拉梅、赞达亚",
                167, "IMAX", "2027-03-19", "保罗·厄崔迪登上皇位后，预言中的圣战席卷宇宙，他必须在权力与命运之间找到出路。",
                9.1, 4));
        movies.put("M036", new Movie("M036", "侏罗纪世界4：重生", "冒险", "加里斯·爱德华斯", "斯嘉丽·约翰逊、马赫沙拉·阿里",
                133, "3D", "2027-07-02", "恐龙族群在赤道孤岛上重建生态，一支科考队的到访让人类与巨兽的平衡再次崩塌。",
                8.8, 5));
    }

    private void seedHalls() {
        halls.put("H01", new CinemaHall("H01", "1号杜比厅", "杜比全景声", 8, 12));
        halls.put("H02", new CinemaHall("H02", "2号激光厅", "激光放映", 6, 10));
        halls.put("H03", new CinemaHall("H03", "3号IMAX厅", "IMAX", 9, 14));
    }

    private void seedSessions() {
        // 9 月 11 日（第 1 天）
        addSessionRow("M001", "H01", 1, 14, 30, 4600);
        addSessionRow("M003", "H02", 1, 19, 0, 3900);
        // 9 月 12 日（第 2 天）
        addSessionRow("M002", "H03", 2, 15, 0, 6200);
        addSessionRow("M004", "H01", 2, 18, 45, 4500);
        // 9 月 13 日（第 3 天）
        addSessionRow("M005", "H02", 3, 16, 0, 4100);
        addSessionRow("M006", "H03", 3, 14, 0, 4800);
        // 9 月 14 日（第 4 天）
        addSessionRow("M001", "H02", 4, 20, 10, 4200);
        addSessionRow("M003", "H01", 4, 11, 20, 3900);
        // 9 月 15 日（第 5 天）
        addSessionRow("M002", "H01", 5, 13, 30, 6200);
        addSessionRow("M004", "H03", 5, 19, 30, 5800);
        // 9 月 16 日（第 6 天）
        addSessionRow("M005", "H03", 6, 10, 40, 5300);
        addSessionRow("M006", "H01", 6, 14, 0, 4800);
        // 9 月 17 日（第 7 天）
        addSessionRow("M001", "H03", 7, 15, 20, 4600);
        addSessionRow("M003", "H02", 7, 17, 10, 3900);
        // 9 月 18 日（第 8 天）
        addSessionRow("M002", "H02", 8, 11, 0, 6200);
        addSessionRow("M004", "H01", 8, 18, 45, 4500);
        // 9 月 19 日（第 9 天）
        addSessionRow("M005", "H01", 9, 14, 0, 4100);
        addSessionRow("M006", "H03", 9, 19, 30, 4800);
        // 9 月 20 日（第 10 天）
        addSessionRow("M001", "H01", 10, 10, 30, 4600);
        addSessionRow("M002", "H02", 10, 20, 0, 6200);
        addSessionRow("M003", "H03", 10, 16, 20, 3900);
        // 新上映虚拟影片的演示场次
        addSessionRow("M007", "H03", 2, 10, 30, 6200);
        addSessionRow("M008", "H01", 3, 20, 0, 4300);
        addSessionRow("M009", "H02", 4, 15, 10, 3800);
        addSessionRow("M010", "H03", 5, 11, 40, 6500);
        addSessionRow("M011", "H02", 6, 18, 20, 5200);
        addSessionRow("M012", "H01", 7, 21, 50, 3600);
        addSessionRow("M013", "H02", 3, 14, 0, 4000);
        addSessionRow("M014", "H03", 4, 19, 30, 6800);
        addSessionRow("M015", "H02", 5, 16, 0, 5000);
        addSessionRow("M016", "H01", 6, 20, 0, 4200);
        addSessionRow("M017", "H02", 7, 11, 0, 4100);
        addSessionRow("M018", "H03", 8, 22, 0, 5400);
        // 即将上映大片的预售场次（首页「即将上映」分区）
        addSessionRow("M029", "H03", 9, 10, 0, 7800);
        addSessionRow("M029", "H03", 12, 19, 30, 7800);
        addSessionRow("M030", "H03", 13, 14, 0, 8200);
        addSessionRow("M031", "H01", 10, 16, 30, 8900);
        addSessionRow("M031", "H03", 14, 20, 0, 8900);
        addSessionRow("M032", "H01", 11, 19, 0, 7200);
        addSessionRow("M033", "H02", 12, 15, 20, 6800);
        addSessionRow("M034", "H03", 13, 21, 0, 7500);
        addSessionRow("M035", "H03", 14, 13, 0, 7600);
        addSessionRow("M036", "H01", 15, 18, 40, 6900);
        markSeedsSold();
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
