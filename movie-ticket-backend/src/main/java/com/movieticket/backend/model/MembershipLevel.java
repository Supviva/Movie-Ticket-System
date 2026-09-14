package com.movieticket.backend.model;

/**
 * 会员等级目录。
 *
 * <p><b>枚举声明顺序即升级顺序</b>，从低到高；数据库
 * {@code membership_level.sort_order} 必须与之一致。新增档位只能追加在末尾。</p>
 *
 * <p>等级由「累计消费金额」唯一推导，见 {@link #forSpending(int)}。</p>
 */
public enum MembershipLevel {

    NORMAL("普通会员", 1000, "无折扣", 0),
    SILVER("银卡会员", 950, "95折", 20000),
    GOLD("金卡会员", 880, "88折", 80000),
    PLATINUM("铂金会员", 840, "84折", 200000),
    DIAMOND("钻石会员", 800, "8折", 500000);

    private final String label;
    private final int discountBasisPoints;
    private final String discountText;
    private final int thresholdFen;

    MembershipLevel(String label, int discountBasisPoints, String discountText, int thresholdFen) {
        this.label = label;
        this.discountBasisPoints = discountBasisPoints;
        this.discountText = discountText;
        this.thresholdFen = thresholdFen;
    }

    public String label() {
        return label;
    }

    /** 折扣基点：1000 表示原价，950 表示 95 折。 */
    public int discountBasisPoints() {
        return discountBasisPoints;
    }

    public String discountText() {
        return discountText;
    }

    /** 晋升到本档所需的累计消费金额（分）；普通会员为 0。 */
    public int thresholdFen() {
        return thresholdFen;
    }

    public boolean hasDiscount() {
        return discountBasisPoints < 1000;
    }

    /** 高一级会员；已是最高档时返回 {@code null}。 */
    public MembershipLevel next() {
        return isTop() ? null : values()[ordinal() + 1];
    }

    public boolean isTop() {
        return ordinal() == values().length - 1;
    }

    /** 等级编码，与数据库 {@code membership_level.level_code} 一致。 */
    public String code() {
        return name();
    }

    /** 按编码查找；无法识别时抛异常，避免静默降级。 */
    public static MembershipLevel fromCode(String code) {
        if (code == null || code.isBlank()) {
            return NORMAL;
        }
        for (MembershipLevel level : values()) {
            if (level.name().equalsIgnoreCase(code.trim())) {
                return level;
            }
        }
        throw new IllegalArgumentException("未知的会员等级编码：" + code);
    }

    /**
     * 按累计消费金额推导应处等级。
     *
     * <p>遍历全部档位取「门槛不超过累计消费」的最高者，因此一次消费跨度多大都会
     * 直接落到对应档。累计消费为 0 或负数时返回 {@link #NORMAL}。</p>
     */
    public static MembershipLevel forSpending(int spentFen) {
        MembershipLevel result = NORMAL;
        for (MembershipLevel level : values()) {
            if (spentFen >= level.thresholdFen) {
                result = level;
            }
        }
        return result;
    }

    @Override
    public String toString() {
        return label;
    }
}
