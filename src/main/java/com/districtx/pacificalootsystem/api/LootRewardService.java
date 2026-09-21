package com.districtx.pacificalootsystem.api;

import org.bukkit.entity.Player;

/** Claims and delivers generated rewards. */
public interface LootRewardService {
    /** Claims a table for a player. */
    LootResult claim(Player player, String tableName, boolean bypass, boolean test);
    /** Delivers all generated rewards to a player. */
    void deliver(Player player, LootResult result, LootTable table);
}