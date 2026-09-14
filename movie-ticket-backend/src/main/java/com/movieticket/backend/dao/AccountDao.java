package com.movieticket.backend.dao;

import com.movieticket.backend.model.Account;
import com.movieticket.backend.model.MembershipLevel;
import com.movieticket.backend.model.Role;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * 账号数据访问对象。
 *
 * <p>所有 SQL 均使用 {@link PreparedStatement} 占位符传参，不做字符串拼接，
 * 从根本上避免 SQL 注入。</p>
 */
public class AccountDao {

    private static final String COLUMNS =
            "id, username, display_name, phone, password_hash, role, enabled, membership_code";

    /** 按主键查询；不存在返回 null。 */
    public Account findById(Connection conn, String id) throws SQLException {
        String sql = "SELECT " + COLUMNS + " FROM account WHERE id = ?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? map(rs) : null;
            }
        }
    }

    /** 按登录名查询（忽略大小写）；不存在返回 null。 */
    public Account findByUsername(Connection conn, String username) throws SQLException {
        String sql = "SELECT " + COLUMNS + " FROM account WHERE LOWER(username) = LOWER(?)";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, username);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? map(rs) : null;
            }
        }
    }

    /** 取口令摘要，用于登录校验；账号不存在返回 null。 */
    public String findPasswordHash(Connection conn, String accountId) throws SQLException {
        String sql = "SELECT password_hash FROM account WHERE id = ?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, accountId);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? rs.getString(1) : null;
            }
        }
    }

    /** 查顾客列表，keyword 可匹配用户名/姓名/手机号；传 null 或空串表示不过滤。 */
    public List<Account> findCustomers(Connection conn, String keyword) throws SQLException {
        StringBuilder sql = new StringBuilder("SELECT " + COLUMNS + " FROM account WHERE role = 'CUSTOMER'");
        boolean hasKeyword = keyword != null && !keyword.isBlank();
        if (hasKeyword) {
            sql.append(" AND (LOWER(username) LIKE ? OR display_name LIKE ? OR phone LIKE ?)");
        }
        sql.append(" ORDER BY id");

        try (PreparedStatement ps = conn.prepareStatement(sql.toString())) {
            if (hasKeyword) {
                String like = "%" + keyword.trim().toLowerCase(Locale.ROOT) + "%";
                ps.setString(1, like);
                ps.setString(2, "%" + keyword.trim() + "%");
                ps.setString(3, like);
            }
            try (ResultSet rs = ps.executeQuery()) {
                List<Account> result = new ArrayList<>();
                while (rs.next()) {
                    result.add(map(rs));
                }
                return result;
            }
        }
    }

    /** 新增账号。 */
    public void insert(Connection conn, Account account, String passwordHash) throws SQLException {
        String sql = "INSERT INTO account (id, username, display_name, phone, password_hash, role, enabled, membership_code)"
                + " VALUES (?, ?, ?, ?, ?, ?, ?, ?)";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, account.id());
            ps.setString(2, account.username());
            ps.setString(3, account.displayName());
            ps.setString(4, account.phone());
            ps.setString(5, passwordHash);
            ps.setString(6, account.role().name());
            ps.setBoolean(7, account.enabled());
            ps.setString(8, account.membership().code());
            ps.executeUpdate();
        }
    }

    /** 更新姓名、手机号与启用状态。 */
    public int updateProfile(Connection conn, String accountId, String displayName,
                             String phone, boolean enabled) throws SQLException {
        String sql = "UPDATE account SET display_name = ?, phone = ?, enabled = ? WHERE id = ?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, displayName);
            ps.setString(2, phone);
            ps.setBoolean(3, enabled);
            ps.setString(4, accountId);
            return ps.executeUpdate();
        }
    }

    /** 修改会员等级。 */
    public int updateMembership(Connection conn, String accountId, MembershipLevel level) throws SQLException {
        String sql = "UPDATE account SET membership_code = ? WHERE id = ?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, level.code());
            ps.setString(2, accountId);
            return ps.executeUpdate();
        }
    }

    /** 取当前最大顾客编号数字部分，用于生成下一个 U-xxx；无顾客时返回 0。 */
    public int maxCustomerSequence(Connection conn) throws SQLException {
        String sql = "SELECT COALESCE(MAX(CAST(SUBSTRING(id, 3) AS UNSIGNED)), 0)"
                + " FROM account WHERE id LIKE 'U-%'";
        try (PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            return rs.next() ? rs.getInt(1) : 0;
        }
    }

    private Account map(ResultSet rs) throws SQLException {
        return new Account(
                rs.getString("id"),
                rs.getString("username"),
                rs.getString("display_name"),
                rs.getString("phone"),
                Role.valueOf(rs.getString("role")),
                rs.getBoolean("enabled"),
                MembershipLevel.fromCode(rs.getString("membership_code")));
    }
}
