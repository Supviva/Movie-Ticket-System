package com.movieticket.model;

public record ShowSession(
        String id,
        String movieId,
        String hallId,
        String startTime,
        String endTime,
        int priceFen) {
}
