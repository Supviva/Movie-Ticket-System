package com.movieticket.backend.model;

import java.util.LinkedHashSet;
import java.util.Set;

/**
 * 订单明细（座位级快照）。一行对应订单中一个座位。
 *
 * @param sessionId    场次编号
 * @param movieTitle   片名快照
 * @param hallName     影厅名快照
 * @param startTime    开映时间快照
 * @param endTime      散场时间快照
 * @param unitPriceFen 单价（分）
 * @param seats        本明细包含的座位（数据库按座位一行存储，读取时聚合）
 */
public record TicketOrderItem(
        String sessionId,
        String movieTitle,
        String hallName,
        String startTime,
        String endTime,
        int unitPriceFen,
        Set<String> seats) {

    /** 本明细合计金额（分）。 */
    public int lineTotalFen() {
        return unitPriceFen * seats.size();
    }

    public TicketOrderItem {
        seats = seats == null ? Set.of() : Set.copyOf(new LinkedHashSet<>(seats));
    }
}
