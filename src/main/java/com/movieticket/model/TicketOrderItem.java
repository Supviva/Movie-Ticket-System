package com.movieticket.model;

import java.io.Serializable;
import java.util.Set;

public record TicketOrderItem(
        String id,
        String sessionId,
        String movieTitle,
        String hallName,
        String startTime,
        String endTime,
        int unitPriceFen,
        Set<String> seats) implements Serializable {

    public int lineTotalFen() {
        return unitPriceFen * seats.size();
    }
}
