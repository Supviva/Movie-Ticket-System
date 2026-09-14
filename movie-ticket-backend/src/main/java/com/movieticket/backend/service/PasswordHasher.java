package com.movieticket.backend.service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;

/**
 * 口令摘要工具。
 *
 * <p>数据库只存 SHA-256 摘要，不存明文。校验时对输入同样摘要后比对。
 * 这是比明文存储更安全的做法；生产环境还应加随机盐并换用 bcrypt/scrypt，
 * 这里为保持依赖简单只做摘要。</p>
 */
public final class PasswordHasher {

    private PasswordHasher() {
    }

    public static String hash(String rawPassword) {
        if (rawPassword == null) {
            throw new IllegalArgumentException("口令不能为 null");
        }
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] bytes = digest.digest(rawPassword.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(bytes);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("当前 JVM 不支持 SHA-256", e);
        }
    }

    /** 常量时间比对，避免通过响应时间推断口令。 */
    public static boolean matches(String rawPassword, String storedHash) {
        if (rawPassword == null || storedHash == null) {
            return false;
        }
        return MessageDigest.isEqual(
                hash(rawPassword).getBytes(StandardCharsets.UTF_8),
                storedHash.toLowerCase().getBytes(StandardCharsets.UTF_8));
    }
}
