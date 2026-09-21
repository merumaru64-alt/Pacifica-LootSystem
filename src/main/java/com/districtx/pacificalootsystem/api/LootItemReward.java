package com.districtx.pacificalootsystem.api;

import org.bukkit.inventory.ItemStack;

/** Public view of a configurable item reward. */
public interface LootItemReward {
    /** Returns the configured item template. */
    ItemStack getItem();
    /** Returns the inclusive lower quantity bound. */
    int getMinimumAmount();
    /** Returns the inclusive upper quantity bound. */
    int getMaximumAmount();
    /** Returns whether generated items should be inserted into the player inventory first. */
    boolean isAutoPickup();
    /** Returns the selection weight. */
    double getWeight();
    /** Generates an inclusive random quantity in the configured range. */
    int generateAmount();
}