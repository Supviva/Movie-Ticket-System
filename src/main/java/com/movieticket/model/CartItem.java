package com.movieticket.model;

import java.util.Set;

public record CartItem(
        String id,
        String sessionId,
        String movieTitle,
        String hallName,
        String startTime,
        String endTime,
        int unitPriceFen,
        Set<String> seats) {

    public int lineTotalFen() {
        return unitPriceFen * seats.size();
    }
}
