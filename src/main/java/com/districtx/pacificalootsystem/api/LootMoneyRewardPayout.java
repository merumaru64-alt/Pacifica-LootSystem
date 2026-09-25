package com.districtx.pacificalootsystem.api;

import lombok.AllArgsConstructor;
import lombok.Data;

/** One generated money reward and its rolled amount. */
@Data
@AllArgsConstructor
public final class LootMoneyRewardPayout {
    private LootMoneyReward reward;
    private double amount;
}