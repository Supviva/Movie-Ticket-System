package com.movieticket.model;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Locale;

public final class Money {

    private Money() {
    }

    public static String format(int fen) {
        return String.format(Locale.ROOT, "¥%d.%02d", fen / 100, Math.abs(fen % 100));
    }

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
