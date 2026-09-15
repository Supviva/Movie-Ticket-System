package com.movieticket.ui.screen;

import com.movieticket.demo.DemoException;
import com.movieticket.demo.DemoMovieTicketService;
import com.movieticket.model.Account;
import com.movieticket.model.CinemaHall;
import com.movieticket.model.MembershipLevel;
import com.movieticket.model.Money;
import com.movieticket.model.Movie;
import com.movieticket.model.ShowSession;
import com.movieticket.model.TicketOrder;
import com.movieticket.model.TicketOrderItem;
import com.movieticket.ui.component.Ui;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.DefaultComboBoxModel;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JPasswordField;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.table.DefaultTableModel;
import java.awt.BorderLayout;
import java.awt.CardLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.util.List;

public final class AdminPanel extends JPanel {

    private final DemoMovieTicketService service;
    private final Runnable onLogout;
    private final Account account;
    private final CardLayout modulesLayout = new CardLayout();
    private final JPanel moduleStack = new JPanel(modulesLayout);
    private final CustomerModule customerModule;
    private final MovieModule movieModule;
    private final HallModule hallModule;
    private final SessionModule sessionModule;
    private final OrderModule orderModule;

    public AdminPanel(DemoMovieTicketService service, Account account, Runnable onLogout) {
        this.service = service;
        this.account = account;
        this.onLogout = onLogout;
        customerModule = new CustomerModule();
        movieModule = new MovieModule();
        hallModule = new HallModule();
        sessionModule = new SessionModule();
        orderModule = new OrderModule();

        setLayout(new BorderLayout());
        setBackground(Ui.CANVAS);
        add(createHeader(), BorderLayout.NORTH);

        JPanel body = new JPanel(new BorderLayout());
        body.setBackground(Ui.CANVAS);
        body.add(createSidebar(), BorderLayout.WEST);
        moduleStack.setBackground(Ui.CANVAS);
        moduleStack.add(customerModule, "customers");
        moduleStack.add(movieModule, "movies");
        moduleStack.add(hallModule, "halls");
        moduleStack.add(sessionModule, "sessions");
        moduleStack.add(orderModule, "orders");
        body.add(moduleStack, BorderLayout.CENTER);
        add(body, BorderLayout.CENTER);
        select("customers", customerModule);
    }

    private JPanel createHeader() {
        JPanel header = Ui.panel(Ui.SURFACE);
        header.setPreferredSize(new Dimension(0, 64));
        header.setBorder(BorderFactory.createMatteBorder(0, 0, 1, 0, Ui.LINE));
        header.setLayout(new BorderLayout());
        JLabel brand = Ui.heading("光影票务 · 管理后台", 18);
        brand.setForeground(Ui.INK);
        brand.setBorder(BorderFactory.createEmptyBorder(0, 26, 0, 0));
        header.add(brand, BorderLayout.WEST);
        JPanel right = Ui.panel(Ui.SURFACE);
        right.setLayout(new FlowLayout(FlowLayout.RIGHT, 14, 18));
        right.add(Ui.subtle("管理员：" + account.displayName()));
        JButton logout = Ui.textButton("退出", Ui.MUTED);
        logout.addActionListener(event -> onLogout.run());
        right.add(logout);
        header.add(right, BorderLayout.EAST);
        return header;
    }

    private JPanel createSidebar() {
        JPanel sidebar = Ui.panel(Ui.SURFACE);
        sidebar.setPreferredSize(new Dimension(210, 0));
        sidebar.setBorder(BorderFactory.createMatteBorder(0, 0, 0, 1, Ui.LINE));
        sidebar.setLayout(new BoxLayout(sidebar, BoxLayout.Y_AXIS));
        sidebar.add(Box.createVerticalStrut(14));
        sidebar.add(nav("顾客管理", "customers", customerModule));
        sidebar.add(nav("影片管理", "movies", movieModule));
        sidebar.add(nav("影厅管理", "halls", hallModule));
        sidebar.add(nav("场次管理", "sessions", sessionModule));
        sidebar.add(nav("订单管理", "orders", orderModule));
        navSelection = new Ui.NavSelection(
                navItems.toArray(new JComponent[0]), this::paintNavItem);
        navSelection.select(0);
        return sidebar;
    }

    private final List<SideNav> navItems = new java.util.ArrayList<>();
    private Ui.NavSelection navSelection;

    /** 侧栏菜单项，承载自身的绘制状态。 */
    private final class SideNav extends JPanel {
        private final String card;
        private final ModulePanel module;
        private final JLabel label;
        private boolean highlighted;

        private SideNav(String text, String card, ModulePanel module) {
            this.card = card;
            this.module = module;
            setLayout(new BorderLayout());
            setOpaque(true);
            setBackground(Ui.SURFACE);
            setPreferredSize(new Dimension(208, 46));
            setMaximumSize(new Dimension(Integer.MAX_VALUE, 46));
            setCursor(java.awt.Cursor.getPredefinedCursor(java.awt.Cursor.HAND_CURSOR));

            label = Ui.label(text, Ui.INK);
            label.setBorder(BorderFactory.createEmptyBorder(0, 26, 0, 20));
            add(label, BorderLayout.CENTER);

            addMouseListener(new java.awt.event.MouseAdapter() {
                @Override
                public void mouseClicked(java.awt.event.MouseEvent event) {
                    select(SideNav.this.card, SideNav.this.module);
                }
            });
        }

        private void setHighlighted(boolean value) {
            if (highlighted == value) {
                return;
            }
            highlighted = value;
            setBackground(value ? Ui.PRIMARY_SOFT : Ui.SURFACE);
            label.setForeground(value ? Ui.PRIMARY : Ui.INK);
            label.setFont(value ? Ui.font(Font.BOLD, 14) : Ui.font(Font.PLAIN, 14));
            repaint();
        }

        /** 选中时在左侧描一条 3px 主色指示条。 */
        @Override
        protected void paintComponent(java.awt.Graphics graphics) {
            super.paintComponent(graphics);
            if (!highlighted) {
                return;
            }
            java.awt.Graphics2D g2 = (java.awt.Graphics2D) graphics.create();
            g2.setColor(Ui.PRIMARY);
            g2.fillRect(0, 0, 3, getHeight());
            g2.dispose();
        }
    }

    /**
     * 重绘单个侧栏项。
     *
     * <p>选中与悬停都用浅橙底 + 主色文字；选中额外在左侧画 3px 主色指示条，
     * 让用户知道当前在哪个模块。</p>
     */
    private void paintNavItem(JComponent component, boolean highlighted) {
        if (component instanceof SideNav item) {
            item.setHighlighted(highlighted);
            item.repaint();
        }
    }

    private SideNav nav(String label, String card, ModulePanel module) {
        SideNav item = new SideNav(label, card, module);
        navItems.add(item);
        return item;
    }

    private void select(String card, ModulePanel module) {
        module.refresh("");
        modulesLayout.show(moduleStack, card);
        if (navSelection != null) {
            for (int i = 0; i < navItems.size(); i++) {
                if (navItems.get(i).card.equals(card)) {
                    navSelection.select(i);
                    break;
                }
            }
        }
    }

    private interface ModulePanel {
        void refresh(String keyword);
    }

    // ---- 测试钩子：暴露模块切换与当前模块状态，避免用反射 ----

    /** 切到指定模块（card 取值见 {@link #createSidebar()}）。 */
    void selectModuleForTest(String card) {
        for (SideNav item : navItems) {
            if (item.card.equals(card)) {
                select(card, item.module);
                return;
            }
        }
        throw new IllegalArgumentException("未知模块: " + card);
    }

    /** 对当前显示的模块按关键词刷新一次。 */
    void refreshCurrentForTest(String keyword) {
        for (SideNav item : navItems) {
            if (((JComponent) item.module).isVisible()) {
                item.module.refresh(keyword);
                return;
            }
        }
        throw new IllegalStateException("没有任何可见模块");
    }

    /** 当前显示的模块是不是还在展示表格（相对空状态）。 */
    boolean currentModuleShowsTable() {
        for (SideNav item : navItems) {
            if (!((JComponent) item.module).isVisible()) {
                continue;
            }
            Ui.TableCard card = tableCardOf(item.module);
            if (card == null) {
                return true;
            }
            for (Component child : card.getComponents()) {
                if (child.isVisible() && child instanceof javax.swing.JScrollPane) {
                    return true;
                }
            }
            return false;
        }
        throw new IllegalStateException("没有任何可见模块");
    }

    private static Ui.TableCard tableCardOf(ModulePanel module) {
        for (java.lang.reflect.Field field : module.getClass().getDeclaredFields()) {
            if (Ui.TableCard.class.isAssignableFrom(field.getType())) {
                try {
                    field.setAccessible(true);
                    return (Ui.TableCard) field.get(module);
                } catch (IllegalAccessException ex) {
                    throw new IllegalStateException(ex);
                }
            }
        }
        return null;
    }

    private final class CustomerModule extends JPanel implements ModulePanel {
        private final JTable table = new JTable();
        private final DefaultTableModel model = new DefaultTableModel(
                new Object[]{"编号", "用户名", "姓名", "手机号", "会员等级", "状态"}, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };
        private final Ui.TableCard tableCard;
        private final JTextField search = new JTextField(16);

        private CustomerModule() {
            setLayout(new BorderLayout());
            setBackground(Ui.CANVAS);
            add(toolbar("顾客管理", CustomerModule.this, "新增顾客", this::addCustomer,
                    new Action("编辑", this::editCustomer),
                    new Action("会员设置", this::membership),
                    new Action("查看订单", this::viewOrders)), BorderLayout.NORTH);
            table.setModel(model);
            Ui.configureTable(table);
            tableCard = new Ui.TableCard(table, Ui.scroll(table));
            add(tableCard, BorderLayout.CENTER);
        }

        @Override
        public void refresh(String keyword) {
            String query = keyword == null ? search.getText() : keyword;
            model.setRowCount(0);
            for (Account account : service.customers(query)) {
                model.addRow(new Object[]{
                        account.id(), account.username(), account.displayName(),
                        account.phone(), account.membership().label(),
                        account.enabled() ? "启用" : "禁用"
                });
            }
            tableCard.sync("暂无顾客", query.isBlank() ? "还没有任何顾客账号" : "没有与「" + query + "」匹配的顾客");
        }

        private void addCustomer() {
            JTextField username = field();
            JTextField name = field();
            JTextField phone = field();
            JPasswordField password = new JPasswordField();
            styleInput(password);
            boolean ok = Ui.showFormDialog(this, "新增顾客",
                    formGrid(new String[]{"用户名", "姓名", "手机号", "初始密码"},
                            new JComponent[]{username, name, phone, password}), 440, 280);
            if (!ok) {
                return;
            }
            try {
                service.addCustomer(username.getText().trim(), name.getText().trim(),
                        phone.getText().trim(), new String(password.getPassword()));
                refresh("");
            } catch (DemoException ex) {
                Ui.error(this, "新增失败", ex.getMessage());
            }
        }

        private void editCustomer() {
            Account selected = selectedAccount();
            if (selected == null) {
                return;
            }
            JTextField name = field(selected.displayName());
            JTextField phone = field(selected.phone());
            JComboBox<String> status = new JComboBox<>(new String[]{"启用", "禁用"});
            status.setSelectedItem(selected.enabled() ? "启用" : "禁用");
            boolean ok = Ui.showFormDialog(this, "编辑顾客",
                    formGrid(new String[]{"姓名", "手机号", "账号状态"},
                            new JComponent[]{name, phone, status}), 440, 230);
            if (!ok) {
                return;
            }
            try {
                service.updateCustomerProfile(selected.id(), name.getText().trim(),
                        phone.getText().trim(), "启用".equals(status.getSelectedItem()));
                refresh("");
            } catch (DemoException ex) {
                Ui.error(this, "保存失败", ex.getMessage());
            }
        }

        private void membership() {
            Account selected = selectedAccount();
            if (selected == null) {
                return;
            }
            JComboBox<MembershipLevel> levels = new JComboBox<>(MembershipLevel.values());
            levels.setSelectedItem(selected.membership());
            JLabel hint = Ui.subtle("运营手动定档，可高于顾客累计消费对应的等级；"
                    + "顾客后续消费只会在此基础上继续升，不会自动降档。");
            boolean ok = Ui.showFormDialog(this, "会员设置",
                    formGrid(new String[]{"会员等级", "规则"}, new JComponent[]{levels, hint}), 520, 190);
            if (!ok) {
                return;
            }
            try {
                service.setMembership(selected.id(), (MembershipLevel) levels.getSelectedItem());
                refresh("");
            } catch (DemoException ex) {
                Ui.error(this, "设置失败", ex.getMessage());
            }
        }

        private void viewOrders() {
            Account selected = selectedAccount();
            if (selected == null) {
                return;
            }
            List<TicketOrder> orders = service.ordersForUser(selected.id());
            if (orders.isEmpty()) {
                Ui.info(this, "历史订单", "该顾客暂无订单");
                return;
            }
            StringBuilder text = new StringBuilder("顾客：" + selected.displayName() + "\n\n");
            for (TicketOrder order : orders) {
                text.append("订单 ").append(order.id()).append(" · ")
                        .append(Ui.statusText(order.status())).append("\n");
                text.append("下单：").append(order.createdAt()).append("\n");
                for (TicketOrderItem item : order.items()) {
                    text.append("  ").append(item.movieTitle()).append(" · ")
                            .append(item.startTime()).append(" · 座位 ")
                            .append(String.join("、", item.seats())).append("\n");
                }
                text.append("金额：").append(Money.format(order.payableFen())).append("\n\n");
            }
            showText(this, "顾客历史订单", text.toString());
        }

        private Account selectedAccount() {
            int row = table.getSelectedRow();
            if (row < 0) {
                Ui.info(this, "顾客管理", "请先在列表中选择一位顾客");
                return null;
            }
            String id = String.valueOf(model.getValueAt(table.convertRowIndexToModel(row), 0));
            return service.account(id);
        }
    }

    private final class MovieModule extends JPanel implements ModulePanel {
        private final JTable table = new JTable();
        private final DefaultTableModel model = new DefaultTableModel(
                new Object[]{"编号", "片名", "类型", "导演", "主演", "时长", "格式", "评分", "上映日期"}, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };
        private final Ui.TableCard tableCard;
        private final JTextField search = new JTextField(16);

        private MovieModule() {
            setLayout(new BorderLayout());
            setBackground(Ui.CANVAS);
            add(toolbar("影片管理", MovieModule.this, "新增影片", this::addMovie,
                    new Action("编辑", this::editMovie),
                    new Action("删除", this::deleteMovie)), BorderLayout.NORTH);
            table.setModel(model);
            Ui.configureTable(table);
            tableCard = new Ui.TableCard(table, Ui.scroll(table));
            add(tableCard, BorderLayout.CENTER);
        }

        @Override
        public void refresh(String keyword) {
            String query = keyword == null ? search.getText() : keyword;
            model.setRowCount(0);
            for (Movie movie : service.searchMovies(query)) {
                model.addRow(new Object[]{
                        movie.id(), movie.title(), movie.genre(), movie.director(), movie.starring(),
                        movie.durationText(), movie.format(), String.format("%.1f", movie.rating()),
                        movie.releaseDate()
                });
            }
            tableCard.sync("暂无影片", query.isBlank() ? "影片库还是空的" : "没有与「" + query + "」匹配的影片");
        }

        private void addMovie() {
            JTextField title = field();
            JTextField genre = field();
            JTextField format = field("2D");
            JTextField director = field();
            JTextField starring = field();
            JTextField minutes = field("100");
            JTextField release = field("2026-12-01");
            JTextField rating = field("8.0");
            boolean ok = Ui.showFormDialog(this, "新增影片",
                    formGrid(new String[]{"片名", "类型", "格式", "导演", "主演", "时长（分钟）", "上映日期", "评分"},
                            new JComponent[]{title, genre, format, director, starring,
                                    minutes, release, rating}), 560, 440);
            if (!ok) {
                return;
            }
            try {
                Movie movie = new Movie("", title.getText().trim(), genre.getText().trim(),
                        director.getText().trim(), starring.getText().trim(),
                        Integer.parseInt(minutes.getText().trim()), format.getText().trim(),
                        release.getText().trim(), "由管理员录入的演示影片。",
                        Double.parseDouble(rating.getText().trim()), 0);
                service.addMovie(movie);
                refresh("");
            } catch (DemoException | NumberFormatException ex) {
                Ui.error(this, "新增失败", ex instanceof DemoException
                        ? ex.getMessage() : "时长和评分必须是数字");
            }
        }

        private void editMovie() {
            Movie movie = selectedMovie();
            if (movie == null) {
                return;
            }
            JTextField title = field(movie.title());
            JTextField genre = field(movie.genre());
            JTextField format = field(movie.format());
            JTextField director = field(movie.director());
            JTextField starring = field(movie.starring());
            JTextField minutes = field(String.valueOf(movie.durationMinutes()));
            JTextField release = field(movie.releaseDate());
            JTextField rating = field(String.valueOf(movie.rating()));
            boolean ok = Ui.showFormDialog(this, "编辑影片",
                    formGrid(new String[]{"片名", "类型", "格式", "导演", "主演", "时长（分钟）", "上映日期", "评分"},
                            new JComponent[]{title, genre, format, director, starring,
                                    minutes, release, rating}), 560, 440);
            if (!ok) {
                return;
            }
            try {
                Movie updated = new Movie(movie.id(), title.getText().trim(), genre.getText().trim(),
                        director.getText().trim(), starring.getText().trim(),
                        Integer.parseInt(minutes.getText().trim()), format.getText().trim(),
                        release.getText().trim(), movie.synopsis(),
                        Double.parseDouble(rating.getText().trim()), movie.posterPalette());
                service.updateMovie(updated);
                refresh("");
            } catch (DemoException | NumberFormatException ex) {
                Ui.error(this, "保存失败", ex instanceof DemoException
                        ? ex.getMessage() : "时长和评分必须是数字");
            }
        }

        private void deleteMovie() {
            Movie movie = selectedMovie();
            if (movie == null || !Ui.confirm(this, "删除影片", "确定删除《" + movie.title() + "》吗？")) {
                return;
            }
            try {
                service.deleteMovie(movie.id());
                refresh("");
            } catch (DemoException ex) {
                Ui.error(this, "删除失败", ex.getMessage());
            }
        }

        private Movie selectedMovie() {
            int row = table.getSelectedRow();
            if (row < 0) {
                Ui.info(this, "影片管理", "请先在列表中选择一部影片");
                return null;
            }
            String id = String.valueOf(model.getValueAt(table.convertRowIndexToModel(row), 0));
            return service.movie(id);
        }
    }

    private final class HallModule extends JPanel implements ModulePanel {
        private final JTable table = new JTable();
        private final DefaultTableModel model = new DefaultTableModel(
                new Object[]{"编号", "名称", "规格", "行", "列"}, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };
        private final Ui.TableCard tableCard;
        private final JTextField search = new JTextField(16);

        private HallModule() {
            setLayout(new BorderLayout());
            setBackground(Ui.CANVAS);
            add(toolbar("影厅管理", HallModule.this, "新增影厅", this::addHall,
                    new Action("编辑", this::editHall),
                    new Action("删除", this::deleteHall)), BorderLayout.NORTH);
            table.setModel(model);
            Ui.configureTable(table);
            tableCard = new Ui.TableCard(table, Ui.scroll(table));
            add(tableCard, BorderLayout.CENTER);
        }

        @Override
        public void refresh(String keyword) {
            String query = keyword == null ? "" : keyword.trim();
            model.setRowCount(0);
            for (CinemaHall hall : service.halls()) {
                if (!query.isEmpty() && !(hall.name().contains(query) || hall.specs().contains(query))) {
                    continue;
                }
                model.addRow(new Object[]{
                        hall.id(), hall.name(), hall.specs(), hall.rows(), hall.cols()
                });
            }
            tableCard.sync("暂无影厅", query.isEmpty() ? "还没有添加任何影厅" : "没有与「" + query + "」匹配的影厅");
        }

        private void addHall() {
            JTextField name = field();
            JTextField specs = field("标准");
            JTextField rows = field("6");
            JTextField cols = field("10");
            boolean ok = Ui.showFormDialog(this, "新增影厅",
                    formGrid(new String[]{"名称", "规格", "行数", "列数"},
                            new JComponent[]{name, specs, rows, cols}), 440, 300);
            if (!ok) {
                return;
            }
            try {
                service.addHall(new CinemaHall("", name.getText().trim(), specs.getText().trim(),
                        Integer.parseInt(rows.getText().trim()), Integer.parseInt(cols.getText().trim())));
                refresh("");
            } catch (DemoException | NumberFormatException ex) {
                Ui.error(this, "新增失败", ex instanceof DemoException
                        ? ex.getMessage() : "行数和列数必须是整数");
            }
        }

        private void editHall() {
            CinemaHall hall = selectedHall();
            if (hall == null) {
                return;
            }
            JTextField name = field(hall.name());
            JTextField specs = field(hall.specs());
            JTextField rows = field(String.valueOf(hall.rows()));
            JTextField cols = field(String.valueOf(hall.cols()));
            boolean ok = Ui.showFormDialog(this, "编辑影厅",
                    formGrid(new String[]{"名称", "规格", "行数", "列数"},
                            new JComponent[]{name, specs, rows, cols}), 440, 300);
            if (!ok) {
                return;
            }
            try {
                service.updateHall(new CinemaHall(hall.id(), name.getText().trim(), specs.getText().trim(),
                        Integer.parseInt(rows.getText().trim()), Integer.parseInt(cols.getText().trim())));
                refresh("");
            } catch (DemoException | NumberFormatException ex) {
                Ui.error(this, "保存失败", ex instanceof DemoException
                        ? ex.getMessage() : "行数和列数必须是整数");
            }
        }

        private void deleteHall() {
            CinemaHall hall = selectedHall();
            if (hall == null || !Ui.confirm(this, "删除影厅", "确定删除" + hall.name() + "吗？")) {
                return;
            }
            try {
                service.deleteHall(hall.id());
                refresh("");
            } catch (DemoException ex) {
                Ui.error(this, "删除失败", ex.getMessage());
            }
        }

        private CinemaHall selectedHall() {
            int row = table.getSelectedRow();
            if (row < 0) {
                Ui.info(this, "影厅管理", "请先在列表中选择一个影厅");
                return null;
            }
            String id = String.valueOf(model.getValueAt(table.convertRowIndexToModel(row), 0));
            return service.hall(id);
        }
    }

    private final class SessionModule extends JPanel implements ModulePanel {
        private final JTable table = new JTable();
        private final DefaultTableModel model = new DefaultTableModel(
                new Object[]{"编号", "影片", "影厅", "开始时间", "结束时间", "票价"}, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };
        private final Ui.TableCard tableCard;
        private final JTextField search = new JTextField(16);

        private SessionModule() {
            setLayout(new BorderLayout());
            setBackground(Ui.CANVAS);
            add(toolbar("场次管理", SessionModule.this, "新增场次", this::addSession,
                    new Action("编辑", this::editSession),
                    new Action("删除", this::deleteSession)), BorderLayout.NORTH);
            table.setModel(model);
            Ui.configureTable(table);
            tableCard = new Ui.TableCard(table, Ui.scroll(table));
            add(tableCard, BorderLayout.CENTER);
        }

        @Override
        public void refresh(String keyword) {
            String query = keyword == null ? "" : keyword.trim();
            model.setRowCount(0);
            for (ShowSession session : service.allSessions()) {
                Movie movie = service.movie(session.movieId());
                CinemaHall hall = service.hall(session.hallId());
                if (!query.isEmpty() && !movie.title().contains(query) && !hall.name().contains(query)) {
                    continue;
                }
                model.addRow(new Object[]{
                        session.id(), movie.title(), hall.name(), session.startTime(),
                        session.endTime(), Money.format(session.priceFen())
                });
            }
            tableCard.sync("暂无场次", query.isEmpty() ? "还没有排片，可点右上角新增场次" : "没有与「" + query + "」匹配的场次");
        }

        private void addSession() {
            JComboBox<String> movies = new JComboBox<>();
            for (Movie movie : service.movies()) {
                movies.addItem(movie.id() + " " + movie.title());
            }
            JComboBox<String> halls = new JComboBox<>();
            for (CinemaHall hall : service.halls()) {
                halls.addItem(hall.id() + " " + hall.name());
            }
            JTextField start = field(java.time.LocalDate.now().plusDays(2) + " 14:00");
            JTextField price = field("45");
            boolean ok = Ui.showFormDialog(this, "新增场次",
                    formGrid(new String[]{"影片", "影厅", "开始时间", "票价（元）"},
                            new JComponent[]{movies, halls, start, price}), 520, 300);
            if (!ok) {
                return;
            }
            try {
                String movieId = String.valueOf(movies.getSelectedItem()).split(" ")[0];
                String hallId = String.valueOf(halls.getSelectedItem()).split(" ")[0];
                service.addSession(movieId, hallId, start.getText().trim(),
                        (int) Math.round(Double.parseDouble(price.getText().trim()) * 100));
                refresh("");
            } catch (DemoException | NumberFormatException ex) {
                Ui.error(this, "新增失败", ex instanceof DemoException
                        ? ex.getMessage() : "票价必须是整数");
            }
        }

        private void editSession() {
            ShowSession session = selectedSession();
            if (session == null) {
                return;
            }
            Movie movie = service.movie(session.movieId());
            CinemaHall hall = service.hall(session.hallId());
            JComboBox<String> movies = new JComboBox<>();
            for (Movie candidate : service.movies()) {
                movies.addItem(candidate.id() + " " + candidate.title());
                if (candidate.id().equals(session.movieId())) {
                    movies.setSelectedIndex(movies.getItemCount() - 1);
                }
            }
            JComboBox<String> halls = new JComboBox<>();
            for (CinemaHall candidate : service.halls()) {
                halls.addItem(candidate.id() + " " + candidate.name());
                if (candidate.id().equals(session.hallId())) {
                    halls.setSelectedIndex(halls.getItemCount() - 1);
                }
            }
            JTextField start = field(session.startTime());
            JTextField price = field(String.format("%.2f", session.priceFen() / 100.0));
            boolean ok = Ui.showFormDialog(this, "编辑场次",
                    formGrid(new String[]{"影片", "影厅", "开始时间", "票价（元）"},
                            new JComponent[]{movies, halls, start, price}), 520, 300);
            if (!ok) {
                return;
            }
            try {
                String movieId = String.valueOf(movies.getSelectedItem()).split(" ")[0];
                String hallId = String.valueOf(halls.getSelectedItem()).split(" ")[0];
                service.updateSession(session.id(), movieId, hallId, start.getText().trim(),
                        (int) Math.round(Double.parseDouble(price.getText().trim()) * 100));
                refresh("");
            } catch (DemoException | NumberFormatException ex) {
                Ui.error(this, "保存失败", ex instanceof DemoException
                        ? ex.getMessage() : "票价必须是整数");
            }
        }

        private void deleteSession() {
            ShowSession session = selectedSession();
            if (session == null || !Ui.confirm(this, "删除场次", "确定删除该场次吗？")) {
                return;
            }
            try {
                service.deleteSession(session.id());
                refresh("");
            } catch (DemoException ex) {
                Ui.error(this, "删除失败", ex.getMessage());
            }
        }

        private ShowSession selectedSession() {
            int row = table.getSelectedRow();
            if (row < 0) {
                Ui.info(this, "场次管理", "请先在列表中选择一个场次");
                return null;
            }
            String id = String.valueOf(model.getValueAt(table.convertRowIndexToModel(row), 0));
            return service.session(id);
        }
    }

    private final class OrderModule extends JPanel implements ModulePanel {
        private final JTable table = new JTable();
        private final DefaultTableModel model = new DefaultTableModel(
                new Object[]{"订单号", "顾客", "下单时间", "票数", "实付", "会员", "状态"}, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };
        private final Ui.TableCard tableCard;
        private final JTextField search = new JTextField(16);

        private OrderModule() {
            setLayout(new BorderLayout());
            setBackground(Ui.CANVAS);
            add(toolbar("订单管理", OrderModule.this, "查看明细", this::detail,
                    new Action("退款/取消", this::cancel)), BorderLayout.NORTH);
            table.setModel(model);
            Ui.configureTable(table);
            tableCard = new Ui.TableCard(table, Ui.scroll(table));
            add(tableCard, BorderLayout.CENTER);
        }

        @Override
        public void refresh(String keyword) {
            String query = keyword == null ? "" : keyword.trim();
            model.setRowCount(0);
            for (TicketOrder order : service.allOrders()) {
                if (!query.isEmpty() && !order.id().contains(query) && !order.username().contains(query)) {
                    continue;
                }
                model.addRow(new Object[]{
                        order.id(), order.username(), order.createdAt(),
                        order.items().stream().mapToInt(item -> item.seats().size()).sum(),
                        Money.format(order.payableFen()), order.membershipLabel(),
                        Ui.statusText(order.status())
                });
            }
            tableCard.sync("暂无订单", query.isEmpty() ? "还没有产生任何订单" : "没有与「" + query + "」匹配的订单");
        }

        private void detail() {
            TicketOrder order = selectedOrder();
            if (order == null) {
                return;
            }
            StringBuilder text = new StringBuilder("订单：" + order.id() + "\n");
            text.append("顾客：" + order.username() + "\n");
            text.append("下单时间：" + order.createdAt() + "\n");
            text.append("状态：" + Ui.statusText(order.status()) + "\n\n");
            for (TicketOrderItem item : order.items()) {
                text.append(item.movieTitle()).append("\n");
                text.append(item.hallName()).append(" · ").append(item.startTime())
                        .append(" 至 ").append(item.endTime()).append("\n");
                text.append("座位：").append(String.join("、", item.seats())).append("\n\n");
            }
            text.append("原价：").append(Money.format(order.originalFen())).append("\n");
            text.append("优惠：-").append(Money.format(order.discountFen())).append("\n");
            text.append("实付：").append(Money.format(order.payableFen()));
            if (!order.ticketCode().isBlank()) {
                text.append("\n出票码：").append(order.ticketCode());
            }
            showText(this, "订单明细", text.toString());
        }

        private void cancel() {
            TicketOrder order = selectedOrder();
            if (order == null) {
                return;
            }
            if ("CANCELED".equals(order.status())) {
                Ui.info(this, "订单管理", "该订单已取消");
                return;
            }
            if (!Ui.confirm(this, "订单处理", "确定退款并取消订单 " + order.id() + " 吗？")) {
                return;
            }
            service.cancelOrder(order.id());
            refresh("");
        }

        private TicketOrder selectedOrder() {
            int row = table.getSelectedRow();
            if (row < 0) {
                Ui.info(this, "订单管理", "请先在列表中选择一个订单");
                return null;
            }
            String id = String.valueOf(model.getValueAt(table.convertRowIndexToModel(row), 0));
            return service.order(id);
        }
    }

    private JPanel toolbar(String title, ModulePanel module, String primaryText,
                           Runnable primaryAction, Action... actions) {
        JPanel bar = Ui.panel(Ui.SURFACE);
        bar.setLayout(new BorderLayout());
        bar.setBorder(BorderFactory.createMatteBorder(0, 0, 1, 0, Ui.LINE));
        JLabel heading = Ui.heading(title, 22);
        heading.setBorder(BorderFactory.createEmptyBorder(18, 26, 18, 0));
        bar.add(heading, BorderLayout.WEST);
        JPanel right = Ui.panel(Ui.SURFACE);
        right.setLayout(new FlowLayout(FlowLayout.RIGHT, 8, 16));
        JTextField query = new JTextField(14);
        styleInput(query);
        right.add(query);
        JButton find = Ui.secondary("查找");
        find.addActionListener(event -> module.refresh(query.getText()));
        right.add(find);
        for (Action action : actions) {
            JButton button = Ui.secondary(action.label());
            button.addActionListener(event -> action.run());
            right.add(button);
        }
        JButton primary = Ui.primary(primaryText);
        primary.addActionListener(event -> primaryAction.run());
        right.add(primary);
        bar.add(right, BorderLayout.EAST);
        return bar;
    }

    private record Action(String label, Runnable run) {
    }

    private static JTextField field() {
        JTextField field = new JTextField();
        styleInput(field);
        return field;
    }

    private static JTextField field(String value) {
        JTextField field = new JTextField(value);
        styleInput(field);
        return field;
    }

    private static void styleInput(JComponent component) {
        component.setPreferredSize(new Dimension(300, 36));
        component.setMaximumSize(new Dimension(300, 36));
        component.setFont(Ui.FONT);
        component.setForeground(Ui.INK);
        component.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(Ui.LINE),
                BorderFactory.createEmptyBorder(7, 10, 7, 10)));
        if (component instanceof JTextField text) {
            text.setColumns(18);
        }
    }

    private static JPanel formGrid(String[] labels, JComponent[] fields) {
        JPanel panel = Ui.panel(Ui.SURFACE);
        panel.setBorder(BorderFactory.createEmptyBorder(20, 22, 8, 22));
        panel.setLayout(new GridBagLayout());
        GridBagConstraints constraints = new GridBagConstraints();
        constraints.anchor = GridBagConstraints.WEST;
        constraints.insets = new Insets(7, 0, 7, 12);
        constraints.gridx = 0;
        for (int i = 0; i < labels.length; i++) {
            constraints.gridy = i;
            JLabel label = Ui.fieldLabel(labels[i]);
            label.setPreferredSize(new Dimension(90, 34));
            panel.add(label, constraints);
            constraints.gridx = 1;
            JComponent field = fields[i];
            field.setPreferredSize(new Dimension(340, 36));
            field.setMaximumSize(new Dimension(340, 36));
            panel.add(field, constraints);
            constraints.gridx = 0;
        }
        return panel;
    }

    private static void showText(Component parent, String title, String text) {
        JTextArea area = new JTextArea(text, 16, 50);
        area.setEditable(false);
        area.setFont(Ui.font(Font.PLAIN, 13));
        area.setBackground(Ui.SURFACE);
        area.setForeground(Ui.INK);
        area.setLineWrap(true);
        JScrollPane scroll = Ui.scroll(area);
        scroll.setPreferredSize(new Dimension(540, 340));
        Ui.showFormDialog(parent, title, scroll, 560, 360);
    }
}
