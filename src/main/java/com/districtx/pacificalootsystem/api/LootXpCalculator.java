package com.districtx.pacificalootsystem.api;

import org.bukkit.entity.Player;

/** Calculates table XP and rank-adjusted XP without granting it. Fractional final XP is floored. */
public interface LootXpCalculator {
    /** Generates an inclusive random XP value from the table's configured bounds. */
    int calculateBaseXp(LootTable lootTable);

    /** Calculates the XP added by the applicable single rank bonus. */
    int calculateBonusXp(Player player, int baseXp);

    /** Calculates final XP after applying at most one configured rank bonus, using floor rounding. */
    int calculateFinalXp(Player player, int baseXp);
}