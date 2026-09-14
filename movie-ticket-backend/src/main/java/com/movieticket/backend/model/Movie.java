package com.movieticket.backend.model;

/**
 * 影片。
 *
 * @param id           影片编号，如 M001
 * @param title        片名
 * @param genre        类型
 * @param director     导演
 * @param starring     主演
 * @param durationMinutes 时长（分钟）
 * @param format       制式：2D / 3D / IMAX
 * @param releaseDate  上映日期，yyyy-MM-dd
 * @param synopsis     剧情简介
 * @param rating       评分 0.0 - 10.0
 * @param posterPalette 海报配色索引
 * @param posterPath   海报文件路径，可为 null
 */
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
        int posterPalette,
        String posterPath) {

    public String durationText() {
        return durationMinutes + " 分钟";
    }
}
