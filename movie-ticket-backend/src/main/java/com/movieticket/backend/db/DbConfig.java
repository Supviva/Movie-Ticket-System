package com.movieticket.backend.db;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

/**
 * 数据库连接配置。
 *
 * <p>优先读环境变量，其次读 classpath 下的 {@code db.properties}，
 * 最后用内置默认值。这样同一个 jar 换机器时不必改代码。</p>
 *
 * <p>可覆盖的环境变量：{@code DB_URL} / {@code DB_USER} / {@code DB_PASSWORD}</p>
 */
public final class DbConfig {

    private static final String DEFAULT_URL =
            "jdbc:mysql://localhost:3306/movie_ticket"
                    + "?useUnicode=true&characterEncoding=utf8"
                    + "&useSSL=false&allowPublicKeyRetrieval=true"
                    + "&serverTimezone=Asia/Shanghai"
                    + "&zeroDateTimeBehavior=convertToNull";

    private static final String DEFAULT_USER = "root";
    private static final String DEFAULT_PASSWORD = "123456";

    private static final Properties FILE_PROPS = loadFileProps();

    private final String url;
    private final String user;
    private final String password;

    private DbConfig(String url, String user, String password) {
        this.url = url;
        this.user = user;
        this.password = password;
    }

    /** 按「环境变量 > 配置文件 > 默认值」的优先级构建配置。 */
    public static DbConfig load() {
        String url = firstNonBlank(System.getenv("DB_URL"),
                FILE_PROPS.getProperty("db.url"), DEFAULT_URL);
        String user = firstNonBlank(System.getenv("DB_USER"),
                FILE_PROPS.getProperty("db.user"), DEFAULT_USER);
        String password = firstNonBlank(System.getenv("DB_PASSWORD"),
                FILE_PROPS.getProperty("db.password"), DEFAULT_PASSWORD);
        return new DbConfig(url, user, password);
    }

    /** 用指定密码构建配置，供需要临时覆盖（如测试）的场景使用。 */
    public static DbConfig of(String url, String user, String password) {
        return new DbConfig(url, user, password);
    }

    private static Properties loadFileProps() {
        Properties props = new Properties();
        try (InputStream in = DbConfig.class.getResourceAsStream("/db.properties")) {
            if (in != null) {
                props.load(new java.io.InputStreamReader(in, java.nio.charset.StandardCharsets.UTF_8));
            }
        } catch (IOException ignored) {
            // 配置文件缺失或损坏时退回默认值，不阻断启动
        }
        return props;
    }

    private static String firstNonBlank(String... values) {
        for (String value : values) {
            if (value != null && !value.isBlank()) {
                return value.trim();
            }
        }
        return null;
    }

    public String url() {
        return url;
    }

    public String user() {
        return user;
    }

    public String password() {
        return password;
    }

    /** 显示用摘要，不泄露口令。 */
    public String describe() {
        return "url=" + url + ", user=" + user + ", password=" + mask(password);
    }

    private static String mask(String value) {
        if (value == null || value.isEmpty()) {
            return "(空)";
        }
        return "*".repeat(value.length());
    }
}
