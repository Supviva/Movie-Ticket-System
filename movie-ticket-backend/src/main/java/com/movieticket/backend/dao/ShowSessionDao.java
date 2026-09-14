package com.movieticket.backend.dao;

import com.movieticket.backend.model.ShowSession;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * 场次数据访问对象。
 *
 * <p>数据库存 DATETIME，模型里用字符串 {@code yyyy-MM-dd HH:mm}，
 * 转换集中在本类的 {@link #format}/{@link #parse} 两个方法中。</p>
 */
public class ShowSessionDao {

    private static final DateTimeFormatter FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm", Locale.ROOT);
    private static final DateTimeFormatter FMT_SECONDS =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss", Locale.ROOT);

    private static final String COLUMNS = "id, movie_id, hall_id, start_time, end_time, price_fen";

    public List<ShowSession> findAll(Connection conn) throws SQLException {
        String sql = "SELECT " + COLUMNS + " FROM show_session ORDER BY start_time";
        try (PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            List<ShowSession> result = new ArrayList<>();
            while (rs.next()) {
                result.add(map(rs));
            }
            return result;
        }
    }

    /** 某部影片的全部场次，按开映时间升序。 */
    public List<ShowSession> findByMovie(Connection conn, String movieId) throws SQLException {
        String sql = "SELECT " + COLUMNS + " FROM show_session WHERE movie_id = ? ORDER BY start_time";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, movieId);
            try (ResultSet rs = ps.executeQuery()) {
                List<ShowSession> result = new ArrayList<>();
                while (rs.next()) {
                    result.add(map(rs));
                }
                return result;
            }
        }
    }

    public ShowSession findById(Connection conn, String id) throws SQLException {
        String sql = "SELECT " + COLUMNS + " FROM show_session WHERE id = ?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? map(rs) : null;
            }
        }
    }

    public void insert(Connection conn, ShowSession session) throws SQLException {
        String sql = "INSERT INTO show_session (id, movie_id, hall_id, start_time, end_time, price_fen)"
                + " VALUES (?, ?, ?, ?, ?, ?)";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, session.id());
            ps.setString(2, session.movieId());
            ps.setString(3, session.hallId());
            ps.setTimestamp(4, Timestamp.valueOf(parse(session.startTime())));
            ps.setTimestamp(5, Timestamp.valueOf(parse(session.endTime())));
            ps.setInt(6, session.priceFen());
            ps.executeUpdate();
        }
    }

    public int update(Connection conn, ShowSession session) throws SQLException {
        String sql = "UPDATE show_session SET movie_id = ?, hall_id = ?, start_time = ?,"
                + " end_time = ?, price_fen = ? WHERE id = ?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, session.movieId());
            ps.setString(2, session.hallId());
            ps.setTimestamp(3, Timestamp.valueOf(parse(session.startTime())));
            ps.setTimestamp(4, Timestamp.valueOf(parse(session.endTime())));
            ps.setInt(5, session.priceFen());
            ps.setString(6, session.id());
            return ps.executeUpdate();
        }
    }

    public int delete(Connection conn, String id) throws SQLException {
        String sql = "DELETE FROM show_session WHERE id = ?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, id);
            return ps.executeUpdate();
        }
    }

    public int maxSessionSequence(Connection conn) throws SQLException {
        String sql = "SELECT COALESCE(MAX(CAST(SUBSTRING(id, 2) AS UNSIGNED)), 0)"
                + " FROM show_session WHERE id LIKE 'S%'";
        try (PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            return rs.next() ? rs.getInt(1) : 0;
        }
    }

    /** 解析 'yyyy-MM-dd HH:mm' 或 'yyyy-MM-dd HH:mm:ss'。 */
    public static LocalDateTime parse(String text) {
        if (text == null || text.isBlank()) {
            throw new IllegalArgumentException("时间不能为空");
        }
        String trimmed = text.trim();
        try {
            return trimmed.length() > 16
                    ? LocalDateTime.parse(trimmed, FMT_SECONDS)
                    : LocalDateTime.parse(trimmed, FMT);
        } catch (Exception e) {
            throw new IllegalArgumentException("时间格式应为 yyyy-MM-dd HH:mm，实际为：" + text);
        }
    }

    /** 格式化为展示用 'yyyy-MM-dd HH:mm'。 */
    public static String format(Timestamp ts) {
        return ts == null ? null : ts.toLocalDateTime().format(FMT);
    }

    private ShowSession map(ResultSet rs) throws SQLException {
        return new ShowSession(
                rs.getString("id"),
                rs.getString("movie_id"),
                rs.getString("hall_id"),
                format(rs.getTimestamp("start_time")),
                format(rs.getTimestamp("end_time")),
                rs.getInt("price_fen"));
    }
}
