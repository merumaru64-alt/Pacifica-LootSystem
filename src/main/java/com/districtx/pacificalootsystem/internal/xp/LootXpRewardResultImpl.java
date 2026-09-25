package com.districtx.pacificalootsystem.internal.xp;

import com.districtx.pacificalootsystem.api.XpRewardResult;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
final class LootXpRewardResultImpl implements XpRewardResult {
    private final boolean successful;
    private final int baseXp;
    private final int bonusXp;
    private final int finalXp;
    private final String appliedRank;
    private final double bonusPercent;
}