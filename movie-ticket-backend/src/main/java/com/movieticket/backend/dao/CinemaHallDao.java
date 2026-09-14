package com.movieticket.backend.dao;

import com.movieticket.backend.model.CinemaHall;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

/**
 * 影厅数据访问对象。新增影厅时会同步生成该厅的全部座位。
 */
public class CinemaHallDao {

    private static final String COLUMNS = "id, name, specs, row_count, col_count";

    public List<CinemaHall> findAll(Connection conn) throws SQLException {
        String sql = "SELECT " + COLUMNS + " FROM cinema_hall ORDER BY id";
        try (PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            List<CinemaHall> result = new ArrayList<>();
            while (rs.next()) {
                result.add(map(rs));
            }
            return result;
        }
    }

    public CinemaHall findById(Connection conn, String id) throws SQLException {
        String sql = "SELECT " + COLUMNS + " FROM cinema_hall WHERE id = ?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? map(rs) : null;
            }
        }
    }

    public void insert(Connection conn, CinemaHall hall) throws SQLException {
        String sql = "INSERT INTO cinema_hall (id, name, specs, row_count, col_count) VALUES (?, ?, ?, ?, ?)";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, hall.id());
            ps.setString(2, hall.name());
            ps.setString(3, hall.specs());
            ps.setInt(4, hall.rows());
            ps.setInt(5, hall.cols());
            ps.executeUpdate();
        }
    }

    public int update(Connection conn, CinemaHall hall) throws SQLException {
        String sql = "UPDATE cinema_hall SET name = ?, specs = ?, row_count = ?, col_count = ? WHERE id = ?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, hall.name());
            ps.setString(2, hall.specs());
            ps.setInt(3, hall.rows());
            ps.setInt(4, hall.cols());
            ps.setString(5, hall.id());
            return ps.executeUpdate();
        }
    }

    public int delete(Connection conn, String id) throws SQLException {
        String sql = "DELETE FROM cinema_hall WHERE id = ?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, id);
            return ps.executeUpdate();
        }
    }

    /**
     * 按行列批量生成座位。座位号形如 A1 / B12，行下标从 0 起对应字母 A 起。
     * 使用 {@code INSERT IGNORE} 保证重复调用不报错。
     */
    public void generateSeats(Connection conn, CinemaHall hall) throws SQLException {
        String sql = "INSERT IGNORE INTO seat (hall_id, seat_key, row_index, col_index, seat_type)"
                + " VALUES (?, ?, ?, ?, ?)";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            for (int row = 0; row < hall.rows(); row++) {
                for (int col = 0; col < hall.cols(); col++) {
                    ps.setString(1, hall.id());
                    ps.setString(2, seatKey(row, col));
                    ps.setInt(3, row);
                    ps.setInt(4, col);
                    ps.setString(5, "NORMAL");
                    ps.addBatch();
                }
            }
            ps.executeBatch();
        }
    }

    /** 座位号生成规则：与业务层 seatKey 保持一致。 */
    public static String seatKey(int row, int col) {
        return String.format(java.util.Locale.ROOT, "%c%d", 'A' + row, col + 1);
    }

    public int maxHallSequence(Connection conn) throws SQLException {
        String sql = "SELECT COALESCE(MAX(CAST(SUBSTRING(id, 2) AS UNSIGNED)), 0)"
                + " FROM cinema_hall WHERE id LIKE 'H%'";
        try (PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            return rs.next() ? rs.getInt(1) : 0;
        }
    }

    private CinemaHall map(ResultSet rs) throws SQLException {
        return new CinemaHall(
                rs.getString("id"),
                rs.getString("name"),
                rs.getString("specs"),
                rs.getInt("row_count"),
                rs.getInt("col_count"));
    }
}
