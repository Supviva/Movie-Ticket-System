package com.movieticket.backend.dao;

import com.movieticket.backend.model.CartItem;
import com.movieticket.backend.model.Movie;
import com.movieticket.backend.model.ShowSession;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

/**
 * 购物车数据访问对象。
 *
 * <p>数据库按「用户 × 场次 × 座位」一行存储，读取时按场次聚合成 {@link CartItem}
 * （一个条目含多个座位），以便与前端模型保持一致。</p>
 */
public class CartDao {

    /** 加入购物车（座位级一行）。重复加入同一座位由唯一键拦下，此处用 INSERT IGNORE 静默跳过。 */
    public void addSeats(Connection conn, String accountId, String sessionId, Set<String> seats)
            throws SQLException {
        String sql = "INSERT IGNORE INTO cart (account_id, session_id, seat_key) VALUES (?, ?, ?)";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            for (String seat : seats) {
                ps.setString(1, accountId);
                ps.setString(2, sessionId);
                ps.setString(3, seat);
                ps.addBatch();
            }
            ps.executeBatch();
        }
    }

    /**
     * 查询购物车，按场次聚合成条目，并补齐片名/影厅/时间/单价等展示信息。
     *
     * <p>放在 SQL 里一次联表查出，避免逐个条目再查一次电影与影厅（N+1 查询）。</p>
     */
    public List<CartItem> findItems(Connection conn, String accountId) throws SQLException {
        String sql = "SELECT c.session_id, c.seat_key,"
                + " s.price_fen, s.start_time, s.end_time,"
                + " m.title AS movie_title, h.name AS hall_name"
                + " FROM cart c"
                + " JOIN show_session s ON s.id = c.session_id"
                + " JOIN movie m        ON m.id = s.movie_id"
                + " JOIN cinema_hall h  ON h.id = s.hall_id"
                + " WHERE c.account_id = ?"
                + " ORDER BY c.session_id, c.seat_key";

        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, accountId);
            try (ResultSet rs = ps.executeQuery()) {
                Map<String, List<String>> seatsBySession = new LinkedHashMap<>();
                Map<String, ShowSession> sessionInfo = new LinkedHashMap<>();
                Map<String, String[]> titles = new LinkedHashMap<>();

                while (rs.next()) {
                    String sessionId = rs.getString("session_id");
                    seatsBySession.computeIfAbsent(sessionId, k -> new ArrayList<>())
                            .add(rs.getString("seat_key"));
                    sessionInfo.putIfAbsent(sessionId, new ShowSession(
                            sessionId,
                            null,
                            null,
                            com.movieticket.backend.dao.ShowSessionDao.format(rs.getTimestamp("start_time")),
                            com.movieticket.backend.dao.ShowSessionDao.format(rs.getTimestamp("end_time")),
                            rs.getInt("price_fen")));
                    titles.putIfAbsent(sessionId, new String[]{
                            rs.getString("movie_title"), rs.getString("hall_name")});
                }

                List<CartItem> items = new ArrayList<>();
                int sequence = 100;
                for (Map.Entry<String, List<String>> entry : seatsBySession.entrySet()) {
                    String sessionId = entry.getKey();
                    ShowSession info = sessionInfo.get(sessionId);
                    String[] titleAndHall = titles.get(sessionId);
                    items.add(new CartItem(
                            "C" + (sequence++),
                            sessionId,
                            titleAndHall[0],
                            titleAndHall[1],
                            info.startTime(),
                            info.endTime(),
                            info.priceFen(),
                            new TreeSet<>(entry.getValue())));
                }
                return items;
            }
        }
    }

    /** 购物车条目数（按场次聚合后的数量）。 */
    public int countItems(Connection conn, String accountId) throws SQLException {
        String sql = "SELECT COUNT(DISTINCT session_id) FROM cart WHERE account_id = ?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, accountId);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? rs.getInt(1) : 0;
            }
        }
    }

    /** 按条目编号移除一整个场次的座位；编号形如 C100，仅作占位，实际按序号定位。 */
    public int removeBySession(Connection conn, String accountId, String sessionId) throws SQLException {
        String sql = "DELETE FROM cart WHERE account_id = ? AND session_id = ?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, accountId);
            ps.setString(2, sessionId);
            return ps.executeUpdate();
        }
    }

    public int clear(Connection conn, String accountId) throws SQLException {
        String sql = "DELETE FROM cart WHERE account_id = ?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, accountId);
            return ps.executeUpdate();
        }
    }

    /** 取购物车中已被占座的冲突信息，用于下单前复核。 */
    public List<String> findSessionIds(Connection conn, String accountId) throws SQLException {
        String sql = "SELECT DISTINCT session_id FROM cart WHERE account_id = ? ORDER BY session_id";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, accountId);
            try (ResultSet rs = ps.executeQuery()) {
                List<String> result = new ArrayList<>();
                while (rs.next()) {
                    result.add(rs.getString(1));
                }
                return result;
            }
        }
    }

    /** 便利方法：把影片与场次信息拼成展示文本，供日志使用。 */
    public static String describe(Movie movie, ShowSession session) {
        return movie.title() + " · " + session.startTime();
    }
}
