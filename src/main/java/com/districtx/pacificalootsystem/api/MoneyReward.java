package com.districtx.pacificalootsystem.api;

import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;

@Getter
@Setter
public final class MoneyReward implements LootMoneyReward {
    private UUID id;
    private UUID lootTableId;
    private UUID collectableId;
    private double minimumAmount;
    private double maximumAmount;
    private boolean enabled = true;

    public MoneyReward(UUID id, UUID lootTableId, UUID collectableId, double minimumAmount, double maximumAmount, boolean enabled) {
        this.id = id;
        this.lootTableId = lootTableId;
        this.collectableId = collectableId;
        this.minimumAmount = Math.max(0, minimumAmount);
        this.maximumAmount = Math.max(this.minimumAmount, maximumAmount);
        this.enabled = enabled;
    }

    public double generateAmount() {
        BigDecimal minimum = BigDecimal.valueOf(minimumAmount).setScale(2, RoundingMode.DOWN);
        BigDecimal maximum = BigDecimal.valueOf(maximumAmount).setScale(2, RoundingMode.DOWN);
        long low = minimum.movePointRight(2).longValue();
        long high = maximum.movePointRight(2).longValue();
        long value = high <= low ? low : ThreadLocalRandom.current().nextLong(low, high + 1);
        return BigDecimal.valueOf(value, 2).doubleValue();
    }
}