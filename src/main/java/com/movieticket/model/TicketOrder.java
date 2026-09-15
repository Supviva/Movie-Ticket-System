package com.movieticket.model;

import java.util.List;

/**
 * 电影票订单。
 *
 * <p>{@code upgradedFrom} / {@code upgradedTo} 记录「支付成功后由累计消费触发的那次升级」：
 * 未升级时两者均为 {@code null}，升级时为升级前后的等级。界面据此提示用户，
 * 无需自己比对等级差异。</p>
 */
public record TicketOrder(
        String id,
        String userId,
        String username,
        List<TicketOrderItem> items,
        int originalFen,
        int discountFen,
        int payableFen,
        String membershipLabel,
        String status,
        String ticketCode,
        String createdAt,
        MembershipLevel upgradedFrom,
        MembershipLevel upgradedTo) {

    public TicketOrder(String id, String userId, String username, List<TicketOrderItem> items,
                       int originalFen, int discountFen, int payableFen, String membershipLabel,
                       String status, String ticketCode, String createdAt) {
        this(id, userId, username, items, originalFen, discountFen, payableFen, membershipLabel,
                status, ticketCode, createdAt, null, null);
    }

    /** 本次支付是否触发了会员升级。 */
    public boolean upgraded() {
        return upgradedFrom != null && upgradedTo != null && upgradedTo.ordinal() > upgradedFrom.ordinal();
    }

    /** 复制订单并附上升级信息；{@code from == to} 时不标记升级。 */
    public TicketOrder withUpgrade(MembershipLevel from, MembershipLevel to) {
        if (from == null || to == null || to.ordinal() <= from.ordinal()) {
            return this;
        }
        return new TicketOrder(id, userId, username, items, originalFen, discountFen, payableFen,
                membershipLabel, status, ticketCode, createdAt, from, to);
    }
}
