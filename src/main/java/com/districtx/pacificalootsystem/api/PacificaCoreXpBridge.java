package com.districtx.pacificalootsystem.api;

import org.bukkit.entity.Player;

/** Integration boundary for Pacifica-Core's permanent Player Level XP API. */
public interface PacificaCoreXpBridge {
    /** Returns whether Pacifica-Core and its Player Level service are available. */
    boolean isAvailable();

    /** Grants positive XP through Pacifica-Core and reports whether XP was applied. */
    boolean awardXp(Player player, int amount);

    /** Gets the player's stored Pacifica-Core XP, floored to an integer. */
    int getPlayerXp(Player player);
}