package com.movieticket.model;

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
