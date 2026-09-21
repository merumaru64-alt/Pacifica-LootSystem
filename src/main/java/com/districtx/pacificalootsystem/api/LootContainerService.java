package com.districtx.pacificalootsystem.api;

import org.bukkit.entity.Player;

/** Opens Pacifica physical loot inventories. */
public interface LootContainerService {
    /** Opens a linked physical container for a player. */
    boolean open(Player player, LinkedLoot linkedLoot);
    /** Opens generated physical loot for a player. */
    boolean open(Player player, LootTable table, LootResult result);
}