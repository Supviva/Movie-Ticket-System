package com.movieticket.backend.db;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

/**
 * 连接管理与事务控制。
 *
 * <p>设计要点：</p>
 * <ul>
 *   <li>连接通过 {@link ThreadLocal} 绑定到当前线程，同一线程内多次 DAO 调用复用同一连接，
 *       这是让多个 DAO 共享一个事务的前提。</li>
 *   <li>{@link #inTransaction} 用模板方法统一处理提交与回滚，业务代码不必手写 try/catch。</li>
 *   <li>所有连接都在 finally 中归还，杜绝连接泄漏。</li>
 * </ul>
 *
 * <p>本类未引入第三方连接池以保持依赖最小；如需更高并发，可把 {@link #open()}
 * 换成 HikariCP 的数据源实现，其余代码无需改动。</p>
 */
public final class Db {

    private static final ThreadLocal<Connection> CURRENT = new ThreadLocal<>();
    private static final DbConfig CONFIG = DbConfig.load();

    static {
        try {
            Class.forName("com.mysql.cj.jdbc.Driver");
        } catch (ClassNotFoundException e) {
            throw new IllegalStateException("找不到 MySQL 驱动，请确认 pom.xml 中的 mysql-connector-j 依赖", e);
        }
    }

    private Db() {
    }

    public static DbConfig config() {
        return CONFIG;
    }

    /** 新建一个连接。调用方负责关闭。 */
    public static Connection open() throws SQLException {
        return DriverManager.getConnection(CONFIG.url(), CONFIG.user(), CONFIG.password());
    }

    /**
     * 取当前线程的连接：已在事务中就复用，否则新建。
     *
     * <p>注意：新建的连接需要调用方自行关闭；事务内由 {@link #inTransaction} 统一关闭。</p>
     */
    public static Connection current() throws SQLException {
        Connection conn = CURRENT.get();
        if (conn == null || conn.isClosed()) {
            conn = open();
            CURRENT.set(conn);
        }
        return conn;
    }

    /** 测试连通性，返回数据库版本号；连接不上时抛出带原因的异常。 */
    public static String testConnection() {
        try (Connection conn = open()) {
            return conn.getMetaData().getDatabaseProductName()
                    + " " + conn.getMetaData().getDatabaseProductVersion();
        } catch (SQLException e) {
            throw new ServiceException("数据库连接失败：" + e.getMessage()
                    + "\n请检查 MySQL 是否已启动，以及 db.properties 里的账号密码是否正确。", e);
        }
    }

    /**
     * 在事务中执行一段业务。
     *
     * <p>正常返回即提交；抛出任何异常则回滚并原样向上抛。事务期间连接绑定到当前线程，
     * 期间所有 DAO 调用自动使用同一连接。</p>
     *
     * @param work 业务逻辑，入参是可以直接用的连接
     */
    public static <T> T inTransaction(TransactionWork<T> work) {
        Connection conn = CURRENT.get();
        boolean ownsConnection = (conn == null);

        try {
            if (ownsConnection) {
                conn = open();
                CURRENT.set(conn);
            }
            conn.setAutoCommit(false);

            T result = work.execute(conn);

            conn.commit();
            return result;
        } catch (RuntimeException e) {
            rollbackQuietly(conn);
            throw e;
        } catch (Exception e) {
            rollbackQuietly(conn);
            throw new ServiceException("数据库操作失败：" + e.getMessage(), e);
        } finally {
            if (ownsConnection) {
                closeQuietly(conn);
                CURRENT.remove();
            } else {
                // 复用外层连接时只恢复自动提交，交由外层关闭
                try {
                    conn.setAutoCommit(true);
                } catch (SQLException ignored) {
                    // 连接已被外层关闭，忽略
                }
            }
        }
    }

    /** 只需要事务、不需要返回值时的便捷写法。 */
    public static void runInTransaction(TransactionWorkVoid work) {
        inTransaction(conn -> {
            work.execute(conn);
            return null;
        });
    }

    private static void rollbackQuietly(Connection conn) {
        if (conn != null) {
            try {
                conn.rollback();
            } catch (SQLException ignored) {
                // 回滚失败通常意味着连接已断，无需再处理
            }
        }
    }

    private static void closeQuietly(Connection conn) {
        if (conn != null) {
            try {
                conn.close();
            } catch (SQLException ignored) {
                // 关闭失败不影响业务结果
            }
        }
    }

    /** 有返回值的业务逻辑。 */
    @FunctionalInterface
    public interface TransactionWork<T> {
        T execute(Connection conn) throws SQLException;
    }

    /** 无返回值的业务逻辑。 */
    @FunctionalInterface
    public interface TransactionWorkVoid {
        void execute(Connection conn) throws SQLException;
    }
}
