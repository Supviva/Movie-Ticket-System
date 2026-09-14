package com.movieticket.backend.model;

import java.util.Set;
import java.util.TreeSet;

/**
 * 某场次的完整座位图：影厅规格 + 场次信息 + 已售座位。
 *
 * @param hall      影厅
 * @param session   场次
 * @param soldSeats 已占用座位（已排序）
 */
public record SeatPlan(
        CinemaHall hall,
        ShowSession session,
        Set<String> soldSeats) {

    public SeatPlan {
        soldSeats = soldSeats == null ? Set.of() : Set.copyOf(new TreeSet<>(soldSeats));
    }

    /** 该场次剩余可售座位数。 */
    public int availableCount() {
        return hall.seatCount() - soldSeats.size();
    }
}
