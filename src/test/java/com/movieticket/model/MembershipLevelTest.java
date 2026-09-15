package com.movieticket.model;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MembershipLevelTest {

    @Test
    void valuesFollowUpgradeOrder() {
        assertEquals(List.of(MembershipLevel.NORMAL, MembershipLevel.SILVER, MembershipLevel.GOLD,
                        MembershipLevel.PLATINUM, MembershipLevel.DIAMOND),
                List.of(MembershipLevel.values()));
    }

    @Test
    void nextAdvancesOneTierAndStopsAtTop() {
        assertEquals(MembershipLevel.SILVER, MembershipLevel.NORMAL.next());
        assertEquals(MembershipLevel.GOLD, MembershipLevel.SILVER.next());
        assertEquals(MembershipLevel.PLATINUM, MembershipLevel.GOLD.next());
        assertEquals(MembershipLevel.DIAMOND, MembershipLevel.PLATINUM.next());
        assertNull(MembershipLevel.DIAMOND.next());
    }

    @Test
    void onlyDiamondIsTopTier() {
        for (MembershipLevel level : MembershipLevel.values()) {
            assertEquals(level == MembershipLevel.DIAMOND, level.isTop(), level.name());
        }
        assertTrue(MembershipLevel.DIAMOND.isTop());
        assertFalse(MembershipLevel.PLATINUM.isTop());
    }

    @Test
    void thresholdIncreasesStrictlyAndStartsFree() {
        assertEquals(0, MembershipLevel.NORMAL.thresholdFen());
        MembershipLevel[] levels = MembershipLevel.values();
        for (int i = 1; i < levels.length; i++) {
            assertTrue(levels[i].thresholdFen() > levels[i - 1].thresholdFen(),
                    levels[i] + " 门槛应高于 " + levels[i - 1]);
        }
    }

    @Test
    void discountDecreasesStrictly() {
        MembershipLevel[] levels = MembershipLevel.values();
        for (int i = 1; i < levels.length; i++) {
            assertTrue(levels[i].discountBasisPoints() < levels[i - 1].discountBasisPoints(),
                    levels[i] + " 折扣应低于 " + levels[i - 1]);
        }
    }

    @Test
    void onlyNormalHasNoDiscount() {
        assertFalse(MembershipLevel.NORMAL.hasDiscount());
        assertEquals("原价", MembershipLevel.NORMAL.discountLabel());
        for (MembershipLevel level : MembershipLevel.values()) {
            if (level != MembershipLevel.NORMAL) {
                assertTrue(level.hasDiscount(), level.name());
                assertEquals(level.discountText(), level.discountLabel());
            }
        }
    }

    @Test
    void everyTierProvidesFourNonBlankBenefits() {
        for (MembershipLevel level : MembershipLevel.values()) {
            List<String[]> benefits = level.benefitLines();
            assertEquals(4, benefits.size(), level + " 应有 4 条权益");
            for (String[] benefit : benefits) {
                assertEquals(2, benefit.length);
                assertNotNull(benefit[0]);
                assertFalse(benefit[0].isBlank(), level + " 权益标题不应为空");
                assertFalse(benefit[1].isBlank(), level + " 权益说明不应为空");
            }
        }
    }

    @Test
    void higherTierNeverCostsMoreThanLowerTier() {
        int samplePrice = 4500;
        MembershipLevel[] levels = MembershipLevel.values();
        for (int i = 1; i < levels.length; i++) {
            int previous = Money.applyDiscount(samplePrice, levels[i - 1].discountBasisPoints());
            int current = Money.applyDiscount(samplePrice, levels[i].discountBasisPoints());
            assertTrue(current < previous,
                    levels[i] + " 折后价应低于 " + levels[i - 1]);
        }
    }

    @Test
    void codeMatchesEnumName() {
        for (MembershipLevel level : MembershipLevel.values()) {
            assertEquals(level.name(), level.code());
        }
    }

    // ---------- 按累计消费推导等级 ----------

    @Test
    void spendingAtOrAboveThresholdUnlocksThatTier() {
        for (MembershipLevel level : MembershipLevel.values()) {
            assertEquals(level, MembershipLevel.forSpending(level.thresholdFen()),
                    "刚好踩线应解锁 " + level);
            assertEquals(level, MembershipLevel.forSpending(level.thresholdFen() + 1),
                    "超过门槛应保持 " + level);
        }
    }

    @Test
    void spendingJustBelowThresholdKeepsLowerTier() {
        MembershipLevel[] levels = MembershipLevel.values();
        for (int i = 1; i < levels.length; i++) {
            assertEquals(levels[i - 1], MembershipLevel.forSpending(levels[i].thresholdFen() - 1),
                    "差 1 分不应升到 " + levels[i]);
        }
    }

    @Test
    void zeroOrNegativeSpendingMeansNormal() {
        assertEquals(MembershipLevel.NORMAL, MembershipLevel.forSpending(0));
        assertEquals(MembershipLevel.NORMAL, MembershipLevel.forSpending(-100));
    }

    @Test
    void spendingFarAboveTopThresholdStaysAtTop() {
        assertEquals(MembershipLevel.DIAMOND,
                MembershipLevel.forSpending(MembershipLevel.DIAMOND.thresholdFen() * 10));
    }

    @Test
    void derivedTierIsMonotonicInSpending() {
        MembershipLevel previous = MembershipLevel.NORMAL;
        // 逐分递增扫描，等级只应前进不应回退
        for (int spent = 0; spent <= MembershipLevel.DIAMOND.thresholdFen() + 1000; spent += 500) {
            MembershipLevel current = MembershipLevel.forSpending(spent);
            assertTrue(current.ordinal() >= previous.ordinal(), "消费 " + spent + " 时等级回退");
            previous = current;
        }
    }

    @Test
    void remainingFromReportsGapAndFloorsAtZero() {
        int target = MembershipLevel.SILVER.thresholdFen();
        assertEquals(target, MembershipLevel.SILVER.remainingFrom(0));
        assertEquals(1, MembershipLevel.SILVER.remainingFrom(target - 1));
        assertEquals(0, MembershipLevel.SILVER.remainingFrom(target));
        assertEquals(0, MembershipLevel.SILVER.remainingFrom(target + 999));
    }
}
