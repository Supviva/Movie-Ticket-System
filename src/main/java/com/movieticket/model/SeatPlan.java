package com.movieticket.model;

import java.util.Set;

public record SeatPlan(
        CinemaHall hall,
        ShowSession session,
        Set<String> soldSeats) {
}
