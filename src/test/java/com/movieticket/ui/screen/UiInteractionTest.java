package com.movieticket.ui.screen;

import com.movieticket.demo.DemoMovieTicketService;
import com.movieticket.ui.component.NavIcon;
import com.movieticket.ui.component.Ui;
import org.junit.jupiter.api.Test;

import javax.swing.JComponent;
import javax.swing.JPanel;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;
import javax.swing.table.DefaultTableModel;
import java.awt.Color;
import java.awt.Component;
import java.awt.Container;
import java.util.ArrayList;
import java.util.List;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 前端 P0 改进的回归测试：回车提交、字段校验、导航选中态、表格空状态、小食区去伪交互。
 *
 * <p>这些改进此前在项目里完全缺失——表单只能点按钮、导航没有任何选中反馈、
 * 表格搜不到结果时是一片空白。测试按「改进点」组织，而不是按类组织。</p>
 */
class UiInteractionTest {

    // ------------------------------------------------------------------
    // P0-1 登录/注册：回车提交 + 字段级红框校验
    // ------------------------------------------------------------------

    /**
     * 用户名与密码输入框都必须绑定回车提交。
     *
     * <p>改之前只有 {@code JButton} 能提交，键盘用户敲回车毫无反应。
     * {@code JTextField.addActionListener} 就是回车（VK_ENTER）的回调，
     * 所以监听器数量可以作为「是否支持回车」的判据。</p>
     */
    @Test
    void loginFieldsSubmitOnEnter() throws Exception {
        SwingUtilities.invokeAndWait(() -> {
            LoginPanel panel = loginPanel();
            List<JTextField> inputs = textFields(panel);
            assertTrue(inputs.size() >= 7,
                    "登录页应有 2 个登录输入框 + 5 个注册输入框，实际 " + inputs.size());

            // 每个输入框都必须具备回车提交能力
            for (JTextField field : inputs) {
                assertTrue(field.getActionListeners().length > 0,
                        "输入框「" + field.getClass().getSimpleName() + "」未绑定回车提交");
            }
        });
    }

    /** 登录表单留空提交：两个输入框都变红，且提示两者都缺。 */
    @Test
    void emptyLoginMarksBothFieldsInvalid() throws Exception {
        SwingUtilities.invokeAndWait(() -> {
            LoginPanel panel = loginPanel();
            List<JTextField> inputs = textFields(panel);
            JTextField user = inputs.get(0);
            JTextField password = inputs.get(1);

            Color borderBefore = borderColor(user);
            pressEnter(user);
            assertNotEquals(borderBefore, borderColor(user), "用户名留空应被标记为红色");
            assertEquals(Ui.DANGER, borderColor(user));
            assertEquals(Ui.DANGER, borderColor(password), "密码留空也应被标记为红色");
            assertEquals("请输入用户名和密码", messageOf(panel));
        });
    }

    /** 只填用户名、密码留空：仅密码框变红，提示为「请输入密码」。 */
    @Test
    void onlyPasswordMissingMarksPasswordAlone() throws Exception {
        SwingUtilities.invokeAndWait(() -> {
            LoginPanel panel = loginPanel();
            List<JTextField> inputs = textFields(panel);
            JTextField user = inputs.get(0);
            JTextField password = inputs.get(1);
            user.setText("customer");

            pressEnter(user);
            assertNotEquals(Ui.DANGER, borderColor(user), "已填写的用户名不应被标红");
            assertEquals(Ui.DANGER, borderColor(password), "缺失的密码应被标红");
            assertEquals("请输入密码", messageOf(panel));
        });
    }

    /** 校验失败后再成功提交，红框必须被清掉（不能残留上一次的错误标记）。 */
    @Test
    void invalidMarkIsClearedWhenInputBecomesValid() throws Exception {
        SwingUtilities.invokeAndWait(() -> {
            LoginPanel panel = loginPanel();
            List<JTextField> inputs = textFields(panel);
            JTextField user = inputs.get(0);
            JTextField password = inputs.get(1);

            pressEnter(user);
            assertEquals(Ui.DANGER, borderColor(user));

            user.setText("customer");
            password.setText("123456");
            pressEnter(password);

            assertNotEquals(Ui.DANGER, borderColor(user), "合法输入后红框应被清除");
            assertNotEquals(Ui.DANGER, borderColor(password), "合法输入后红框应被清除");
        });
    }

    /** 注册表单按「用户名→姓名→手机号→密码」顺序逐项拦截，且只标红当前缺的那一项。 */
    @Test
    void registerValidatesFieldsInOrder() throws Exception {
        SwingUtilities.invokeAndWait(() -> {
            LoginPanel panel = loginPanel();
            List<JTextField> all = textFields(panel);
            JTextField user = all.get(2);
            JTextField name = all.get(3);
            JTextField phone = all.get(4);
            JTextField password = all.get(5);
            JTextField confirm = all.get(6);

            pressEnter(user);
            assertEquals("请输入用户名", registerMessageOf(panel));
            assertEquals(Ui.DANGER, borderColor(user));
            assertNotEquals(Ui.DANGER, borderColor(name), "未轮到的字段不应被标红");

            user.setText("newbie");
            pressEnter(user);
            assertEquals("请输入姓名", registerMessageOf(panel));
            assertEquals(Ui.DANGER, borderColor(name));
            assertNotEquals(Ui.DANGER, borderColor(user), "已填字段的红框应被清除");

            name.setText("新用户");
            pressEnter(name);
            assertEquals("请输入手机号", registerMessageOf(panel));
            assertEquals(Ui.DANGER, borderColor(phone));

            phone.setText("13900000000");
            pressEnter(phone);
            assertEquals("请输入密码", registerMessageOf(panel));
            assertEquals(Ui.DANGER, borderColor(password));

            password.setText("123456");
            confirm.setText("654321");
            pressEnter(password);
            assertEquals("两次输入的密码不一致", registerMessageOf(panel));
            assertEquals(Ui.DANGER, borderColor(password));
            assertEquals(Ui.DANGER, borderColor(confirm), "两次不一致时两个密码框都应标红");
        });
    }

    /** 注册信息全部合法时应真的创建账号并回调登录成功。 */
    @Test
    void validRegisterCreatesAccountAndAuthenticates() throws Exception {
        SwingUtilities.invokeAndWait(() -> {
            DemoMovieTicketService service = new DemoMovieTicketService();
            List<String> loggedIn = new ArrayList<>();
            LoginPanel panel = new LoginPanel(service, account -> loggedIn.add(account.username()));
            List<JTextField> all = textFields(panel);
            all.get(2).setText("newbie");
            all.get(3).setText("新用户");
            all.get(4).setText("13900000000");
            all.get(5).setText("123456");
            all.get(6).setText("123456");

            pressEnter(all.get(2));
            assertEquals(List.of("newbie"), loggedIn, "注册成功应回调登录");
            assertNotNull(service.login("newbie", "123456"));
        });
    }

    // ------------------------------------------------------------------
    // P0-2 导航选中态
    // ------------------------------------------------------------------

    /** 顾客端初始应停在「影片」；切到订单/会员/购物车后选中项必须跟着走。 */
    @Test
    void customerNavFollowsCurrentPage() throws Exception {
        SwingUtilities.invokeAndWait(() -> {
            DemoMovieTicketService service = new DemoMovieTicketService();
            CustomerPanel panel = new CustomerPanel(
                    service, service.login("customer", "123456"), () -> {
            });

            List<JPanel> navItems = navItemPanels(panel);
            assertEquals(5, navItems.size(), "顶栏应有 影片/订单/会员/购物车/退出 五项");

            // 首页：只有「影片」是主色 + 浅橙底
            assertSelected(navItems.get(0), "影片");
            assertUnselected(navItems.get(1));
            assertUnselected(navItems.get(2));
            assertUnselected(navItems.get(3));

            panel.showOrdersForTest();
            assertSelected(navItems.get(1), "我的订单");
            assertUnselected(navItems.get(0));

            panel.showMembershipForTest();
            assertSelected(navItems.get(2), "我的会员");

            panel.showCartForTest();
            assertSelected(navItems.get(3), "购物车");
        });
    }

    /** 悬停只加一层极淡的白色蒙版、不改文字色；选中项在鼠标划过时不能被悬停态覆盖。 */
    @Test
    void navHoverDoesNotStealSelection() throws Exception {
        SwingUtilities.invokeAndWait(() -> {
            DemoMovieTicketService service = new DemoMovieTicketService();
            CustomerPanel panel = new CustomerPanel(
                    service, service.login("customer", "123456"), () -> {
            });
            List<JPanel> navItems = navItemPanels(panel);
            JPanel home = navItems.get(0);
            JPanel orders = navItems.get(1);

            Color homeBg = home.getBackground();
            Color homeFg = navLabel(navItems, 0).getForeground();

            // 悬停未选中项：只叠一层淡白蒙版，不能变成选中态的浅橙底
            dispatchMouse(orders, true);
            assertNotEquals(Ui.PRIMARY_SOFT, orders.getBackground(),
                    "悬停项不应使用选中态的浅橙底色");
            assertTrue(orders.isOpaque(), "悬停时应叠一层蒙版");
            assertEquals(14, orders.getBackground().getAlpha(), "蒙版应是极淡的半透明白");
            dispatchMouse(orders, false);
            assertFalse(orders.isOpaque(), "鼠标移出后应恢复透明");

            // 悬停已选中项：选中底色与主色文字都必须保持
            dispatchMouse(home, true);
            assertTrue(home.isOpaque(), "选中项被悬停后仍应保持高亮底色");
            assertEquals(homeBg, home.getBackground());
            assertEquals(Ui.PRIMARY, navLabel(navItems, 0).getForeground(),
                    "选中项被悬停后文字仍应是主色");

            // 鼠标移出选中项：不能被清成普通态
            dispatchMouse(home, false);
            assertSelected(home, "影片");
            assertEquals(homeFg, navLabel(navItems, 0).getForeground());
        });
    }

    /** 后台侧栏初始选中第一项，点击其它模块后指示状态随之迁移。 */
    @Test
    void adminSidebarHighlightsCurrentModule() throws Exception {
        SwingUtilities.invokeAndWait(() -> {
            DemoMovieTicketService service = new DemoMovieTicketService();
            AdminPanel panel = new AdminPanel(
                    service, service.login("admin", "123456"), () -> {
            });

            List<JComponent> navItems = adminNavItems(panel);
            assertEquals(5, navItems.size(), "后台侧栏应有五个模块入口");

            assertTrue(isAdminItemHighlighted(navItems.get(0)), "初始应高亮「顾客管理」");
            assertFalse(isAdminItemHighlighted(navItems.get(1)));

            panel.selectModuleForTest("orders");
            assertTrue(isAdminItemHighlighted(navItems.get(4)), "切到订单管理后应高亮第五项");
            assertFalse(isAdminItemHighlighted(navItems.get(0)), "旧选中项应被取消高亮");

            panel.selectModuleForTest("halls");
            assertTrue(isAdminItemHighlighted(navItems.get(2)));
            assertFalse(isAdminItemHighlighted(navItems.get(4)));
        });
    }

    // ------------------------------------------------------------------
    // P0-3 表格空状态
    // ------------------------------------------------------------------

    /** 有数据时显示表格、无数据时切到空状态面板。 */
    @Test
    void tableCardSwitchesBetweenTableAndEmptyState() throws Exception {
        SwingUtilities.invokeAndWait(() -> {
            JTable table = new JTable(new DefaultTableModel(new Object[]{"A"}, 0));
            Ui.TableCard card = new Ui.TableCard(table, Ui.scroll(table));

            ((DefaultTableModel) table.getModel()).addRow(new Object[]{"1"});
            card.sync("暂无数据", "没有内容");
            assertTrue(visibleCardIsTable(card), "有数据时应显示表格");

            ((DefaultTableModel) table.getModel()).setRowCount(0);
            card.sync("暂无数据", "没有内容");
            assertFalse(visibleCardIsTable(card), "无数据时应切到空状态");
            assertNotNull(findEmptyStateText(card, "暂无数据"), "空状态应显示主文案");
            assertNotNull(findEmptyStateText(card, "没有内容"), "空状态应显示副文案");
        });
    }

    /** 空状态面板不应包含任何按钮（避免又造出一个死交互）。 */
    @Test
    void emptyStateWithoutActionHasNoButton() throws Exception {
        SwingUtilities.invokeAndWait(() -> {
            JPanel panel = Ui.emptyState("暂无数据", "换个关键词试试", null);
            assertTrue(findComponents(panel, javax.swing.JButton.class).isEmpty(),
                    "未传入 action 时不应出现按钮");
        });
    }

    /** 后台五个模块在搜索无结果时都要走空状态，不能出现空白表体。 */
    @Test
    void adminModulesShowEmptyStateWhenSearchMisses() throws Exception {
        SwingUtilities.invokeAndWait(() -> {
            DemoMovieTicketService service = new DemoMovieTicketService();
            AdminPanel panel = new AdminPanel(
                    service, service.login("admin", "123456"), () -> {
            });

            String[] cards = {"customers", "movies", "halls", "sessions", "orders"};
            for (String card : cards) {
                panel.selectModuleForTest(card);
                panel.refreshCurrentForTest("zzz-no-match-zzz");
                assertFalse(panel.currentModuleShowsTable(),
                        "模块 " + card + " 搜索无结果时应显示空状态而不是空白表格");
            }
        });
    }

    // ------------------------------------------------------------------
    // P0-4 小食区去伪交互
    // ------------------------------------------------------------------

    /**
     * 小食卖品卡里不能有「购买」按钮。
     *
     * <p>演示数据里小食并没有接入购物车/订单，之前的按钮点了只弹一句提示，
     * 是典型的伪交互。现在改成纯展示规格文案。</p>
     */
    @Test
    void snackCardsContainNoBuyButton() throws Exception {
        SwingUtilities.invokeAndWait(() -> {
            DemoMovieTicketService service = new DemoMovieTicketService();
            CustomerPanel panel = new CustomerPanel(
                    service, service.login("customer", "123456"), () -> {
            });

            List<javax.swing.JButton> buttons = findComponents(panel, javax.swing.JButton.class);
            for (javax.swing.JButton button : buttons) {
                assertFalse("购买".equals(button.getText()),
                        "小食区不应再有「购买」按钮");
            }
            // 规格文案应保留，卡片仍有信息量
            assertTrue(findLabels(panel, "大桶 · 约 120g"), "小食卡应展示规格文案");
            assertTrue(findLabels(panel, "经典 · 含面包"), "小食卡应展示规格文案");
        });
    }

    // ------------------------------------------------------------------
    // 订单闭环：未支付订单必须有「去支付」入口
    // ------------------------------------------------------------------

    /**
     * UNPAID 订单必须带「去支付」按钮，PAID/CANCELED 不带。
     *
     * <p>之前未支付订单只有「取消订单」——用户关掉结算页后这笔单就永远
     * 付不了了，只能重新选座下单，属于业务闭环断裂。</p>
     */
    @Test
    void unpaidOrderHasPayEntry() throws Exception {
        SwingUtilities.invokeAndWait(() -> {
            DemoMovieTicketService service = new DemoMovieTicketService();
            CustomerPanel panel = new CustomerPanel(
                    service, service.login("customer", "123456"), () -> {
            });

            // 造一笔真实未支付订单（座位在单场次内足够，无需轮转）
            var session = service.sessionsForMovie(service.movies().get(0).id()).get(0);
            var plan = service.seatPlan(session.id());
            String seat = freeSeat(plan);
            com.movieticket.model.TicketOrder unpaid =
                    service.buySeatsNow(panel.currentAccountForTest().id(), session.id(), java.util.Set.of(seat));
            assertEquals("UNPAID", unpaid.status(), "前置条件：新订单应为未支付");

            List<javax.swing.JButton> buttons = findComponents(panel.orderRowForTest(unpaid), javax.swing.JButton.class);
            assertTrue(buttons.stream().anyMatch(button -> "去支付".equals(button.getText())),
                    "未支付订单应显示「去支付」按钮");
            assertTrue(buttons.stream().anyMatch(button -> "取消订单".equals(button.getText())),
                    "未支付订单仍应保留「取消订单」");
            assertTrue(buttons.stream().anyMatch(button -> "查看明细".equals(button.getText())));

            // 支付后同一订单行不应再有去支付入口
            service.payOrder(unpaid.id());
            List<javax.swing.JButton> paidButtons =
                    findComponents(panel.orderRowForTest(service.ordersForUser(panel.currentAccountForTest().id())
                            .stream().filter(order -> order.id().equals(unpaid.id())).findFirst().orElseThrow()), javax.swing.JButton.class);
            assertFalse(paidButtons.stream().anyMatch(button -> "去支付".equals(button.getText())),
                    "已支付订单不应再显示「去支付」");
        });
    }

    private static String freeSeat(com.movieticket.model.SeatPlan plan) {
        for (int row = 0; row < plan.hall().rows(); row++) {
            for (int col = 1; col <= plan.hall().cols(); col++) {
                String candidate = (char) ('A' + row) + String.valueOf(col);
                if (!plan.soldSeats().contains(candidate)) {
                    return candidate;
                }
            }
        }
        throw new AssertionError("场次已无空座");
    }

    // ------------------------------------------------------------------
    // 选座页：滚动容器 / 合计金额 / 已售座行内提示
    // ------------------------------------------------------------------

    /**
     * 选座页必须包在滚动容器里（后台可加任意大的影厅），选中座位后
     * 出现合计金额；点击已售座通过行内红字提示而非弹窗。
     */
    @Test
    void seatViewScrollableWithTotalAndHint() throws Exception {
        SwingUtilities.invokeAndWait(() -> {
            DemoMovieTicketService service = new DemoMovieTicketService();
            CustomerPanel panel = new CustomerPanel(
                    service, service.login("customer", "123456"), () -> {
            });
            var session = service.sessionsForMovie(service.movies().get(0).id()).get(0);
            panel.showSeatForTest(session);

            // 座位图必须位于 JScrollPane 内（防大影厅裁切）
            com.movieticket.ui.component.SeatMapPanel seatMap =
                    findComponents(panel, com.movieticket.ui.component.SeatMapPanel.class).get(0);
            assertTrue(hasAncestor(seatMap, javax.swing.JScrollPane.class),
                    "座位图应包在滚动容器里");

            // 选一个座位：出现合计金额
            var plan = service.seatPlan(session.id());
            int[] free = freeSeatPos(plan);
            pressSeat(seatMap, free[0], free[1]);
            assertTrue(findLabels(panel, "合计 " + com.movieticket.model.Money.format(
                            session.priceFen())),
                    "选座后应显示合计金额");
            assertTrue(findLabels(panel, "已选 1 张 · A" + (free[1] + 1)));

            // 点已售座：selectionLabel 变红提示（行内，不是弹窗）
            int[] sold = soldSeatPos(plan);
            if (sold != null) {
                pressSeat(seatMap, sold[0], sold[1]);
                javax.swing.JLabel selection = findComponents(panel, javax.swing.JLabel.class)
                        .stream()
                        .filter(label -> label.getText() != null && label.getText().contains("已售出"))
                        .findFirst().orElse(null);
                assertTrue(selection != null, "点已售座应出现行内提示");
                assertEquals(Ui.DANGER, selection.getForeground());
            }
        });
    }

    private static boolean hasAncestor(Component component, Class<?> type) {
        Container parent = component.getParent();
        while (parent != null) {
            if (type.isInstance(parent)) {
                return true;
            }
            parent = parent.getParent();
        }
        return false;
    }

    private static void pressSeat(com.movieticket.ui.component.SeatMapPanel seatMap, int row, int col) {
        java.awt.Point center = com.movieticket.ui.component.SeatMapPanel.seatCenter(row, col);
        java.awt.event.MouseEvent event = new java.awt.event.MouseEvent(seatMap,
                java.awt.event.MouseEvent.MOUSE_PRESSED, System.currentTimeMillis(), 0,
                center.x, center.y, 1, false);
        for (java.awt.event.MouseListener listener : seatMap.getMouseListeners()) {
            listener.mousePressed(event);
        }
    }

    /** 找一个空座（行列下标）。 */
    private static int[] freeSeatPos(com.movieticket.model.SeatPlan plan) {
        for (int row = 0; row < plan.hall().rows(); row++) {
            for (int col = 0; col < plan.hall().cols(); col++) {
                String seat = String.valueOf((char) ('A' + row)) + (col + 1);
                if (!plan.soldSeats().contains(seat)) {
                    return new int[]{row, col};
                }
            }
        }
        throw new AssertionError("场次已无空座");
    }

    /** 找一个已售座（行列下标），没有返回 null。 */
    private static int[] soldSeatPos(com.movieticket.model.SeatPlan plan) {
        for (int row = 0; row < plan.hall().rows(); row++) {
            for (int col = 0; col < plan.hall().cols(); col++) {
                String seat = String.valueOf((char) ('A' + row)) + (col + 1);
                if (plan.soldSeats().contains(seat)) {
                    return new int[]{row, col};
                }
            }
        }
        return null;
    }

    // ------------------------------------------------------------------
    // 组件级：NavIcon 颜色可变 / 常量不再重复定义
    // ------------------------------------------------------------------

    /** NavIcon 必须支持运行时改色，否则导航选中态无法同时换图标与文字色。 */
    @Test
    void navIconColorIsMutable() throws Exception {
        SwingUtilities.invokeAndWait(() -> {
            NavIcon icon = new NavIcon(NavIcon.Kind.FILM, 20, Ui.INK_SOFT);
            assertEquals(Ui.INK_SOFT, icon.color());
            icon.setColor(Ui.PRIMARY);
            assertEquals(Ui.PRIMARY, icon.color());
            // null 应被忽略，避免把图标画成不可见
            icon.setColor(null);
            assertEquals(Ui.PRIMARY, icon.color());
        });
    }

    /** 历史别名必须仍与新常量同值，避免旧调用点行为悄悄变化。 */
    @Test
    void deprecatedColorAliasesStayInSync() {
        assertSame(Ui.PRIMARY, Ui.ORANGE, "ORANGE 应与 PRIMARY 同值");
        assertSame(Ui.LINE, Ui.DISABLED, "DISABLED 应与 LINE 同值");
        assertEquals(new Color(0xFF, 0xB4, 0x00), Ui.SCORE, "评分金色应统一到 Ui.SCORE");
    }

    // ------------------------------------------------------------------
    // 工具方法
    // ------------------------------------------------------------------

    private static LoginPanel loginPanel() {
        return new LoginPanel(new DemoMovieTicketService(), account -> {
        });
    }

    /** 按组件树顺序取出所有 JTextField（含 JPasswordField）。 */
    private static List<JTextField> textFields(Container root) {
        return findComponents(root, JTextField.class);
    }

    private static <T> List<T> findComponents(Container root, Class<T> type) {
        List<T> found = new ArrayList<>();
        collect(root, type, found);
        return found;
    }

    private static <T> void collect(Container root, Class<T> type, List<T> found) {
        for (Component child : root.getComponents()) {
            if (type.isInstance(child)) {
                found.add(type.cast(child));
            }
            if (child instanceof Container container) {
                collect(container, type, found);
            }
        }
    }

    private static boolean findLabels(Container root, String text) {
        return !findComponents(root, javax.swing.JLabel.class).stream()
                .filter(label -> text.equals(label.getText()))
                .toList().isEmpty();
    }

    /** 触发输入框的回车动作（等价于键盘敲 Enter）。 */
    private static void pressEnter(JTextField field) {
        for (java.awt.event.ActionListener listener : field.getActionListeners()) {
            listener.actionPerformed(new java.awt.event.ActionEvent(
                    field, java.awt.event.ActionEvent.ACTION_PERFORMED, field.getText()));
        }
    }

    private static void dispatchMouse(JComponent component, boolean enter) {
        java.awt.event.MouseEvent event = new java.awt.event.MouseEvent(
                component, enter ? java.awt.event.MouseEvent.MOUSE_ENTERED
                : java.awt.event.MouseEvent.MOUSE_EXITED,
                System.currentTimeMillis(), 0, 1, 1, 0, false);
        for (java.awt.event.MouseListener listener : component.getMouseListeners()) {
            if (enter) {
                listener.mouseEntered(event);
            } else {
                listener.mouseExited(event);
            }
        }
    }

    /** 取输入框的外层描边颜色（校验失败时为 Ui.DANGER）。 */
    private static Color borderColor(JComponent field) {
        javax.swing.border.Border border = field.getBorder();
        if (border instanceof javax.swing.border.CompoundBorder compound) {
            border = compound.getOutsideBorder();
        }
        if (border instanceof javax.swing.border.LineBorder line) {
            return line.getLineColor();
        }
        return null;
    }

    /** 页面底部的提示文案。 */
    private static String messageOf(Container root) {
        return firstLabelWithForeground(root, Ui.DANGER);
    }

    private static String registerMessageOf(Container root) {
        return firstLabelWithForeground(root, Ui.DANGER);
    }

    /** 取第一个「前景色为指定颜色且文字非空白」的标签文本。 */
    private static String firstLabelWithForeground(Container root, Color color) {
        for (javax.swing.JLabel label : findComponents(root, javax.swing.JLabel.class)) {
            if (color.equals(label.getForeground())
                    && label.getText() != null && !label.getText().isBlank()) {
                return label.getText();
            }
        }
        return null;
    }

    @SuppressWarnings("unchecked")
    private static List<JPanel> navItemPanels(CustomerPanel panel) {
        try {
            java.lang.reflect.Field field = CustomerPanel.class.getDeclaredField("navItems");
            field.setAccessible(true);
            return (List<JPanel>) field.get(panel);
        } catch (ReflectiveOperationException ex) {
            throw new AssertionError("无法读取顾客端导航项", ex);
        }
    }

    /** 导航项里的文字标签（用来断言文字颜色随状态变化）。 */
    private static javax.swing.JLabel navLabel(List<JPanel> navItems, int index) {
        List<javax.swing.JLabel> labels = findComponents(navItems.get(index), javax.swing.JLabel.class);
        assertFalse(labels.isEmpty(), "导航项 " + index + " 里应有文字标签");
        return labels.get(0);
    }

    @SuppressWarnings("unchecked")
    private static List<JComponent> adminNavItems(AdminPanel panel) {
        try {
            java.lang.reflect.Field field = AdminPanel.class.getDeclaredField("navItems");
            field.setAccessible(true);
            java.util.List<?> raw = (java.util.List<?>) field.get(panel);
            List<JComponent> items = new ArrayList<>();
            for (Object item : raw) {
                items.add((JComponent) item);
            }
            return items;
        } catch (ReflectiveOperationException ex) {
            throw new AssertionError("无法读取后台侧栏项", ex);
        }
    }

    private static boolean isAdminItemHighlighted(JComponent item) {
        try {
            java.lang.reflect.Field field = item.getClass().getDeclaredField("highlighted");
            field.setAccessible(true);
            return (boolean) field.get(item);
        } catch (ReflectiveOperationException ex) {
            throw new AssertionError("无法读取侧栏高亮状态", ex);
        }
    }

    private static void assertSelected(JPanel item, String what) {
        assertTrue(item.isOpaque(), "选中项「" + what + "」应有高亮底色");
        assertEquals(Ui.PRIMARY_SOFT, item.getBackground(), "选中项「" + what + "」底色应为浅橙");
    }

    private static void assertUnselected(JPanel item) {
        assertFalse(item.isOpaque(), "未选中项不应有高亮底色");
    }

    /** CardLayout 当前显示的是不是表格（相对空状态）。 */
    private static boolean visibleCardIsTable(Ui.TableCard card) {
        for (Component child : card.getComponents()) {
            if (child.isVisible()) {
                return child instanceof javax.swing.JScrollPane;
            }
        }
        throw new AssertionError("TableCard 里没有任何可见子组件");
    }

    private static javax.swing.JLabel findEmptyStateText(Container root, String text) {
        for (javax.swing.JLabel label : findComponents(root, javax.swing.JLabel.class)) {
            if (text.equals(label.getText())) {
                return label;
            }
        }
        return null;
    }
}
