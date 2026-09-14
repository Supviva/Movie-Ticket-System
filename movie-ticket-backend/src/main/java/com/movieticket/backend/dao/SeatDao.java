package com.movieticket.backend.dao;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.TreeSet;

/**
 * 座位与占座数据访问对象。
 *
 * <p>「已占座位」的口径是业务核心：<b>未支付与已支付订单都占座，取消后释放</b>。
 * 这个口径写成了数据库视图 {@code v_sold_seats}，本类直接查询该视图，
 * 保证与数据库层的定义永远一致，不会出现两处逻辑打架。</p>
 */
public class SeatDao {

    /**
     * 查询某场次已被占用的座位。
     *
     * @return 已排序的座位集合，如 [A1, A5, B3]
     */
    public Set<String> findSoldSeats(Connection conn, String sessionId) throws SQLException {
        String sql = "SELECT seat_key FROM v_sold_seats WHERE session_id = ? ORDER BY seat_key";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, sessionId);
            try (ResultSet rs = ps.executeQuery()) {
                Set<String> result = new TreeSet<>();
                while (rs.next()) {
                    result.add(rs.getString(1));
                }
                return result;
            }
        }
    }

    /**
     * 判断指定座位中是否有已被占用的。
     *
     * <p>用 {@code IN} 占位符做一次性查询，并在 SQL 层加 {@code FOR UPDATE} 锁住相关行，
     * 这样「检查 + 下单」在同一事务内不会出现并发超卖。</p>
     */
    public Set<String> findConflictingSeats(Connection conn, String sessionId, Set<String> seats)
            throws SQLException {
        if (seats == null || seats.isEmpty()) {
            return Set.of();
        }
        String placeholders = String.join(",", java.util.Collections.nCopies(seats.size(), "?"));
        String sql = "SELECT seat_key FROM v_sold_seats WHERE session_id = ? AND seat_key IN ("
                + placeholders + ") FOR UPDATE";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, sessionId);
            int index = 2;
            for (String seat : seats) {
                ps.setString(index++, seat);
            }
            try (ResultSet rs = ps.executeQuery()) {
                Set<String> conflicts = new LinkedHashSet<>();
                while (rs.next()) {
                    conflicts.add(rs.getString(1));
                }
                return conflicts;
            }
        }
    }

    /** 取影厅座位总数，用于展示「剩余座位」。 */
    public int countSeats(Connection conn, String hallId) throws SQLException {
        String sql = "SELECT COUNT(*) FROM seat WHERE hall_id = ?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, hallId);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? rs.getInt(1) : 0;
            }
        }
    }
}
