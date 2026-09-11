package com.movieticket.model;

public record CinemaHall(
        String id,
        String name,
        String specs,
        int rows,
        int cols) {
}
