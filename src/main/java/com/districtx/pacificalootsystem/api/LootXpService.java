package com.districtx.pacificalootsystem.api;

import org.bukkit.entity.Player;

/** Calculates and grants XP through Pacifica-Core for one linked-container restock state. */
public interface LootXpService extends LootXpCalculator {
    /** Returns whether XP rewards are enabled and Pacifica-Core's XP API is reachable. */
    boolean isAvailable();

    /** Attempts the one-time XP transaction for this physical linked container. */
    XpRewardResult awardXp(Player player, LinkedLoot linkedLoot, int baseXp);
}