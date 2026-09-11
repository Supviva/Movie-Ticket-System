package com.movieticket.ui.screen;

import com.movieticket.demo.DemoException;
import com.movieticket.demo.DemoMovieTicketService;
import com.movieticket.model.Account;
import com.movieticket.model.CartItem;
import com.movieticket.model.CinemaHall;
import com.movieticket.model.MembershipLevel;
import com.movieticket.model.MembershipStats;
import com.movieticket.model.Money;
import com.movieticket.model.Movie;
import com.movieticket.model.SeatPlan;
import com.movieticket.model.ShowSession;
import com.movieticket.model.TicketOrder;
import com.movieticket.model.TicketOrderItem;
import com.movieticket.ui.component.PosterPanel;
import com.movieticket.ui.component.NavIcon;
import com.movieticket.ui.component.CircleBadge;
import com.movieticket.ui.component.HeroBannerPanel;
import com.movieticket.ui.component.ImageBadge;
import com.movieticket.ui.component.MovieCardPanel;
import com.movieticket.ui.component.SeatMapPanel;
import com.movieticket.ui.component.Ui;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextField;
import javax.swing.SwingConstants;
import javax.swing.border.EmptyBorder;
import java.awt.BorderLayout;
import java.awt.CardLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Container;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.GridLayout;
import java.awt.RenderingHints;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.StringJoiner;
import java.util.function.Consumer;

public final class CustomerPanel extends JPanel {

    private static final DateTimeFormatter EXPIRY = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    /** 等级主题色的浅色底，用于图标圆底（在深色主题下保持可辨识）。 */
    private static Color levelTint(Color color) {
        return new Color(color.getRed(), color.getGreen(), color.getBlue(), 46);
    }

    private final DemoMovieTicketService service;
    private final Runnable onLogout;
    private Account account;
    private Movie currentMovie;

    private final CardLayout pageLayout = new CardLayout();
    private final JPanel pageStack = new JPanel(pageLayout);
    private final JTextField headerSearch = new JTextField();
    private final JLabel cartText = Ui.label("购物车", Ui.INK_SOFT);

    private final MovieBrowseView browseView;
    private final MovieDetailView detailView;
    private final SeatView seatView;
    private final CartView cartView;
    private final CheckoutView checkoutView;
    private final MembershipView membershipView;
    private final OrdersView ordersView;

    public CustomerPanel(DemoMovieTicketService service, Account account, Runnable onLogout) {
        this.service = service;
        this.account = account;
        this.onLogout = onLogout;
        setLayout(new BorderLayout());
        setBackground(Ui.CANVAS);

        browseView = new MovieBrowseView();
        detailView = new MovieDetailView();
        seatView = new SeatView();
        cartView = new CartView();
        checkoutView = new CheckoutView();
        membershipView = new MembershipView();
        ordersView = new OrdersView();

        pageStack.setOpaque(false);
        pageStack.add(browseView, "browse");
        pageStack.add(detailView, "detail");
        pageStack.add(seatView, "seat");
        pageStack.add(cartView, "cart");
        pageStack.add(checkoutView, "checkout");
        pageStack.add(membershipView, "membership");
        pageStack.add(ordersView, "orders");

        add(createHeader(), BorderLayout.NORTH);
        add(pageStack, BorderLayout.CENTER);
        refreshAccount();
        browseView.refresh("");
        selectNav(Nav.HOME);
    }

    /** 顶栏导航项，顺序与 {@link #navItems} 一致，用于驱动选中高亮。 */
    private enum Nav { HOME, ORDERS, MEMBERSHIP, CART, LOGOUT }

    private final List<JPanel> navItems = new ArrayList<>();
    private final List<NavIcon> navIcons = new ArrayList<>();
    private final List<JLabel> navLabels = new ArrayList<>();

    private JPanel createHeader() {
        JPanel header = Ui.panel(Ui.SURFACE);
        header.setPreferredSize(new Dimension(0, 74));
        header.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(0, 0, 1, 0, Ui.LINE),
                new EmptyBorder(0, 26, 0, 26)));
        header.setLayout(new BorderLayout());

        JPanel brand = Ui.panel(Ui.SURFACE);
        brand.setLayout(new BoxLayout(brand, BoxLayout.X_AXIS));
        NavIcon logoMark = new NavIcon(NavIcon.Kind.FILM, 26, Ui.ORANGE);
        logoMark.setBorder(new EmptyBorder(0, 0, 0, 10));
        brand.add(logoMark);
        JLabel title = Ui.heading("光影票务", 22);
        title.setForeground(Ui.LINK);
        brand.add(title);
        header.add(brand, BorderLayout.WEST);

        JPanel center = Ui.panel(Ui.SURFACE);
        center.setLayout(new FlowLayout(FlowLayout.CENTER, 0, 15));
        center.add(searchBar());
        header.add(center, BorderLayout.CENTER);

        JPanel nav = Ui.panel(Ui.SURFACE);
        nav.setLayout(new FlowLayout(FlowLayout.RIGHT, 22, 12));
        nav.add(navItem(NavIcon.Kind.FILM, "影片", Nav.HOME, this::showHome));
        nav.add(navItem(NavIcon.Kind.TICKET, "我的订单", Nav.ORDERS, this::showOrders));
        nav.add(navItem(NavIcon.Kind.STAR, "我的会员", Nav.MEMBERSHIP, this::showMembership));
        nav.add(cartItem());
        nav.add(navItem(NavIcon.Kind.USER, "退出", Nav.LOGOUT, onLogout));
        header.add(nav, BorderLayout.EAST);
        return header;
    }

    private JPanel navItem(NavIcon.Kind icon, String text, Nav nav, Runnable action) {
        JPanel item = new JPanel();
        item.setOpaque(false);
        item.setLayout(new BoxLayout(item, BoxLayout.Y_AXIS));
        item.setBorder(new EmptyBorder(6, 10, 6, 10));

        NavIcon glyph = new NavIcon(icon, 20, Ui.INK_SOFT);
        glyph.setAlignmentX(Component.CENTER_ALIGNMENT);
        item.add(glyph);
        item.add(Box.createVerticalStrut(3));
        JLabel label = Ui.label(text, Ui.INK_SOFT);
        label.setFont(Ui.font(Font.PLAIN, 12));
        label.setAlignmentX(Component.CENTER_ALIGNMENT);
        item.add(label);

        int index = navItems.size();
        navItems.add(item);
        navIcons.add(glyph);
        navLabels.add(label);

        item.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        installNavHover(index);
        item.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent event) {
                action.run();
            }
        });
        return item;
    }

    private JPanel cartItem() {
        JPanel item = new JPanel();
        item.setOpaque(false);
        item.setLayout(new BoxLayout(item, BoxLayout.Y_AXIS));
        item.setBorder(new EmptyBorder(6, 10, 6, 10));

        NavIcon glyph = new NavIcon(NavIcon.Kind.CART, 20, Ui.INK_SOFT);
        glyph.setAlignmentX(Component.CENTER_ALIGNMENT);
        item.add(glyph);
        item.add(Box.createVerticalStrut(3));
        cartText.setFont(Ui.font(Font.PLAIN, 12));
        cartText.setAlignmentX(Component.CENTER_ALIGNMENT);
        item.add(cartText);

        int index = navItems.size();
        navItems.add(item);
        navIcons.add(glyph);
        navLabels.add(cartText);

        item.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        installNavHover(index);
        item.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent event) {
                showCart();
            }
        });
        return item;
    }

    /**
     * 给导航项加「悬停变亮、选中变主色 + 浅橙底」的反馈。
     *
     * <p>悬停状态独立记录，避免鼠标移出时把选中态也一起清掉。</p>
     */
    private void installNavHover(int index) {
        JPanel item = navItems.get(index);
        item.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseEntered(MouseEvent event) {
                if (index != selectedNav) {
                    paintNav(index, true, false);
                }
            }

            @Override
            public void mouseExited(MouseEvent event) {
                if (index != selectedNav) {
                    paintNav(index, false, false);
                }
            }
        });
    }

    private int selectedNav = -1;

    /**
     * 切换顶栏选中项，并重绘文字、图标与底色。
     *
     * @param nav 目标导航项
     */
    private void selectNav(Nav nav) {
        int index = nav.ordinal();
        if (index >= navItems.size()) {
            return;
        }
        if (selectedNav >= 0 && selectedNav < navItems.size()) {
            paintNav(selectedNav, false, false);
        }
        selectedNav = index;
        paintNav(index, false, true);
    }

    /**
     * 重绘单个导航项。
     *
     * @param index    导航项索引
     * @param hovered  是否处于悬停
     * @param selected 是否处于选中
     */
    private void paintNav(int index, boolean hovered, boolean selected) {
        Color foreground;
        if (selected) {
            foreground = Ui.PRIMARY;
        } else if (hovered) {
            foreground = Ui.INK;
        } else {
            foreground = Ui.INK_SOFT;
        }
        navLabels.get(index).setForeground(foreground);
        navIcons.get(index).setColor(foreground);

        JPanel item = navItems.get(index);
        Color background = selected ? Ui.PRIMARY_SOFT
                : hovered ? new Color(0xFF, 0xFF, 0xFF, 14) : null;
        if (background == null) {
            item.setOpaque(false);
        } else {
            item.setOpaque(true);
            item.setBackground(background);
        }
        item.setBorder(new EmptyBorder(6, 10, 6, 10));
        item.repaint();
    }

    private JComponent searchBar() {
        Ui.RoundedPanel bar = Ui.card(new Color(0x2A, 0x2A, 0x2A), 19);
        bar.setLayout(new BorderLayout());
        bar.setBorder(new EmptyBorder(2, 14, 2, 5));
        bar.setPreferredSize(new Dimension(380, 40));
        bar.setMaximumSize(new Dimension(380, 40));

        headerSearch.setOpaque(false);
        headerSearch.setBorder(BorderFactory.createEmptyBorder(5, 2, 5, 2));
        headerSearch.setForeground(Ui.INK);
        headerSearch.setCaretColor(Ui.INK);
        headerSearch.setFont(Ui.FONT);
        headerSearch.setToolTipText("搜索影片名称、类型或导演");
        headerSearch.addActionListener(event -> showHome());
        bar.add(headerSearch, BorderLayout.CENTER);

        JButton searchButton = new JButton();
        searchButton.setPreferredSize(new Dimension(32, 32));
        searchButton.setOpaque(false);
        searchButton.setContentAreaFilled(false);
        searchButton.setBorderPainted(false);
        searchButton.setFocusPainted(false);
        searchButton.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        searchButton.setIcon(new javax.swing.Icon() {
            @Override
            public void paintIcon(Component c, Graphics g, int x, int y) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(Ui.LINK);
                g2.fillOval(x, y, 30, 30);
                g2.setColor(Color.WHITE);
                g2.setStroke(new java.awt.BasicStroke(2f, java.awt.BasicStroke.CAP_ROUND, java.awt.BasicStroke.JOIN_ROUND));
                g2.drawOval(x + 8, y + 8, 9, 9);
                g2.drawLine(x + 15, y + 15, x + 22, y + 22);
                g2.dispose();
            }

            @Override
            public int getIconWidth() {
                return 30;
            }

            @Override
            public int getIconHeight() {
                return 30;
            }
        });
        searchButton.addActionListener(event -> showHome());
        JPanel buttonWrap = Ui.panel(Ui.SURFACE);
        buttonWrap.setOpaque(false);
        buttonWrap.add(searchButton);
        bar.add(buttonWrap, BorderLayout.EAST);
        return bar;
    }

    private void refreshAccount() {
        account = service.account(account.id());
        cartText.setText("购物车（" + service.cartCount(account.id()) + "）");
    }

    private void showHome() {
        selectNav(Nav.HOME);
        browseView.refresh(browseView.keyword());
        pageLayout.show(pageStack, "browse");
    }

    private void showMovie(Movie movie) {
        currentMovie = movie;
        detailView.refresh(movie);
        pageLayout.show(pageStack, "detail");
    }

    private void showSeat(ShowSession session) {
        seatView.refresh(session);
        pageLayout.show(pageStack, "seat");
    }

    private void showCart() {
        selectNav(Nav.CART);
        refreshAccount();
        cartView.refresh();
        pageLayout.show(pageStack, "cart");
    }

    private void showCheckout(TicketOrder order) {
        checkoutView.refresh(order);
        pageLayout.show(pageStack, "checkout");
    }

    private void showMembership() {
        selectNav(Nav.MEMBERSHIP);
        refreshAccount();
        membershipView.refresh();
        pageLayout.show(pageStack, "membership");
    }

    private void showOrders() {
        selectNav(Nav.ORDERS);
        refreshAccount();
        ordersView.refresh();
        pageLayout.show(pageStack, "orders");
    }

    // ---- 测试钩子：把私有的页面切换暴露给同包测试，避免用反射 ----

    void showHomeForTest() {
        showHome();
    }

    void showOrdersForTest() {
        showOrders();
    }

    void showMembershipForTest() {
        showMembership();
    }

    void showCartForTest() {
        showCart();
    }

    /** 直接渲染一条订单行的操作区，供测试断言「去支付」入口是否存在。 */
    JPanel orderRowForTest(TicketOrder order) {
        return orderRow(order);
    }

    /** 当前登录账号（下单测试用）。 */
    Account currentAccountForTest() {
        return account;
    }

    /** 直接进入选座页，供测试断言座位图交互。 */
    void showSeatForTest(ShowSession session) {
        showSeat(session);
    }

    private static String joinSeats(Iterable<String> seats) {
        StringJoiner joiner = new StringJoiner("、");
        seats.forEach(joiner::add);
        return joiner.toString();
    }

    private static String sessionTime(ShowSession session) {
        return session.startTime() + " 至 " + session.endTime();
    }

    private static JLabel statusLabel(String status) {
        Color color = switch (status) {
            case "UNPAID" -> Ui.WARNING;
            case "PAID" -> Ui.SUCCESS;
            case "CANCELED" -> Ui.MUTED;
            default -> Ui.INK;
        };
        JLabel label = Ui.label(Ui.statusText(status), color);
        label.setFont(Ui.font(Font.BOLD, 13));
        return label;
    }

    private final class MovieBrowseView extends JPanel {
        /** 类型筛选选项，首个为「全部」——即不做过滤。 */
        private static final String[] GENRES = {
                "全部", "爱情", "科幻", "悬疑", "剧情", "动画", "喜剧", "动作", "奇幻"};

        private final JPanel content = new JPanel();
        private final List<SectionGrid> grids = new ArrayList<>();
        private HeroBannerPanel hero;
        private String activeGenre = GENRES[0];

        private MovieBrowseView() {
            setLayout(new BorderLayout());
            setBackground(Ui.CANVAS);
            content.setLayout(new BoxLayout(content, BoxLayout.Y_AXIS));
            content.setBackground(Ui.CANVAS);
            content.setBorder(new EmptyBorder(20, 26, 36, 26));
            JScrollPane scroll = Ui.scrollNoBorder(content);
            scroll.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
            scroll.getVerticalScrollBar().setUnitIncrement(18);
            content.addComponentListener(new ComponentAdapter() {
                @Override
                public void componentResized(ComponentEvent event) {
                    relayoutGrids();
                }
            });
            add(scroll, BorderLayout.CENTER);
        }

        private String keyword() {
            return headerSearch.getText();
        }

        private void refresh(String keyword) {
            hero = null;
            content.removeAll();
            grids.clear();
            String query = keyword == null ? "" : keyword.trim();
            if (query.isEmpty()) {
                content.add(heroSection());
                content.add(Box.createVerticalStrut(22));
                content.add(filterBar());
                content.add(Box.createVerticalStrut(6));
                addSections();
            } else {
                List<Movie> movies = service.searchMovies(query);
                if (movies.isEmpty()) {
                    content.add(emptyPanel("没有找到匹配的影片"));
                } else {
                    content.add(section("搜索结果", movies, null));
                }
            }
            content.revalidate();
            relayoutGrids();
            content.repaint();
        }

        /**
         * 渲染首页三个分区：热映 / 即将上映 / 经典重映。
         *
         * <p>按年代划分三个区间，保证内容互不重复——早期用 slice 切分时
         * 因数组越界，后两段实际渲染了同一批影片。</p>
         *
         * <p>「即将上映」取次年及以后的待映大片（如复仇者联盟5、阿凡达3），
         * 带「预售」角标；「经典重映」取 15 年前的片子（如泰坦尼克号、肖申克）。</p>
         */
        private void addSections() {
            List<Movie> all = service.movies();
            int currentYear = LocalDate.now().getYear();
            int classicBefore = currentYear - 15;

            List<Movie> nowShowing = filterByGenre(all).stream()
                    .filter(movie -> releaseYear(movie) <= currentYear)
                    .filter(movie -> releaseYear(movie) >= classicBefore)
                    .toList();
            if (!nowShowing.isEmpty()) {
                content.add(section("正在热映", nowShowing, "gift"));
                content.add(Box.createVerticalStrut(28));
            }

            List<Movie> upcoming = filterByGenre(all).stream()
                    .filter(movie -> releaseYear(movie) > currentYear)
                    .toList();
            if (!upcoming.isEmpty()) {
                content.add(section("即将上映", upcoming, "presale"));
                content.add(Box.createVerticalStrut(28));
            }

            List<Movie> classics = filterByGenre(all).stream()
                    .filter(movie -> releaseYear(movie) < classicBefore)
                    .toList();
            if (!classics.isEmpty()) {
                content.add(section("经典重映", classics, null));
                content.add(Box.createVerticalStrut(28));
            }
            content.add(snackSection());
        }

        private List<Movie> filterByGenre(List<Movie> movies) {
            if (GENRES[0].equals(activeGenre)) {
                return movies;
            }
            return movies.stream().filter(movie -> movie.genre().contains(activeGenre)).toList();
        }

        private static int releaseYear(Movie movie) {
            String date = movie.releaseDate();
            try {
                return date != null && date.length() >= 4 ? Integer.parseInt(date.substring(0, 4)) : 0;
            } catch (NumberFormatException ex) {
                return 0;
            }
        }

        /** 主视觉横幅 + 类型筛选条：首页最上方两个区块。 */
        private JPanel heroSection() {
            List<Movie> featured = service.movies();
            if (featured.isEmpty()) {
                return emptyPanel("暂无影片");
            }
            List<Movie> promoted = featured.size() > 6 ? featured.subList(0, 6) : featured;
            hero = new HeroBannerPanel(promoted, this::buyNow, this::openMovie, this::relayoutGrids);
            hero.setAlignmentX(Component.LEFT_ALIGNMENT);
            return hero;
        }

        private JPanel filterBar() {
            JPanel row = new JPanel(new FlowLayout(FlowLayout.LEFT, 9, 0));
            row.setOpaque(false);
            row.setAlignmentX(Component.LEFT_ALIGNMENT);
            row.setMaximumSize(new Dimension(Integer.MAX_VALUE, 40));
            for (String genre : GENRES) {
                row.add(genreChip(genre));
            }
            return row;
        }

        private JComponent genreChip(String genre) {
            boolean on = genre.equals(activeGenre);
            JButton chip = new JButton(genre) {
                @Override
                protected void paintComponent(Graphics graphics) {
                    Graphics2D g2 = (Graphics2D) graphics.create();
                    g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                    boolean hot = on || getModel().isRollover();
                    g2.setColor(on ? Ui.PRIMARY : Ui.SURFACE);
                    g2.fillRoundRect(0, 0, getWidth(), getHeight(), 40, 40);
                    g2.setColor(on ? Ui.PRIMARY : (hot ? new Color(0x4A, 0x4A, 0x4A) : Ui.LINE));
                    g2.drawRoundRect(0, 0, getWidth() - 1, getHeight() - 1, 40, 40);
                    g2.dispose();
                    super.paintComponent(graphics);
                }
            };
            chip.setOpaque(false);
            chip.setContentAreaFilled(false);
            chip.setBorderPainted(false);
            chip.setFocusPainted(false);
            chip.setForeground(on ? Ui.CANVAS : Ui.INK_SOFT);
            chip.setFont(Ui.font(on ? Font.BOLD : Font.PLAIN, 13));
            chip.setBorder(new EmptyBorder(7, 16, 7, 16));
            chip.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
            chip.addActionListener(event -> {
                if (!genre.equals(activeGenre)) {
                    activeGenre = genre;
                    refresh(keyword());
                }
            });
            return chip;
        }

        /** 卡片上的「选座购票」：直接跳到该影片最近一场的选座页。 */
        private void buyNow(Movie movie) {
            List<ShowSession> sessions = service.sessionsForMovie(movie.id());
            if (sessions.isEmpty()) {
                Ui.info(this, "暂无可售场次", "《" + movie.title() + "》暂时没有排期");
                return;
            }
            showSeat(sessions.getFirst());
        }

        private void openMovie(Movie movie) {
            showMovie(movie);
        }

        private void relayoutGrids() {
            int width = content.getWidth();
            if (width <= 0) {
                width = 1240;
            }
            int available = Math.max(220, width - 52);
            int columns = Math.max(1, available / 205);
            for (SectionGrid holder : grids) {
                holder.grid.removeAll();
                holder.grid.setLayout(new GridLayout(0, columns, 16, 20));
                for (Component card : holder.cards) {
                    holder.grid.add(card);
                }
                holder.grid.revalidate();
                holder.grid.repaint();
            }
            content.revalidate();
            content.repaint();
        }

        private JPanel section(String title, List<Movie> movies, String badgeMode) {
            JPanel section = new JPanel();
            section.setOpaque(false);
            section.setLayout(new BoxLayout(section, BoxLayout.Y_AXIS));
            section.setAlignmentX(Component.LEFT_ALIGNMENT);

            JLabel titleLabel = Ui.heading(title + "（" + movies.size() + "）", 19);
            titleLabel.setHorizontalAlignment(SwingConstants.LEFT);
            titleLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
            section.add(titleLabel);
            section.add(Box.createVerticalStrut(14));

            List<Component> cards = new ArrayList<>();
            int index = 0;
            for (Movie movie : movies) {
                String badge = "presale".equals(badgeMode) ? "presale"
                        : ("gift".equals(badgeMode) && index < 3 ? "gift" : null);
                MovieCardPanel card = new MovieCardPanel(movie, badge,
                        () -> showMovie(movie), this::buyNow);
                card.setPreferredSize(new Dimension(188, 358));
                cards.add(card);
                index++;
            }
            JPanel grid = new JPanel();
            grid.setOpaque(false);
            grids.add(new SectionGrid(grid, cards));
            section.add(grid);
            return section;
        }

        private JPanel snackSection() {
            JPanel section = new JPanel();
            section.setOpaque(false);
            section.setLayout(new BoxLayout(section, BoxLayout.Y_AXIS));
            section.setAlignmentX(Component.LEFT_ALIGNMENT);

            JLabel titleLabel = Ui.heading("小食", 19);
            titleLabel.setHorizontalAlignment(SwingConstants.LEFT);
            titleLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
            section.add(titleLabel);
            section.add(Box.createVerticalStrut(14));

            List<Component> cards = new ArrayList<>();
            cards.add(snackCard("popcorn.png", NavIcon.Kind.POPCORN, Ui.ORANGE, "爆米花", "18.00", "大桶 · 约 120g"));
            cards.add(snackCard("cola.png", NavIcon.Kind.COLA, Ui.LINK, "可乐", "12.00", "中杯 · 500ml"));
            cards.add(snackCard("fries.png", NavIcon.Kind.FRIES, Ui.GOLD, "薯条", "16.00", "大份 · 约 150g"));
            cards.add(snackCard("ice_cream.png", NavIcon.Kind.ICE_CREAM, Ui.PURPLE, "冰淇淋", "15.00", "单球 · 90g"));
            cards.add(snackCard("hotdog.png", NavIcon.Kind.HOTDOG, Ui.PRIMARY, "热狗", "20.00", "经典 · 含面包"));

            JPanel grid = new JPanel();
            grid.setOpaque(false);
            grids.add(new SectionGrid(grid, cards));
            section.add(grid);
            return section;
        }

        /**
         * 小食卖品位。纯展示，不提供购买入口。
         *
         * <p>演示数据里小食并没有进入购物车/订单的业务流程，之前挂一个「购买」
         * 按钮点了只弹提示，属于伪交互。这里改为只展示规格文案，不误导用户。</p>
         */
        private JPanel snackCard(String image, NavIcon.Kind icon, Color color, String name,
                                 String price, String spec) {
            Ui.RoundedPanel card = Ui.card(Ui.SURFACE, 12);
            card.setLayout(new BoxLayout(card, BoxLayout.Y_AXIS));
            card.setBorder(new EmptyBorder(16, 20, 16, 20));
            card.setPreferredSize(new Dimension(150, 190));

            ImageBadge badge = new ImageBadge("/snacks/" + image, color, icon, 64);
            badge.setAlignmentX(Component.CENTER_ALIGNMENT);
            card.add(badge);
            card.add(Box.createVerticalStrut(12));

            JLabel nameLabel = Ui.label(name, Ui.INK);
            nameLabel.setFont(Ui.font(Font.BOLD, 14));
            nameLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
            card.add(nameLabel);
            card.add(Box.createVerticalStrut(4));

            JLabel priceLabel = Ui.label("¥" + price, Ui.ORANGE);
            priceLabel.setFont(Ui.font(Font.BOLD, 13));
            priceLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
            card.add(priceLabel);
            card.add(Box.createVerticalGlue());

            JLabel specLabel = Ui.label(spec, Ui.MUTED);
            specLabel.setFont(Ui.font(Font.PLAIN, 11));
            specLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
            card.add(specLabel);
            return card;
        }

        private static final class SectionGrid {
            private final JPanel grid;
            private final List<Component> cards;

            private SectionGrid(JPanel grid, List<Component> cards) {
                this.grid = grid;
                this.cards = cards;
            }
        }
    }

    private final class MovieDetailView extends JPanel {
        private final JPanel sessions = new JPanel();
        private final JLabel titleLabel = Ui.heading("", 26);
        private final JLabel genreLabel = Ui.subtle("");
        private final JLabel ratingLabel = Ui.label("", Ui.PRIMARY);
        private final JLabel directorLabel = Ui.label("");
        private final JLabel castLabel = Ui.label("");
        private final JLabel releaseLabel = Ui.subtle("");
        private final JLabel synopsisLabel = Ui.subtle("");
        private final JPanel posterWrap = Ui.panel(Ui.SURFACE);
        private PosterPanel poster;

        private MovieDetailView() {
            setLayout(new BorderLayout());
            setBackground(Ui.CANVAS);

            JPanel top = Ui.panel(Ui.SURFACE);
            top.setLayout(new BorderLayout());
            JButton back = Ui.textButton("返回影片", Ui.MUTED);
            back.addActionListener(event -> showHome());
            top.add(back, BorderLayout.WEST);
            top.setBorder(BorderFactory.createCompoundBorder(
                    BorderFactory.createMatteBorder(0, 0, 1, 0, Ui.LINE),
                    new EmptyBorder(14, 26, 14, 26)));
            add(top, BorderLayout.NORTH);

            JPanel body = new JPanel(new BorderLayout());
            body.setBackground(Ui.CANVAS);

            JPanel summary = Ui.panel(Ui.SURFACE);
            summary.setBorder(new EmptyBorder(28, 28, 20, 28));
            summary.setLayout(new BorderLayout());
            poster = new PosterPanel(movieForPoster(), 220, 320);
            posterWrap.setLayout(new FlowLayout(FlowLayout.LEFT, 0, 0));
            posterWrap.add(poster);
            summary.add(posterWrap, BorderLayout.WEST);

            JPanel meta = new JPanel();
            meta.setOpaque(false);
            meta.setLayout(new BoxLayout(meta, BoxLayout.Y_AXIS));
            meta.add(titleLabel);
            meta.add(Box.createVerticalStrut(8));
            meta.add(genreLabel);
            meta.add(Box.createVerticalStrut(8));
            meta.add(ratingLabel);
            meta.add(Box.createVerticalStrut(20));
            meta.add(metaLine("导演", directorLabel));
            meta.add(Box.createVerticalStrut(10));
            meta.add(metaLine("主演", castLabel));
            meta.add(Box.createVerticalStrut(10));
            meta.add(metaLine("上映", releaseLabel));
            meta.add(Box.createVerticalStrut(20));
            JLabel synopsisTitle = Ui.heading("剧情简介", 16);
            synopsisTitle.setAlignmentX(Component.LEFT_ALIGNMENT);
            meta.add(synopsisTitle);
            meta.add(Box.createVerticalStrut(8));
            synopsisLabel.setFont(Ui.font(Font.PLAIN, 14));
            synopsisLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
            synopsisLabel.setForeground(Ui.INK_SOFT);
            meta.add(synopsisLabel);
            summary.add(meta, BorderLayout.CENTER);
            body.add(summary, BorderLayout.NORTH);

            sessions.setBackground(Ui.CANVAS);
            sessions.setLayout(new BoxLayout(sessions, BoxLayout.Y_AXIS));
            JScrollPane scroll = Ui.scrollNoBorder(sessions);
            body.add(scroll, BorderLayout.CENTER);
            add(body, BorderLayout.CENTER);
        }

        private JPanel metaLine(String name, JLabel value) {
            JPanel row = new JPanel(new BorderLayout());
            row.setOpaque(false);
            row.setMaximumSize(new Dimension(Integer.MAX_VALUE, 26));
            JLabel key = Ui.label(name, Ui.MUTED);
            key.setPreferredSize(new Dimension(70, 24));
            row.add(key, BorderLayout.WEST);
            row.add(value, BorderLayout.CENTER);
            return row;
        }

        private void refresh(Movie movie) {
            titleLabel.setText(movie.title());
            genreLabel.setText(movie.genre() + " · " + movie.format() + " · " + movie.durationText());
            ratingLabel.setText("评分 " + String.format("%.1f", movie.rating()));
            directorLabel.setText(movie.director());
            castLabel.setText(movie.starring());
            releaseLabel.setText("上映日期 " + movie.releaseDate());
            synopsisLabel.setText(movie.synopsis());
            posterWrap.removeAll();
            poster = new PosterPanel(movie, 220, 320);
            posterWrap.add(poster);
            posterWrap.revalidate();
            posterWrap.repaint();

            sessions.removeAll();
            List<ShowSession> rows = service.sessionsForMovie(movie.id());
            if (rows.isEmpty()) {
                JPanel empty = emptyPanel("该影片暂未排片");
                sessions.add(empty);
            } else {
                JLabel section = Ui.heading("场次", 18);
                section.setAlignmentX(Component.LEFT_ALIGNMENT);
                section.setBorder(new EmptyBorder(16, 28, 10, 0));
                sessions.add(section);
                for (ShowSession showSession : rows) {
                    sessions.add(sessionRow(movie, showSession));
                    sessions.add(Box.createVerticalStrut(8));
                }
            }
            sessions.revalidate();
            sessions.repaint();
        }

        private Movie movieForPoster() {
            return new Movie("", "光影票务", "影院", "", "", 0, "", "", "", 0, 0);
        }
    }

    private JPanel sessionRow(Movie movie, ShowSession showSession) {
        Ui.RoundedPanel row = Ui.card();
        row.setMaximumSize(new Dimension(Integer.MAX_VALUE, 108));
        row.setLayout(new BorderLayout());
        row.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(Ui.LINE),
                new EmptyBorder(14, 22, 14, 18)));

        JPanel left = Ui.panel(Ui.SURFACE);
        left.setLayout(new BoxLayout(left, BoxLayout.Y_AXIS));
        left.add(Ui.heading(showSession.startTime(), 17));
        left.add(Box.createVerticalStrut(6));
        left.add(Ui.subtle("预计结束 " + showSession.endTime()));
        row.add(left, BorderLayout.CENTER);

        JPanel right = Ui.panel(Ui.SURFACE);
        right.setLayout(new FlowLayout(FlowLayout.RIGHT, 18, 12));
        JPanel hallInfo = Ui.panel(Ui.SURFACE);
        hallInfo.setLayout(new BoxLayout(hallInfo, BoxLayout.Y_AXIS));
        hallInfo.add(Ui.subtle(service.hall(showSession.hallId()).name()));
        hallInfo.add(Box.createVerticalStrut(4));
        hallInfo.add(Ui.subtle(movie.format() + " · " + Money.format(showSession.priceFen()) + " / 张"));
        right.add(hallInfo);
        JButton select = Ui.primary("选座");
        select.addActionListener(event -> showSeat(showSession));
        right.add(select);
        row.add(right, BorderLayout.EAST);
        return row;
    }

    private final class SeatView extends JPanel {
        private final JPanel mapArea = Ui.panel(Ui.CANVAS);
        private final JLabel movieLabel = Ui.heading("", 18);
        private final JLabel sessionLabel = Ui.subtle("");
        private final JLabel priceLabel = Ui.label("");
        private final JLabel selectionLabel = Ui.label("未选择座位");
        private final JLabel totalLabel = Ui.heading("", 16);
        private final JButton addCart = Ui.primary("加入购物车");
        private final JButton buyNow = Ui.primary("立即购票");
        /** 行内提示的恢复定时器：提示展示 2.5 秒后回到已选座位列表。 */
        private final javax.swing.Timer hintTimer = new javax.swing.Timer(2500, event -> updateSelection());
        private SeatMapPanel seatMap;
        private ShowSession current;

        private SeatView() {
            setLayout(new BorderLayout());
            setBackground(Ui.CANVAS);
            add(createTop(), BorderLayout.NORTH);

            JPanel body = new JPanel(new BorderLayout(18, 0));
            body.setBackground(Ui.CANVAS);
            body.setBorder(new EmptyBorder(20, 26, 26, 26));

            mapArea.setLayout(new FlowLayout(FlowLayout.CENTER, 0, 0));
            // 包滚动容器：后台新增大影厅时座位图超出视口可滚动，不再被裁
            body.add(Ui.scrollNoBorder(mapArea), BorderLayout.CENTER);

            Ui.RoundedPanel side = Ui.card();
            side.setLayout(new BoxLayout(side, BoxLayout.Y_AXIS));
            side.setPreferredSize(new Dimension(300, 0));
            side.setBorder(BorderFactory.createCompoundBorder(
                    BorderFactory.createLineBorder(Ui.LINE),
                    new EmptyBorder(24, 22, 24, 22)));
            side.add(Ui.heading("购票信息", 17));
            side.add(Box.createVerticalStrut(18));
            movieLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
            side.add(movieLabel);
            side.add(Box.createVerticalStrut(7));
            sessionLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
            side.add(sessionLabel);
            side.add(Box.createVerticalStrut(16));
            priceLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
            side.add(priceLabel);
            side.add(Box.createVerticalStrut(14));
            JLabel legendTitle = Ui.heading("图例", 13);
            legendTitle.setAlignmentX(Component.LEFT_ALIGNMENT);
            side.add(legendTitle);
            side.add(Box.createVerticalStrut(8));
            side.add(legend(Ui.SURFACE, "可选"));
            side.add(Box.createVerticalStrut(5));
            side.add(legend(Ui.PRIMARY, "已选"));
            side.add(Box.createVerticalStrut(5));
            side.add(legend(Ui.SOLD, "已售"));
            side.add(Box.createVerticalGlue());
            selectionLabel.setFont(Ui.font(Font.PLAIN, 14));
            selectionLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
            selectionLabel.setBorder(new EmptyBorder(0, 0, 4, 0));
            side.add(selectionLabel);
            totalLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
            totalLabel.setForeground(Ui.PRIMARY);
            totalLabel.setBorder(new EmptyBorder(0, 0, 12, 0));
            side.add(totalLabel);
            addCart.setAlignmentX(Component.LEFT_ALIGNMENT);
            addCart.setMaximumSize(new Dimension(Integer.MAX_VALUE, 44));
            addCart.addActionListener(event -> addSelectedToCart());
            side.add(addCart);
            side.add(Box.createVerticalStrut(10));
            buyNow.setAlignmentX(Component.LEFT_ALIGNMENT);
            buyNow.setMaximumSize(new Dimension(Integer.MAX_VALUE, 44));
            buyNow.setBackground(Ui.INK);
            buyNow.addActionListener(event -> buyNow());
            side.add(buyNow);

            body.add(side, BorderLayout.EAST);
            add(body, BorderLayout.CENTER);
        }

        private JPanel createTop() {
            JPanel top = Ui.panel(Ui.SURFACE);
            top.setBorder(new EmptyBorder(14, 26, 14, 26));
            top.setLayout(new FlowLayout(FlowLayout.LEFT, 10, 0));
            JButton back = Ui.textButton("返回场次", Ui.MUTED);
            back.addActionListener(event -> showMovie(currentMovie));
            top.add(back);
            top.add(Ui.subtle("选择座位"));
            return top;
        }

        private JPanel legend(Color color, String text) {
            JPanel row = new JPanel(new BorderLayout(10, 0));
            row.setOpaque(false);
            row.setMaximumSize(new Dimension(Integer.MAX_VALUE, 24));
            JPanel swatch = new JPanel();
            swatch.setBackground(color);
            swatch.setBorder(BorderFactory.createLineBorder(Ui.LINE));
            swatch.setPreferredSize(new Dimension(26, 18));
            row.add(swatch, BorderLayout.WEST);
            row.add(Ui.label(text, Ui.INK_SOFT), BorderLayout.CENTER);
            return row;
        }

        private void refresh(ShowSession showSession) {
            current = showSession;
            Movie movie = service.movie(showSession.movieId());
            CinemaHall hall = service.hall(showSession.hallId());
            SeatPlan plan = service.seatPlan(showSession.id());
            movieLabel.setText(movie.title());
            sessionLabel.setText(hall.name() + " · " + sessionTime(showSession));
            priceLabel.setText(Money.format(showSession.priceFen()) + " / 张");

            mapArea.removeAll();
            seatMap = new SeatMapPanel(hall, plan.soldSeats(), this::selectionChanged, this::showHint);
            mapArea.add(seatMap);
            updateSelection();
            mapArea.revalidate();
            mapArea.repaint();
        }

        private void selectionChanged() {
            updateSelection();
        }

        /** 行内提示：不打断操作，2.5 秒后自动恢复已选座位列表。 */
        private void showHint(String text) {
            hintTimer.stop();
            selectionLabel.setForeground(Ui.DANGER);
            selectionLabel.setText(text);
            hintTimer.restart();
        }

        private void updateSelection() {
            hintTimer.stop();
            selectionLabel.setForeground(Ui.INK);
            List<String> seats = new ArrayList<>(seatMap == null ? List.of() : seatMap.selectedSeats());
            if (seats.isEmpty()) {
                selectionLabel.setText("未选择座位");
                totalLabel.setText("");
                addCart.setEnabled(false);
                buyNow.setEnabled(false);
            } else {
                selectionLabel.setText("已选 " + seats.size() + " 张 · " + joinSeats(seats));
                totalLabel.setText("合计 " + Money.format(current.priceFen() * seats.size()));
                addCart.setEnabled(true);
                buyNow.setEnabled(true);
            }
        }

        private void addSelectedToCart() {
            try {
                service.addToCart(account.id(), current.id(), seatMap.selectedSeats());
                showCart();
            } catch (DemoException ex) {
                Ui.error(this, "加入购物车失败", ex.getMessage());
                refresh(current);
            }
        }

        private void buyNow() {
            try {
                TicketOrder order = service.buySeatsNow(account.id(), current.id(), seatMap.selectedSeats());
                showCheckout(order);
            } catch (DemoException ex) {
                Ui.error(this, "下单失败", ex.getMessage());
                refresh(current);
            }
        }
    }

    private final class CartView extends JPanel {
        private final JPanel items = new JPanel();
        private final JLabel totalLabel = Ui.label("");
        private final JButton checkout = Ui.primary("结算全部");

        private CartView() {
            setLayout(new BorderLayout());
            setBackground(Ui.CANVAS);
            JPanel toolbar = Ui.panel(Ui.SURFACE);
            toolbar.setBorder(BorderFactory.createMatteBorder(0, 0, 1, 0, Ui.LINE));
            toolbar.setLayout(new BorderLayout());
            JLabel title = Ui.heading("购物车", 22);
            title.setBorder(new EmptyBorder(18, 26, 18, 0));
            toolbar.add(title, BorderLayout.WEST);
            JPanel actions = Ui.panel(Ui.SURFACE);
            actions.setLayout(new FlowLayout(FlowLayout.RIGHT, 10, 16));
            JButton clear = Ui.secondary("清空");
            clear.addActionListener(event -> {
                if (Ui.confirm(this, "清空购物车", "确定清空购物车中的所有项目吗？")) {
                    service.clearCart(account.id());
                    refresh();
                }
            });
            actions.add(totalLabel);
            actions.add(clear);
            actions.add(checkout);
            checkout.addActionListener(event -> {
                try {
                    TicketOrder order = service.checkoutCart(account.id());
                    showCheckout(order);
                } catch (DemoException ex) {
                    Ui.error(this, "结算失败", ex.getMessage());
                }
            });
            toolbar.add(actions, BorderLayout.EAST);
            add(toolbar, BorderLayout.NORTH);

            items.setBackground(Ui.CANVAS);
            items.setLayout(new BoxLayout(items, BoxLayout.Y_AXIS));
            add(Ui.scrollNoBorder(items), BorderLayout.CENTER);
        }

        private void refresh() {
            refreshAccount();
            items.removeAll();
            List<CartItem> cartItems = service.cartItems(account.id());
            int total = 0;
            for (CartItem item : cartItems) {
                items.add(cartRow(item));
                items.add(Box.createVerticalStrut(10));
                total += item.lineTotalFen();
            }
            if (cartItems.isEmpty()) {
                JPanel empty = emptyPanel("购物车为空");
                items.add(empty);
                checkout.setEnabled(false);
            } else {
                checkout.setEnabled(true);
            }
            totalLabel.setText("原价合计 " + Money.format(total));
            totalLabel.setForeground(Ui.INK);
            items.revalidate();
            items.repaint();
        }
    }

    private JPanel cartRow(CartItem item) {
        Ui.RoundedPanel row = Ui.card();
        row.setMaximumSize(new Dimension(Integer.MAX_VALUE, 116));
        row.setLayout(new BorderLayout());
        row.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(Ui.LINE),
                new EmptyBorder(14, 22, 14, 16)));

        JPanel info = Ui.panel(Ui.SURFACE);
        info.setLayout(new BoxLayout(info, BoxLayout.Y_AXIS));
        info.add(Ui.heading(item.movieTitle(), 16));
        info.add(Box.createVerticalStrut(6));
        info.add(Ui.subtle(item.hallName() + " · " + item.startTime() + " 至 " + item.endTime()));
        info.add(Box.createVerticalStrut(6));
        info.add(Ui.label("座位 " + joinSeats(item.seats()), Ui.INK_SOFT));
        row.add(info, BorderLayout.CENTER);

        JPanel right = Ui.panel(Ui.SURFACE);
        right.setLayout(new FlowLayout(FlowLayout.RIGHT, 14, 28));
        right.add(Ui.heading(Money.format(item.lineTotalFen()), 16));
        JButton remove = Ui.danger("删除");
        remove.addActionListener(event -> {
            service.removeCartItem(account.id(), item.id());
            showCart();
        });
        right.add(remove);
        row.add(right, BorderLayout.EAST);
        return row;
    }

    private final class CheckoutView extends JPanel {
        private final JPanel orderArea = new JPanel();
        private final JLabel original = Ui.label("");
        private final JLabel discount = Ui.label("");
        private final JLabel payable = Ui.heading("", 22);
        private final JButton pay = Ui.primary("模拟支付");
        private TicketOrder currentOrder;

        private CheckoutView() {
            setLayout(new BorderLayout());
            setBackground(Ui.CANVAS);
            JPanel top = Ui.panel(Ui.SURFACE);
            top.setLayout(new FlowLayout(FlowLayout.LEFT, 12, 15));
            top.setBorder(new EmptyBorder(8, 24, 8, 24));
            JButton back = Ui.textButton("返回", Ui.MUTED);
            back.addActionListener(event -> showHome());
            top.add(back);
            top.add(Ui.heading("确认订单", 20));
            add(top, BorderLayout.NORTH);

            JPanel body = new JPanel(new BorderLayout(20, 0));
            body.setBackground(Ui.CANVAS);
            body.setBorder(new EmptyBorder(20, 26, 26, 26));
            orderArea.setBackground(Ui.CANVAS);
            orderArea.setLayout(new BoxLayout(orderArea, BoxLayout.Y_AXIS));
            body.add(Ui.scrollNoBorder(orderArea), BorderLayout.CENTER);

            Ui.RoundedPanel total = Ui.card();
            total.setPreferredSize(new Dimension(320, 0));
            total.setLayout(new BoxLayout(total, BoxLayout.Y_AXIS));
            total.setBorder(BorderFactory.createCompoundBorder(
                    BorderFactory.createLineBorder(Ui.LINE),
                    new EmptyBorder(24, 22, 24, 22)));
            total.add(Ui.heading("金额", 17));
            total.add(Box.createVerticalStrut(18));
            total.add(totalLine("原价", original));
            total.add(Box.createVerticalStrut(8));
            total.add(totalLine("会员优惠", discount));
            total.add(Box.createVerticalStrut(18));
            payable.setAlignmentX(Component.LEFT_ALIGNMENT);
            payable.setForeground(Ui.PRIMARY);
            total.add(payable);
            total.add(Box.createVerticalGlue());
            pay.setAlignmentX(Component.LEFT_ALIGNMENT);
            pay.setMaximumSize(new Dimension(Integer.MAX_VALUE, 46));
            pay.addActionListener(event -> pay());
            total.add(pay);
            body.add(total, BorderLayout.EAST);
            add(body, BorderLayout.CENTER);
        }

        private JPanel totalLine(String name, JLabel value) {
            JPanel row = new JPanel(new BorderLayout());
            row.setOpaque(false);
            row.setMaximumSize(new Dimension(Integer.MAX_VALUE, 26));
            row.add(Ui.subtle(name), BorderLayout.WEST);
            value.setHorizontalAlignment(JLabel.RIGHT);
            row.add(value, BorderLayout.CENTER);
            return row;
        }

        private void refresh(TicketOrder order) {
            currentOrder = order;
            orderArea.removeAll();
            JLabel orderTitle = Ui.heading("订单 " + order.id(), 15);
            orderTitle.setAlignmentX(Component.LEFT_ALIGNMENT);
            orderArea.add(orderTitle);
            orderArea.add(Box.createVerticalStrut(4));
            JLabel created = Ui.subtle("下单时间 " + order.createdAt());
            created.setAlignmentX(Component.LEFT_ALIGNMENT);
            orderArea.add(created);
            orderArea.add(Box.createVerticalStrut(14));
            for (TicketOrderItem item : order.items()) {
                orderArea.add(orderItemRow(item));
                orderArea.add(Box.createVerticalStrut(10));
            }
            original.setText(Money.format(order.originalFen()));
            discount.setText("-" + Money.format(order.discountFen()) + "（" + order.membershipLabel() + "）");
            payable.setText("应付 " + Money.format(order.payableFen()));
            pay.setEnabled("UNPAID".equals(order.status()));
            orderArea.revalidate();
            orderArea.repaint();
        }

        private JPanel orderItemRow(TicketOrderItem item) {
            Ui.RoundedPanel row = Ui.card();
            row.setMaximumSize(new Dimension(Integer.MAX_VALUE, 106));
            row.setLayout(new BorderLayout());
            row.setBorder(BorderFactory.createCompoundBorder(
                    BorderFactory.createLineBorder(Ui.LINE),
                    new EmptyBorder(14, 20, 14, 20)));
            JPanel info = Ui.panel(Ui.SURFACE);
            info.setLayout(new BoxLayout(info, BoxLayout.Y_AXIS));
            info.add(Ui.heading(item.movieTitle(), 15));
            info.add(Box.createVerticalStrut(6));
            info.add(Ui.subtle(item.hallName() + " · " + item.startTime() + " 至 " + item.endTime()));
            info.add(Box.createVerticalStrut(6));
            info.add(Ui.label("座位 " + joinSeats(item.seats()), Ui.INK_SOFT));
            row.add(info, BorderLayout.CENTER);
            JPanel price = Ui.panel(Ui.SURFACE);
            price.setLayout(new FlowLayout(FlowLayout.RIGHT, 10, 28));
            price.add(Ui.heading(Money.format(item.lineTotalFen()), 16));
            row.add(price, BorderLayout.EAST);
            return row;
        }

        private void pay() {
            try {
                TicketOrder paid = service.payOrder(currentOrder.id());
                String message = "订单 " + paid.id() + "\n出票码：" + paid.ticketCode();
                if (paid.upgraded()) {
                    message += "\n\n恭喜升级：" + paid.upgradedFrom().label()
                            + " → " + paid.upgradedTo().label()
                            + "（累计消费满 " + Money.format(paid.upgradedTo().thresholdFen()) + "）";
                }
                Ui.info(this, paid.upgraded() ? "支付成功 · 会员升级" : "支付成功", message);
                showOrders();
            } catch (DemoException ex) {
                Ui.error(this, "支付失败", ex.getMessage());
            }
        }
    }

    /** 会员页：等级卡 + 本卡权益 + 观影数据 + 升级进度 + 三档权益对比。 */
    private final class MembershipView extends JPanel {
        private final JPanel actions = new JPanel();
        private final JLabel levelLabel = Ui.heading("", 28);
        private final JLabel benefitLabel = Ui.label("", Ui.INK_SOFT);
        private final JLabel validityLabel = Ui.subtle("");
        private final CircleBadge levelBadge = new CircleBadge(Ui.GOLD, NavIcon.Kind.STAR, Ui.CANVAS, 62);
        private final Ui.RoundedPanel levelCard = Ui.card(Ui.SURFACE, 14);
        private final JPanel benefitRow = new JPanel(new GridLayout(1, 4, 14, 0));
        private final JLabel[] statValues = new JLabel[4];
        private final JLabel progressText = Ui.heading("", 16);
        private final ProgressBar progress = new ProgressBar();
        private final JPanel compareRow = new JPanel(new GridLayout(2, 3, 14, 14));

        private MembershipView() {
            setLayout(new BorderLayout());
            setBackground(Ui.CANVAS);
            JLabel title = Ui.heading("我的会员", 22);
            title.setBorder(new EmptyBorder(20, 28, 14, 0));
            add(title, BorderLayout.NORTH);

            JPanel content = Ui.panel(Ui.CANVAS);
            content.setLayout(new BoxLayout(content, BoxLayout.Y_AXIS));
            content.setBorder(new EmptyBorder(0, 28, 26, 28));
            content.add(levelSection());
            content.add(Box.createVerticalStrut(18));
            content.add(section("本卡权益", benefitSection()));
            content.add(Box.createVerticalStrut(18));
            content.add(section("我的观影数据", statsSection()));
            content.add(Box.createVerticalStrut(18));
            content.add(section("升级进度", progressSection()));
            content.add(Box.createVerticalStrut(18));
            content.add(section("等级权益对比", compareSection()));
            content.add(Box.createVerticalGlue());

            JScrollPane scroll = Ui.scrollNoBorder(content);
            scroll.getViewport().setBackground(Ui.CANVAS);
            add(scroll, BorderLayout.CENTER);
        }

        // ---------- 区块 1：等级卡 ----------

        private JPanel levelSection() {
            levelCard.setLayout(new BorderLayout(22, 0));
            levelCard.setBorder(BorderFactory.createCompoundBorder(
                    BorderFactory.createLineBorder(Ui.LINE),
                    new EmptyBorder(24, 26, 24, 26)));
            levelCard.setMaximumSize(new Dimension(Integer.MAX_VALUE, 132));

            JPanel left = Ui.panel(Ui.SURFACE);
            left.setLayout(new BoxLayout(left, BoxLayout.X_AXIS));
            levelBadge.setAlignmentY(Component.CENTER_ALIGNMENT);
            left.add(levelBadge);
            left.add(Box.createHorizontalStrut(20));

            JPanel texts = Ui.panel(Ui.SURFACE);
            texts.setLayout(new BoxLayout(texts, BoxLayout.Y_AXIS));
            texts.add(Box.createVerticalGlue());
            levelLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
            texts.add(levelLabel);
            texts.add(Box.createVerticalStrut(8));
            benefitLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
            benefitLabel.setFont(Ui.font(Font.PLAIN, 15));
            texts.add(benefitLabel);
            texts.add(Box.createVerticalStrut(6));
            validityLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
            texts.add(validityLabel);
            texts.add(Box.createVerticalGlue());
            left.add(texts);
            levelCard.add(left, BorderLayout.CENTER);

            actions.setOpaque(false);
            actions.setLayout(new FlowLayout(FlowLayout.RIGHT, 10, 0));
            JPanel actionHolder = Ui.panel(Ui.SURFACE);
            actionHolder.setLayout(new GridBagLayout());
            actionHolder.add(actions);
            levelCard.add(actionHolder, BorderLayout.EAST);
            return levelCard;
        }

        // ---------- 区块 2：本卡权益 ----------

        private JPanel benefitSection() {
            benefitRow.setOpaque(false);
            return benefitRow;
        }

        private void renderBenefits(MembershipLevel level) {
            benefitRow.removeAll();
            List<String[]> benefits = level.benefitLines();
            NavIcon.Kind[] icons = {NavIcon.Kind.TICKET, NavIcon.Kind.STAR,
                    NavIcon.Kind.GIFT, NavIcon.Kind.POPCORN};
            Color tint = Ui.tierColor(level);
            for (int i = 0; i < benefits.size() && i < icons.length; i++) {
                benefitRow.add(benefitCard(icons[i], tint, benefits.get(i)[0], benefits.get(i)[1]));
            }
            benefitRow.revalidate();
            benefitRow.repaint();
        }

        private JPanel benefitCard(NavIcon.Kind icon, Color tint, String title, String detail) {
            Ui.RoundedPanel card = Ui.card(Ui.SURFACE, 12);
            card.setLayout(new BorderLayout(14, 0));
            card.setBorder(BorderFactory.createCompoundBorder(
                    BorderFactory.createLineBorder(Ui.LINE),
                    new EmptyBorder(16, 18, 16, 18)));

            CircleBadge badge = new CircleBadge(levelTint(tint), icon, tint, 40);
            JPanel badgeHolder = Ui.panel(Ui.SURFACE);
            badgeHolder.setLayout(new GridBagLayout());
            badgeHolder.add(badge);
            card.add(badgeHolder, BorderLayout.WEST);

            JPanel texts = Ui.panel(Ui.SURFACE);
            texts.setLayout(new BoxLayout(texts, BoxLayout.Y_AXIS));
            texts.add(Box.createVerticalGlue());
            JLabel head = Ui.label(title, Ui.INK);
            head.setFont(Ui.font(Font.BOLD, 14));
            head.setAlignmentX(Component.LEFT_ALIGNMENT);
            texts.add(head);
            texts.add(Box.createVerticalStrut(5));
            JLabel body = Ui.label("<html><body style='width:118px'>" + detail + "</body></html>", Ui.MUTED);
            body.setFont(Ui.font(Font.PLAIN, 12));
            body.setAlignmentX(Component.LEFT_ALIGNMENT);
            texts.add(body);
            texts.add(Box.createVerticalGlue());
            card.add(texts, BorderLayout.CENTER);
            return card;
        }

        // ---------- 区块 3：观影数据 ----------

        private JPanel statsSection() {
            String[] titles = {"累计消费", "已省金额", "观影次数", "购票张数"};
            NavIcon.Kind[] icons = {NavIcon.Kind.TICKET, NavIcon.Kind.GIFT,
                    NavIcon.Kind.FILM, NavIcon.Kind.CART};
            JPanel row = new JPanel(new GridLayout(1, 4, 14, 0));
            row.setOpaque(false);
            for (int i = 0; i < titles.length; i++) {
                Ui.RoundedPanel card = Ui.card(Ui.SURFACE, 12);
                card.setLayout(new BorderLayout(12, 0));
                card.setBorder(BorderFactory.createCompoundBorder(
                        BorderFactory.createLineBorder(Ui.LINE),
                        new EmptyBorder(16, 18, 16, 18)));

                CircleBadge badge = new CircleBadge(Ui.PRIMARY_SOFT, icons[i], Ui.PRIMARY, 38);
                JPanel badgeHolder = Ui.panel(Ui.SURFACE);
                badgeHolder.setLayout(new GridBagLayout());
                badgeHolder.add(badge);
                card.add(badgeHolder, BorderLayout.WEST);

                JPanel texts = Ui.panel(Ui.SURFACE);
                texts.setLayout(new BoxLayout(texts, BoxLayout.Y_AXIS));
                texts.add(Box.createVerticalGlue());
                JLabel value = Ui.heading("--", 20);
                statValues[i] = value;
                value.setAlignmentX(Component.LEFT_ALIGNMENT);
                texts.add(value);
                texts.add(Box.createVerticalStrut(4));
                JLabel name = Ui.subtle(titles[i]);
                name.setFont(Ui.font(Font.PLAIN, 12));
                name.setAlignmentX(Component.LEFT_ALIGNMENT);
                texts.add(name);
                texts.add(Box.createVerticalGlue());
                card.add(texts, BorderLayout.CENTER);
                row.add(card);
            }
            return row;
        }

        // ---------- 区块 4：升级进度 ----------

        private JPanel progressSection() {
            Ui.RoundedPanel card = Ui.card(Ui.SURFACE, 12);
            card.setLayout(new BorderLayout(18, 0));
            card.setBorder(BorderFactory.createCompoundBorder(
                    BorderFactory.createLineBorder(Ui.LINE),
                    new EmptyBorder(20, 24, 20, 24)));

            progressText.setAlignmentX(Component.LEFT_ALIGNMENT);
            JPanel left = Ui.panel(Ui.SURFACE);
            left.setLayout(new GridBagLayout());
            left.add(progressText);
            card.add(left, BorderLayout.WEST);

            progress.setPreferredSize(new Dimension(320, 12));
            JPanel barHolder = Ui.panel(Ui.SURFACE);
            barHolder.setLayout(new GridBagLayout());
            GridBagConstraints barConstraints = new GridBagConstraints();
            barConstraints.fill = GridBagConstraints.HORIZONTAL;
            barConstraints.weightx = 1;
            barHolder.add(progress, barConstraints);
            card.add(barHolder, BorderLayout.CENTER);
            return card;
        }

        // ---------- 区块 5：等级权益对比 ----------

        /** 对比区每行放 3 列，5 档排成两行（第二行补 1 个空位）。 */
        private static final int COMPARE_COLUMNS = 3;

        private JPanel compareSection() {
            compareRow.setOpaque(false);
            return compareRow;
        }

        private void renderComparison(MembershipLevel current, int spentFen) {
            compareRow.removeAll();
            MembershipLevel[] levels = MembershipLevel.values();
            for (MembershipLevel level : levels) {
                compareRow.add(compareColumn(level, level == current, reached(level, spentFen)));
            }
            // 补齐空位，避免最后一行被拉伸
            int remainder = levels.length % COMPARE_COLUMNS;
            if (remainder != 0) {
                for (int i = remainder; i < COMPARE_COLUMNS; i++) {
                    JPanel filler = Ui.panel(Ui.CANVAS);
                    filler.setOpaque(false);
                    compareRow.add(filler);
                }
            }
            compareRow.revalidate();
            compareRow.repaint();
        }

        private JPanel compareColumn(MembershipLevel level, boolean active, boolean unlocked) {
            Color tint = Ui.tierColor(level);
            Color cardFill = active ? Ui.PRIMARY_SOFT : Ui.SURFACE;
            Ui.RoundedPanel card = Ui.card(cardFill, 12);
            card.setLayout(new BorderLayout());
            card.setBorder(BorderFactory.createCompoundBorder(
                    BorderFactory.createLineBorder(active ? tint : Ui.LINE, active ? 2 : 1),
                    new EmptyBorder(18, 20, 18, 20)));

            JPanel body = Ui.panel(cardFill);
            body.setLayout(new BoxLayout(body, BoxLayout.Y_AXIS));

            JPanel head = Ui.panel(cardFill);
            head.setLayout(new BorderLayout());
            head.setMaximumSize(new Dimension(Integer.MAX_VALUE, 30));
            JLabel name = Ui.label(level.label(), tint);
            name.setFont(Ui.font(Font.BOLD, 15));
            head.add(name, BorderLayout.WEST);
            if (active) {
                JLabel tag = Ui.label("当前等级", Ui.PRIMARY);
                tag.setFont(Ui.font(Font.BOLD, 11));
                head.add(tag, BorderLayout.EAST);
            } else if (unlocked) {
                JLabel tag = Ui.label("已达成", Ui.MUTED);
                tag.setFont(Ui.font(Font.BOLD, 11));
                head.add(tag, BorderLayout.EAST);
            }
            body.add(head);
            body.add(Box.createVerticalStrut(12));

            JLabel price = Ui.heading(level.discountLabel(), 26);
            price.setForeground(level.hasDiscount() ? tint : Ui.MUTED);
            price.setAlignmentX(Component.LEFT_ALIGNMENT);
            body.add(price);
            body.add(Box.createVerticalStrut(4));
            JLabel priceNote = Ui.subtle("《示例影片》¥45 → " + Money.format(
                    Money.applyDiscount(4500, level.discountBasisPoints())));
            priceNote.setFont(Ui.font(Font.PLAIN, 12));
            priceNote.setAlignmentX(Component.LEFT_ALIGNMENT);
            body.add(priceNote);
            body.add(Box.createVerticalStrut(4));
            JLabel threshold = Ui.subtle(level.thresholdFen() == 0
                    ? "免费注册即享"
                    : "累计消费满 " + Money.format(level.thresholdFen()));
            threshold.setFont(Ui.font(Font.PLAIN, 11));
            threshold.setAlignmentX(Component.LEFT_ALIGNMENT);
            body.add(threshold);
            body.add(Box.createVerticalStrut(14));

            for (String[] benefit : level.benefitLines()) {
                JPanel line = Ui.panel(cardFill);
                line.setLayout(new BorderLayout(8, 0));
                line.setMaximumSize(new Dimension(Integer.MAX_VALUE, 24));
                NavIcon check = new NavIcon(NavIcon.Kind.STAR, 12, tint);
                JPanel mark = Ui.panel(cardFill);
                mark.setLayout(new GridBagLayout());
                mark.add(check);
                line.add(mark, BorderLayout.WEST);
                JLabel text = Ui.label(benefit[0], Ui.INK_SOFT);
                text.setFont(Ui.font(Font.PLAIN, 13));
                line.add(text, BorderLayout.CENTER);
                body.add(line);
                body.add(Box.createVerticalStrut(8));
            }
            card.add(body, BorderLayout.CENTER);
            return card;
        }

        /** 该档门槛是否已被累计消费满足——满足即已获得，未满足则只能靠继续消费。 */
        private static boolean reached(MembershipLevel level, int spentFen) {
            return spentFen >= level.thresholdFen();
        }

        // ---------- 通用 ----------

        private JPanel section(String heading, JPanel body) {
            JPanel wrapper = Ui.panel(Ui.CANVAS);
            wrapper.setLayout(new BoxLayout(wrapper, BoxLayout.Y_AXIS));
            JLabel title = Ui.heading(heading, 16);
            title.setAlignmentX(Component.LEFT_ALIGNMENT);
            title.setBorder(new EmptyBorder(0, 2, 12, 0));
            wrapper.add(title);
            body.setAlignmentX(Component.LEFT_ALIGNMENT);
            wrapper.add(body);
            return wrapper;
        }

        private void refresh() {
            account = service.account(account.id());
            MembershipLevel level = account.membership();
            Color tint = Ui.tierColor(level);

            levelLabel.setText(level.label());
            levelLabel.setForeground(tint);
            levelCard.setFill(Ui.tierSoftColor(level));
            levelBadge.setFill(tint);

            benefitLabel.setText(level.hasDiscount()
                    ? "购票享受 " + level.discountText() + " 优惠"
                    : "普通会员购票无折扣");
            validityLabel.setText("演示有效期至 " + LocalDate.now().plusYears(1).format(EXPIRY));

            renderBenefits(level);
            int spentFen = loadStats(level);
            renderComparison(level, spentFen);

            actions.removeAll();
            JLabel note = Ui.label("等级由累计消费自动升级，购票出票后立即生效", Ui.MUTED);
            note.setFont(Ui.font(Font.PLAIN, 12));
            actions.add(note);
            actions.revalidate();
            actions.repaint();
        }

        /** 汇总订单数据并更新统计格与升级进度；同时返回累计消费，供对比区复用。 */
        private int loadStats(MembershipLevel level) {
            MembershipStats stats = MembershipStats.of(service.ordersForUser(account.id()));
            statValues[0].setText(Money.format(stats.spentFen()));
            statValues[1].setText(Money.format(stats.savedFen()));
            statValues[2].setText(stats.paidOrderCount() + " 次");
            statValues[3].setText(stats.ticketCount() + " 张");

            int spent = stats.spentFen();
            MembershipLevel next = level.next();
            if (next == null) {
                Color tint = Ui.tierColor(level);
                progress.setValues(1f, tint);
                progressText.setForeground(tint);
                progressText.setText(level.label() + "已是最高等级，感谢您的支持");
            } else {
                int target = next.thresholdFen();
                int remain = next.remainingFrom(spent);
                progress.setValues(target <= 0 ? 1f : Math.min(1f, spent / (float) target),
                        Ui.tierColor(level));
                progressText.setForeground(Ui.INK);
                progressText.setText("再消费 " + Money.format(remain) + " 即可升级 " + next.label());
            }
            progress.repaint();
            return spent;
        }

    }

    /** 圆角进度条，fill 为 0~1 的比例。 */
    private static final class ProgressBar extends JComponent {
        private float ratio;
        private Color color = Ui.PRIMARY;

        private ProgressBar() {
            setOpaque(false);
        }

        private void setValues(float ratio, Color color) {
            this.ratio = Math.max(0f, Math.min(1f, ratio));
            this.color = color;
        }

        @Override
        protected void paintComponent(Graphics graphics) {
            Graphics2D g2 = (Graphics2D) graphics.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            int height = getHeight();
            int width = getWidth();
            int radius = Math.min(height, width);
            g2.setColor(Ui.LINE);
            g2.fillRoundRect(0, 0, width, height, radius, radius);
            int filled = Math.round(width * ratio);
            if (filled > 0) {
                g2.setColor(color);
                g2.fillRoundRect(0, 0, Math.max(filled, height), height, radius, radius);
            }
            g2.dispose();
        }
    }

    private final class OrdersView extends JPanel {
        private final JPanel orders = new JPanel();

        private OrdersView() {
            setLayout(new BorderLayout());
            setBackground(Ui.CANVAS);
            JLabel title = Ui.heading("我的订单", 22);
            title.setBorder(new EmptyBorder(20, 28, 18, 0));
            add(title, BorderLayout.NORTH);
            orders.setBackground(Ui.CANVAS);
            orders.setLayout(new BoxLayout(orders, BoxLayout.Y_AXIS));
            add(Ui.scrollNoBorder(orders), BorderLayout.CENTER);
        }

        private void refresh() {
            orders.removeAll();
            List<TicketOrder> list = service.ordersForUser(account.id());
            if (list.isEmpty()) {
                JPanel empty = emptyPanel("还没有订单");
                orders.add(empty);
            } else {
                for (TicketOrder order : list) {
                    orders.add(orderRow(order));
                    orders.add(Box.createVerticalStrut(10));
                }
            }
            orders.revalidate();
            orders.repaint();
        }
    }

    private JPanel orderRow(TicketOrder order) {
        Ui.RoundedPanel row = Ui.card();
        // 未支付订单有三个操作按钮，148px 会把「取消订单」裁掉
        row.setMaximumSize(new Dimension(Integer.MAX_VALUE, 190));
        row.setLayout(new BorderLayout());
        row.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(Ui.LINE),
                new EmptyBorder(15, 22, 15, 16)));

        JPanel head = Ui.panel(Ui.SURFACE);
        head.setLayout(new BoxLayout(head, BoxLayout.Y_AXIS));
        JPanel top = Ui.panel(Ui.SURFACE);
        top.setLayout(new FlowLayout(FlowLayout.LEFT, 12, 0));
        top.add(Ui.heading(order.id(), 15));
        top.add(statusLabel(order.status()));
        head.add(top);
        head.add(Box.createVerticalStrut(7));
        JLabel created = Ui.subtle("下单 " + order.createdAt() + " · " + order.membershipLabel());
        created.setAlignmentX(Component.LEFT_ALIGNMENT);
        head.add(created);
        head.add(Box.createVerticalStrut(8));
        for (TicketOrderItem item : order.items()) {
            JLabel line = Ui.label(item.movieTitle() + " · " + item.startTime()
                    + " · " + item.hallName() + " · 座位 " + joinSeats(item.seats()), Ui.INK_SOFT);
            line.setAlignmentX(Component.LEFT_ALIGNMENT);
            head.add(line);
            head.add(Box.createVerticalStrut(4));
        }
        row.add(head, BorderLayout.CENTER);

        JPanel actions = Ui.panel(Ui.SURFACE);
        actions.setLayout(new BoxLayout(actions, BoxLayout.Y_AXIS));
        actions.add(Ui.heading("实付 " + Money.format(order.payableFen()), 16));
        actions.add(Box.createVerticalStrut(12));
        if ("UNPAID".equals(order.status())) {
            // 未支付订单必须能回到支付页，否则用户关掉结算页后这笔单就再也付不了了
            JButton pay = Ui.primary("去支付");
            pay.setAlignmentX(Component.LEFT_ALIGNMENT);
            pay.addActionListener(event -> showCheckout(order));
            actions.add(pay);
            actions.add(Box.createVerticalStrut(6));
        }
        JButton detail = Ui.secondary("查看明细");
        detail.setAlignmentX(Component.LEFT_ALIGNMENT);
        detail.addActionListener(event -> showOrderDetail(order));
        actions.add(detail);
        if ("UNPAID".equals(order.status())) {
            actions.add(Box.createVerticalStrut(6));
            JButton cancel = Ui.danger("取消订单");
            cancel.setAlignmentX(Component.LEFT_ALIGNMENT);
            cancel.addActionListener(event -> {
                if (Ui.confirm(this, "取消订单", "确定取消该订单吗？")) {
                    service.cancelOrder(order.id());
                    showOrders();
                }
            });
            actions.add(cancel);
        }
        row.add(actions, BorderLayout.EAST);
        return row;
    }

    private void showOrderDetail(TicketOrder order) {
        StringBuilder text = new StringBuilder();
        text.append("订单：").append(order.id()).append("\n");
        text.append("下单时间：").append(order.createdAt()).append("\n");
        text.append("会员优惠：").append(order.membershipLabel()).append("\n");
        for (TicketOrderItem item : order.items()) {
            text.append("\n").append(item.movieTitle()).append("\n");
            text.append(item.hallName()).append(" · ").append(item.startTime())
                    .append(" 至 ").append(item.endTime()).append("\n");
            text.append("座位：").append(joinSeats(item.seats())).append("\n");
            text.append("小计：").append(Money.format(item.lineTotalFen())).append("\n");
        }
        text.append("\n原价：").append(Money.format(order.originalFen()));
        text.append("\n优惠：-").append(Money.format(order.discountFen()));
        text.append("\n实付：").append(Money.format(order.payableFen()));
        text.append("\n状态：").append(Ui.statusText(order.status()));
        if (!order.ticketCode().isBlank()) {
            text.append("\n出票码：").append(order.ticketCode());
        }
        javax.swing.JTextArea area = new javax.swing.JTextArea(text.toString(), 18, 46);
        area.setEditable(false);
        area.setFont(Ui.font(Font.PLAIN, 13));
        area.setBackground(Ui.SURFACE);
        area.setForeground(Ui.INK);
        JScrollPane scroll = Ui.scroll(area);
        scroll.setPreferredSize(new Dimension(540, 330));
        Ui.showFormDialog(this, "订单明细", scroll, 560, 350);
    }

    private static JPanel emptyPanel(String text) {
        JPanel panel = Ui.panel(Ui.CANVAS);
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
        panel.add(Box.createVerticalStrut(120));
        JLabel label = Ui.heading(text, 18);
        label.setAlignmentX(Component.CENTER_ALIGNMENT);
        label.setForeground(Ui.MUTED);
        panel.add(label);
        return panel;
    }

    private static void styleSearch(JTextField field) {
        field.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(Ui.LINE),
                BorderFactory.createEmptyBorder(8, 12, 8, 12)));
        field.setFont(Ui.FONT);
        field.setPreferredSize(new Dimension(190, 38));
    }

    private static void makeClickable(Component component, Runnable action) {
        MouseAdapter adapter = new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent event) {
                action.run();
            }
        };
        applyMouse(component, adapter);
    }

    private static void applyMouse(Component component, MouseAdapter adapter) {
        component.addMouseListener(adapter);
        component.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        if (component instanceof Container container) {
            for (Component child : container.getComponents()) {
                applyMouse(child, adapter);
            }
        }
    }
}
