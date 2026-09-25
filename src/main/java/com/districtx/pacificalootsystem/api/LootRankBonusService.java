package com.districtx.pacificalootsystem.api;

import org.bukkit.entity.Player;

import java.util.Optional;

/** Resolves one LuckPerms primary group for XP bonuses; inherited groups are not stacked. */
public interface LootRankBonusService {
    /** Returns whether LuckPerms is available for rank resolution. */
    boolean isAvailable();

    /** Returns the LuckPerms-configured primary group, or empty when unavailable. */
    Optional<String> getApplicableRank(Player player);

    /** Returns the XP-only multiplier fraction, e.g. 0.07 for a seven-percent bonus. */
    double getXpBonusPercent(Player player);
}