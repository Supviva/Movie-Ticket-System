package com.movieticket.backend.dao;

import com.movieticket.backend.model.TicketOrder;
import com.movieticket.backend.model.TicketOrderItem;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

/**
 * 订单数据访问对象。
 *
 * <p>订单由「主表 + 明细表」两部分组成：主表一行一个订单，明细表一行一个座位。
 * 写入用同一个事务保证两表一致；读取时一次联表查询再在内存聚合，
 * 避免为每个订单单独查明细（N+1 查询）。</p>
 */
public class OrderDao {

    private static final DateTimeFormatter FMT =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss", Locale.ROOT);

    /** 插入订单主表。 */
    public void insertOrder(Connection conn, TicketOrder order) throws SQLException {
        String sql = "INSERT INTO ticket_order (id, account_id, username, original_fen, discount_fen,"
                + " payable_fen, membership_label, status, ticket_code, created_at, paid_at)"
                + " VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, order.id());
            ps.setString(2, order.userId());
            ps.setString(3, order.username());
            ps.setInt(4, order.originalFen());
            ps.setInt(5, order.discountFen());
            ps.setInt(6, order.payableFen());
            ps.setString(7, order.membershipLabel());
            ps.setString(8, order.status());
            ps.setString(9, order.ticketCode());
            ps.setTimestamp(10, toTimestamp(order.createdAt()));
            ps.setTimestamp(11, TicketOrder.STATUS_PAID.equals(order.status())
                    ? toTimestamp(order.createdAt()) : null);
            ps.executeUpdate();
        }
    }

    /** 批量插入订单明细（座位级）。 */
    public void insertItems(Connection conn, String orderId, List<TicketOrderItem> items)
            throws SQLException {
        String sql = "INSERT INTO ticket_order_item (order_id, session_id, movie_title, hall_name,"
                + " start_time, end_time, unit_price_fen, seat_key) VALUES (?, ?, ?, ?, ?, ?, ?, ?)";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            for (TicketOrderItem item : items) {
                for (String seat : item.seats()) {
                    ps.setString(1, orderId);
                    ps.setString(2, item.sessionId());
                    ps.setString(3, item.movieTitle());
                    ps.setString(4, item.hallName());
                    ps.setTimestamp(5, Timestamp.valueOf(ShowSessionDao.parse(item.startTime())));
                    ps.setTimestamp(6, Timestamp.valueOf(ShowSessionDao.parse(item.endTime())));
                    ps.setInt(7, item.unitPriceFen());
                    ps.setString(8, seat);
                    ps.addBatch();
                }
            }
            ps.executeBatch();
        }
    }

    /** 更新订单状态与出票码（支付 / 取消都用它）。 */
    public int updateStatus(Connection conn, String orderId, String status, String ticketCode)
            throws SQLException {
        String sql = "UPDATE ticket_order SET status = ?, ticket_code = ?,"
                + " paid_at = CASE WHEN ? = 'PAID' THEN NOW() ELSE paid_at END WHERE id = ?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, status);
            ps.setString(2, ticketCode);
            ps.setString(3, status);
            ps.setString(4, orderId);
            return ps.executeUpdate();
        }
    }

    public TicketOrder findById(Connection conn, String orderId) throws SQLException {
        String sql = baseSelect() + " WHERE o.id = ?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, orderId);
            List<TicketOrder> orders = readOrders(ps);
            return orders.isEmpty() ? null : orders.get(0);
        }
    }

    public List<TicketOrder> findByUser(Connection conn, String accountId) throws SQLException {
        String sql = baseSelect() + " WHERE o.account_id = ? ORDER BY o.created_at DESC, o.id";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, accountId);
            return readOrders(ps);
        }
    }

    public List<TicketOrder> findAll(Connection conn) throws SQLException {
        String sql = baseSelect() + " ORDER BY o.created_at DESC, o.id";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            return readOrders(ps);
        }
    }

    /**
     * 统计账号的累计消费金额（分），只算已支付订单。
     *
     * <p>会员等级完全由这个数推导，所以放在一条 SQL 里算，避免把全部订单读进内存。</p>
     */
    public int sumPaidAmount(Connection conn, String accountId) throws SQLException {
        String sql = "SELECT COALESCE(SUM(payable_fen), 0) FROM ticket_order"
                + " WHERE account_id = ? AND status = 'PAID'";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, accountId);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? rs.getInt(1) : 0;
            }
        }
    }

    /** 该账号已支付订单的累计节省金额（分）。 */
    public int sumPaidDiscount(Connection conn, String accountId) throws SQLException {
        String sql = "SELECT COALESCE(SUM(discount_fen), 0) FROM ticket_order"
                + " WHERE account_id = ? AND status = 'PAID'";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, accountId);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? rs.getInt(1) : 0;
            }
        }
    }

    /** 该账号已支付订单数。 */
    public int countPaidOrders(Connection conn, String accountId) throws SQLException {
        String sql = "SELECT COUNT(*) FROM ticket_order WHERE account_id = ? AND status = 'PAID'";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, accountId);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? rs.getInt(1) : 0;
            }
        }
    }

    /** 该账号已购票张数（按明细座位数）。 */
    public int countPaidSeats(Connection conn, String accountId) throws SQLException {
        String sql = "SELECT COUNT(i.id) FROM ticket_order_item i"
                + " JOIN ticket_order o ON o.id = i.order_id"
                + " WHERE o.account_id = ? AND o.status = 'PAID'";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, accountId);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? rs.getInt(1) : 0;
            }
        }
    }

    /** 取当前最大订单序号，用于生成下一个 Txxxxxx。 */
    public int maxOrderSequence(Connection conn) throws SQLException {
        String sql = "SELECT COALESCE(MAX(CAST(SUBSTRING(id, 2) AS UNSIGNED)), 1000)"
                + " FROM ticket_order WHERE id LIKE 'T%'";
        try (PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            return rs.next() ? rs.getInt(1) : 1000;
        }
    }

    private String baseSelect() {
        return "SELECT o.id, o.account_id, o.username, o.original_fen, o.discount_fen,"
                + " o.payable_fen, o.membership_label, o.status, o.ticket_code, o.created_at,"
                + " i.session_id, i.movie_title, i.hall_name, i.start_time, i.end_time,"
                + " i.unit_price_fen, i.seat_key"
                + " FROM ticket_order o"
                + " LEFT JOIN ticket_order_item i ON i.order_id = o.id";
    }

    /**
     * 读取联表结果并聚合：同一订单的多个座位明细合并成一个
     * {@link TicketOrderItem}（按场次分组，座位合并为集合）。
     */
    private List<TicketOrder> readOrders(PreparedStatement ps) throws SQLException {
        Map<String, OrderBuilder> builders = new LinkedHashMap<>();
        try (ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                String orderId = rs.getString("id");
                OrderBuilder builder = builders.computeIfAbsent(orderId, id -> {
                    try {
                        return new OrderBuilder(
                                id,
                                rs.getString("account_id"),
                                rs.getString("username"),
                                rs.getInt("original_fen"),
                                rs.getInt("discount_fen"),
                                rs.getInt("payable_fen"),
                                rs.getString("membership_label"),
                                rs.getString("status"),
                                rs.getString("ticket_code"),
                                ShowSessionDao.format(rs.getTimestamp("created_at")));
                    } catch (SQLException e) {
                        throw new IllegalStateException("读取订单主表字段失败", e);
                    }
                });

                String sessionId = rs.getString("session_id");
                if (sessionId != null) {
                    builder.addSeat(
                            sessionId,
                            rs.getString("movie_title"),
                            rs.getString("hall_name"),
                            ShowSessionDao.format(rs.getTimestamp("start_time")),
                            ShowSessionDao.format(rs.getTimestamp("end_time")),
                            rs.getInt("unit_price_fen"),
                            rs.getString("seat_key"));
                }
            }
        }

        List<TicketOrder> orders = new ArrayList<>();
        for (OrderBuilder builder : builders.values()) {
            orders.add(builder.build());
        }
        return orders;
    }

    /**
     * 把 'yyyy-MM-dd HH:mm' 或 'yyyy-MM-dd HH:mm:ss' 文本转为 Timestamp。
     * 无法解析时退回当前时间，保证下单不会因为时间格式问题失败。
     */
    private Timestamp toTimestamp(String text) {
        if (text == null || text.isBlank()) {
            return Timestamp.valueOf(LocalDateTime.now());
        }
        String trimmed = text.trim();
        try {
            return Timestamp.valueOf(trimmed.length() > 16
                    ? LocalDateTime.parse(trimmed, FMT)
                    : ShowSessionDao.parse(trimmed));
        } catch (Exception e) {
            return Timestamp.valueOf(LocalDateTime.now());
        }
    }

    /** 订单聚合过程中的可变容器，读完即可变回不可变的 record。 */
    private static final class OrderBuilder {
        private final String id;
        private final String userId;
        private final String username;
        private final int originalFen;
        private final int discountFen;
        private final int payableFen;
        private final String membershipLabel;
        private final String status;
        private final String ticketCode;
        private final String createdAt;

        private final Map<String, ItemBuilder> items = new LinkedHashMap<>();

        OrderBuilder(String id, String userId, String username, int originalFen, int discountFen,
                     int payableFen, String membershipLabel, String status, String ticketCode,
                     String createdAt) {
            this.id = id;
            this.userId = userId;
            this.username = username;
            this.originalFen = originalFen;
            this.discountFen = discountFen;
            this.payableFen = payableFen;
            this.membershipLabel = membershipLabel;
            this.status = status;
            this.ticketCode = ticketCode;
            this.createdAt = createdAt;
        }

        void addSeat(String sessionId, String movieTitle, String hallName, String startTime,
                     String endTime, int unitPriceFen, String seatKey) {
            items.computeIfAbsent(sessionId,
                            k -> new ItemBuilder(sessionId, movieTitle, hallName, startTime, endTime, unitPriceFen))
                    .seats.add(seatKey);
        }

        TicketOrder build() {
            List<TicketOrderItem> list = new ArrayList<>();
            for (ItemBuilder item : items.values()) {
                list.add(new TicketOrderItem(item.sessionId, item.movieTitle, item.hallName,
                        item.startTime, item.endTime, item.unitPriceFen,
                        new LinkedHashSet<>(item.seats)));
            }
            return new TicketOrder(id, userId, username, list, originalFen, discountFen, payableFen,
                    membershipLabel, status, ticketCode, createdAt);
        }
    }

    private static final class ItemBuilder {
        private final String sessionId;
        private final String movieTitle;
        private final String hallName;
        private final String startTime;
        private final String endTime;
        private final int unitPriceFen;
        private final Set<String> seats = new LinkedHashSet<>();

        ItemBuilder(String sessionId, String movieTitle, String hallName, String startTime,
                    String endTime, int unitPriceFen) {
            this.sessionId = sessionId;
            this.movieTitle = movieTitle;
            this.hallName = hallName;
            this.startTime = startTime;
            this.endTime = endTime;
            this.unitPriceFen = unitPriceFen;
        }
    }
}
