package com.movieticket.backend.model;

/**
 * 账号。
 *
 * @param id             主键，如 A-admin / U-001
 * @param username       登录名
 * @param displayName    姓名
 * @param phone          手机号
 * @param role           角色
 * @param enabled        是否启用
 * @param membership     会员等级
 */
public record Account(
        String id,
        String username,
        String displayName,
        String phone,
        Role role,
        boolean enabled,
        MembershipLevel membership) {

    public Account withMembership(MembershipLevel level) {
        return new Account(id, username, displayName, phone, role, enabled, level);
    }

    public Account withProfile(String name, String phoneNumber, boolean active) {
        return new Account(id, username, name, phoneNumber, role, active, membership);
    }
}
