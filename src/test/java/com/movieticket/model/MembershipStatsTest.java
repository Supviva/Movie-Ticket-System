package com.movieticket.model;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;

class MembershipStatsTest {

    private TicketOrder order(String status, int payable, int discount, int seatCount) {
        List<String> seats = switch (seatCount) {
            case 1 -> List.of("A1");
            case 2 -> List.of("A1", "A2");
            case 3 -> List.of("A1", "A2", "A3");
            default -> List.of();
        };
        TicketOrderItem item = new TicketOrderItem("OI1", "S001", "测试影片", "1号厅",
                "2026-09-11 19:00", "2026-09-11 21:00", 4500, Set.copyOf(seats));
        return new TicketOrder("T000001", "customer", "customer", List.of(item),
                payable + discount, discount, payable, "银卡会员", status, "", "2026-09-11 10:00:00");
    }

    @Test
    void emptyListYieldsZeroStats() {
        assertEquals(MembershipStats.empty(), MembershipStats.of(List.of()));
    }

    @Test
    void nullListYieldsZeroStats() {
        assertEquals(MembershipStats.empty(), MembershipStats.of(null));
    }

    @Test
    void onlyPaidOrdersAreCounted() {
        MembershipStats stats = MembershipStats.of(List.of(
                order("PAID", 8740, 460, 2),
                order("UNPAID", 4500, 0, 1),
                order("CANCELED", 9000, 1000, 3)));

        assertEquals(8740, stats.spentFen());
        assertEquals(460, stats.savedFen());
        assertEquals(1, stats.paidOrderCount());
        assertEquals(2, stats.ticketCount());
    }

    @Test
    void paidOrdersAccumulateAcrossOrders() {
        MembershipStats stats = MembershipStats.of(List.of(
                order("PAID", 1000, 100, 1),
                order("PAID", 2000, 200, 2),
                order("PAID", 3000, 300, 3)));

        assertEquals(6000, stats.spentFen());
        assertEquals(600, stats.savedFen());
        assertEquals(3, stats.paidOrderCount());
        assertEquals(6, stats.ticketCount());
    }
}
