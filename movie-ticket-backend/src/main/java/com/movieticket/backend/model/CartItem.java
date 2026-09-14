package com.movieticket.backend.model;

import java.util.LinkedHashSet;
import java.util.Set;

/**
 * 购物车条目。数据库按「用户 × 场次 × 座位」一行存储，
 * 查询时按场次聚合成本对象（一个条目含多个座位）。
 *
 * @param id           条目编号，形如 C100（由场次与座位聚合而来）
 * @param sessionId    场次编号
 * @param movieTitle   片名快照
 * @param hallName     影厅名快照
 * @param startTime    开映时间
 * @param endTime      散场时间
 * @param unitPriceFen 单价（分）
 * @param seats        座位集合
 */
public record CartItem(
        String id,
        String sessionId,
        String movieTitle,
        String hallName,
        String startTime,
        String endTime,
        int unitPriceFen,
        Set<String> seats) {

    /** 本条目合计金额（分）＝ 单价 × 座位数。 */
    public int lineTotalFen() {
        return unitPriceFen * seats.size();
    }

    /** 防御性拷贝，避免外部改动内部集合。 */
    public CartItem {
        seats = seats == null ? Set.of() : Set.copyOf(new LinkedHashSet<>(seats));
    }
}
