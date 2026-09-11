package com.movieticket.model;

public record Movie(
        String id,
        String title,
        String genre,
        String director,
        String starring,
        int durationMinutes,
        String format,
        String releaseDate,
        String synopsis,
        double rating,
        int posterPalette) {

    public String durationText() {
        return durationMinutes + " 分钟";
    }
}
