package com.movieticket.backend.model;

/**
 * 场次。
 *
 * @param id        场次编号，如 S001
 * @param movieId   影片编号
 * @param hallId    影厅编号
 * @param startTime 开映时间，yyyy-MM-dd HH:mm
 * @param endTime   散场时间，yyyy-MM-dd HH:mm
 * @param priceFen  票价（分）
 */
public record ShowSession(
        String id,
        String movieId,
        String hallId,
        String startTime,
        String endTime,
        int priceFen) {
}
