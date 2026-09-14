package com.movieticket.backend.model;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Locale;

/**
 * 金额工具。全系统金额以「分」为单位的 int 流转，不使用浮点，避免精度误差。
 */
public final class Money {

    private Money() {
    }

    /** 格式化为 ¥ 前缀的展示文本，如 4600 -> "¥46.00"。 */
    public static String format(int fen) {
        return String.format(Locale.ROOT, "¥%d.%02d", fen / 100, Math.abs(fen % 100));
    }

    /**
     * 按折扣基点计算折后价。
     *
     * <p>例：原价 4600 分、950 基点（95 折）→ 4370 分。采用四舍五入到分。</p>
     */
    public static int applyDiscount(int originalFen, int basisPoints) {
        if (basisPoints >= 1000) {
            return originalFen;
        }
        return BigDecimal.valueOf(originalFen)
                .multiply(BigDecimal.valueOf(basisPoints))
                .divide(BigDecimal.valueOf(1000), 0, RoundingMode.HALF_UP)
                .intValueExact();
    }
}
