package com.movieticket.model;

import java.util.List;

/**
 * 会员页展示用的观影数据汇总。
 *
 * <p>纯计算、无状态，只统计已出票（PAID）的订单：待支付与已取消的订单不计入，
 * 避免进度条与"已省金额"被未真实发生的消费虚增。</p>
 */
public record MembershipStats(int spentFen, int savedFen, int paidOrderCount, int ticketCount) {

    private static final MembershipStats EMPTY = new MembershipStats(0, 0, 0, 0);

    public static MembershipStats of(List<TicketOrder> orders) {
        if (orders == null || orders.isEmpty()) {
            return EMPTY;
        }
        int spent = 0;
        int saved = 0;
        int orderCount = 0;
        int tickets = 0;
        for (TicketOrder order : orders) {
            if (!"PAID".equals(order.status())) {
                continue;
            }
            spent += order.payableFen();
            saved += order.discountFen();
            orderCount++;
            tickets += order.items().stream().mapToInt(item -> item.seats().size()).sum();
        }
        return new MembershipStats(spent, saved, orderCount, tickets);
    }

    public static MembershipStats empty() {
        return EMPTY;
    }
}
