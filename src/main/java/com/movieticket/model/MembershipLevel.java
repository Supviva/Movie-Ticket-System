package com.movieticket.model;

import java.util.List;
import java.util.Locale;

/**
 * 会员等级目录。
 *
 * <p><b>枚举声明顺序即升级顺序</b>，从低到高。{@link #values()} 的次序被会员页的
 * 升级链与权益对比区依赖，新增档位必须追加在末尾，不可插入中间；数据库
 * {@code membership_level.sort_order} 需与之一致。</p>
 *
 * <p>本枚举自带升级门槛与权益文案，UI 层直接取用，无需再对等级做 switch 分支。
 * 配色属于表现层，由 {@code ui.component.Ui} 按等级映射，不放在模型里。</p>
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

    public int discountBasisPoints() {
        return discountBasisPoints;
    }

    public String discountText() {
        return discountText;
    }

    /** 升级到本档所需的累计消费金额（分）。普通会员作为起点，门槛为 0。 */
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

    /** 展示用折扣标签，如「95折」；无折扣档返回「原价」。 */
    public String discountLabel() {
        return hasDiscount() ? discountText : "原价";
    }

    /** 升到本档后相对原价省下的比例描述，用于对比区标题。 */
    public String savingText() {
        return hasDiscount() ? "立省 " + (1000 - discountBasisPoints) / 10 + "%" : "暂无折扣";
    }

    /**
     * 本档的 4 条会员权益，元素为 {@code {标题, 说明}}。
     *
     * <p>文案随档位递进而增强，便于用户在权益对比区看出各档差别。</p>
     */
    public List<String[]> benefitLines() {
        String discount = hasDiscount() ? "票价 " + discountText : "票价原价";
        String discountDetail = hasDiscount()
                ? "每张票自动按 " + discountText + " 结算"
                : "升级后每张票自动享受折扣";
        return List.of(
                new String[]{"票价优惠", discount + " · " + discountDetail},
                new String[]{"优先选座", priorityDetail()},
                new String[]{"专属客服", serviceDetail()},
                new String[]{"生日礼遇", birthdayDetail()});
    }

    private String priorityDetail() {
        return switch (this) {
            case DIAMOND -> "任意场次锁定最佳观影区";
            case PLATINUM -> "新片首日可选黄金座";
            case GOLD -> "新片可选最佳观影区";
            case SILVER -> "热门场次提前 1 小时选座";
            case NORMAL -> "升级银卡后解锁优先选座";
        };
    }

    private String serviceDetail() {
        return switch (this) {
            case DIAMOND, PLATINUM, GOLD -> "专属客服 7×24 小时响应";
            case SILVER -> "在线客服优先接入";
            case NORMAL -> "升级后享优先客服";
        };
    }

    private String birthdayDetail() {
        return switch (this) {
            case DIAMOND -> "生日当月赠 6 张观影券 + 双人套餐";
            case PLATINUM -> "生日当月赠 4 张观影券";
            case GOLD -> "生日当月赠 2 张观影券";
            case SILVER -> "生日当月赠 1 张观影券";
            case NORMAL -> "升级银卡后赠送观影券";
        };
    }

    /** 等级编码，与数据库 {@code membership_level.code} 一致。 */
    public String code() {
        return name();
    }

    /**
     * 按累计消费金额推导应处等级——用户消费越多等级越高，这就是升级的唯一依据。
     *
     * <p>遍历全部档位取「门槛不超过累计消费」的最高者，因此无论一次消费跨度多大
     * 都会直接落到对应档，不需要逐级点。累计消费为 0 或负数时返回 {@link #NORMAL}。</p>
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

    /** 从 {@code from} 升到本档还差多少分；已达标返回 0。 */
    public int remainingFrom(int spentFen) {
        return Math.max(0, thresholdFen - spentFen);
    }

    @Override
    public String toString() {
        return label;
    }

    static {
        // 声明顺序必须与门槛递增、折扣递减一致，配置写错时尽早暴露
        MembershipLevel[] all = values();
        for (int i = 1; i < all.length; i++) {
            if (all[i].thresholdFen <= all[i - 1].thresholdFen) {
                throw new IllegalStateException(String.format(Locale.ROOT,
                        "会员门槛必须递增：%s(%d) 未高于 %s(%d)",
                        all[i].name(), all[i].thresholdFen, all[i - 1].name(), all[i - 1].thresholdFen));
            }
            if (all[i].discountBasisPoints >= all[i - 1].discountBasisPoints) {
                throw new IllegalStateException(String.format(Locale.ROOT,
                        "会员折扣必须递减：%s(%d) 未低于 %s(%d)",
                        all[i].name(), all[i].discountBasisPoints, all[i - 1].name(), all[i - 1].discountBasisPoints));
            }
        }
    }
}
