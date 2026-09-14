package com.movieticket.backend.model;

/**
 * 影厅。
 *
 * @param id   影厅编号，如 H01
 * @param name 影厅名称
 * @param specs 规格，如 杜比全景声
 * @param rows 座位行数
 * @param cols 座位列数
 */
public record CinemaHall(
        String id,
        String name,
        String specs,
        int rows,
        int cols) {

    /** 座位总数。 */
    public int seatCount() {
        return rows * cols;
    }
}
