package com.movieticket.backend.dao;

import com.movieticket.backend.model.Movie;

import java.sql.Connection;
import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * 影片数据访问对象。
 */
public class MovieDao {

    private static final String COLUMNS =
            "id, title, genre, director, starring, duration_min, format, release_date,"
                    + " synopsis, rating, poster_palette, poster_path";

    /** 全部影片，按编号排序。 */
    public List<Movie> findAll(Connection conn) throws SQLException {
        String sql = "SELECT " + COLUMNS + " FROM movie ORDER BY id";
        try (PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            List<Movie> result = new ArrayList<>();
            while (rs.next()) {
                result.add(map(rs));
            }
            return result;
        }
    }

    /** 关键词搜索：片名 / 类型 / 导演 / 主演。 */
    public List<Movie> search(Connection conn, String keyword) throws SQLException {
        if (keyword == null || keyword.isBlank()) {
            return findAll(conn);
        }
        String sql = "SELECT " + COLUMNS + " FROM movie"
                + " WHERE LOWER(title) LIKE ? OR genre LIKE ? OR LOWER(director) LIKE ? OR LOWER(starring) LIKE ?"
                + " ORDER BY id";
        String lowered = "%" + keyword.trim().toLowerCase(Locale.ROOT) + "%";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, lowered);
            ps.setString(2, "%" + keyword.trim() + "%");
            ps.setString(3, lowered);
            ps.setString(4, lowered);
            try (ResultSet rs = ps.executeQuery()) {
                List<Movie> result = new ArrayList<>();
                while (rs.next()) {
                    result.add(map(rs));
                }
                return result;
            }
        }
    }

    /** 按主键查询；不存在返回 null。 */
    public Movie findById(Connection conn, String id) throws SQLException {
        String sql = "SELECT " + COLUMNS + " FROM movie WHERE id = ?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? map(rs) : null;
            }
        }
    }

    public void insert(Connection conn, Movie movie) throws SQLException {
        String sql = "INSERT INTO movie (id, title, genre, director, starring, duration_min, format,"
                + " release_date, synopsis, rating, poster_palette, poster_path)"
                + " VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            bind(ps, movie);
            ps.executeUpdate();
        }
    }

    public int update(Connection conn, Movie movie) throws SQLException {
        String sql = "UPDATE movie SET title = ?, genre = ?, director = ?, starring = ?,"
                + " duration_min = ?, format = ?, release_date = ?, synopsis = ?, rating = ?,"
                + " poster_palette = ?, poster_path = ? WHERE id = ?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, movie.title());
            ps.setString(2, movie.genre());
            ps.setString(3, movie.director());
            ps.setString(4, movie.starring());
            ps.setInt(5, movie.durationMinutes());
            ps.setString(6, movie.format());
            setDate(ps, 7, movie.releaseDate());
            ps.setString(8, movie.synopsis());
            ps.setDouble(9, movie.rating());
            ps.setInt(10, movie.posterPalette());
            ps.setString(11, movie.posterPath());
            ps.setString(12, movie.id());
            return ps.executeUpdate();
        }
    }

    public int delete(Connection conn, String id) throws SQLException {
        String sql = "DELETE FROM movie WHERE id = ?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, id);
            return ps.executeUpdate();
        }
    }

    /** 取当前最大影片编号数字部分，用于生成下一个 Mxxx。 */
    public int maxMovieSequence(Connection conn) throws SQLException {
        String sql = "SELECT COALESCE(MAX(CAST(SUBSTRING(id, 2) AS UNSIGNED)), 0)"
                + " FROM movie WHERE id LIKE 'M%'";
        try (PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            return rs.next() ? rs.getInt(1) : 0;
        }
    }

    private void bind(PreparedStatement ps, Movie movie) throws SQLException {
        ps.setString(1, movie.id());
        ps.setString(2, movie.title());
        ps.setString(3, movie.genre());
        ps.setString(4, movie.director());
        ps.setString(5, movie.starring());
        ps.setInt(6, movie.durationMinutes());
        ps.setString(7, movie.format());
        setDate(ps, 8, movie.releaseDate());
        ps.setString(9, movie.synopsis());
        ps.setDouble(10, movie.rating());
        ps.setInt(11, movie.posterPalette());
        ps.setString(12, movie.posterPath());
    }

    private void setDate(PreparedStatement ps, int index, String date) throws SQLException {
        if (date == null || date.isBlank()) {
            ps.setNull(index, java.sql.Types.DATE);
        } else {
            ps.setDate(index, Date.valueOf(date));
        }
    }

    private Movie map(ResultSet rs) throws SQLException {
        Date releaseDate = rs.getDate("release_date");
        return new Movie(
                rs.getString("id"),
                rs.getString("title"),
                rs.getString("genre"),
                rs.getString("director"),
                rs.getString("starring"),
                rs.getInt("duration_min"),
                rs.getString("format"),
                releaseDate == null ? null : releaseDate.toString(),
                rs.getString("synopsis"),
                rs.getDouble("rating"),
                rs.getInt("poster_palette"),
                rs.getString("poster_path"));
    }
}
