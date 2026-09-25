package com.districtx.pacificalootsystem.api;

/** Read-only outcome of a linked-container XP award attempt. */
public interface XpRewardResult {
    boolean isSuccessful();
    int getBaseXp();
    int getBonusXp();
    int getFinalXp();
    String getAppliedRank();
    double getBonusPercent();
}