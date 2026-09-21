package com.districtx.pacificalootsystem.api;

/** Public view of a configurable money reward. */
public interface LootMoneyReward {
    /** Returns the inclusive lower money bound. */
    double getMinimumAmount();
    /** Returns the inclusive upper money bound. */
    double getMaximumAmount();
    /** Generates a randomized amount in the configured range. */
    double generateAmount();
}