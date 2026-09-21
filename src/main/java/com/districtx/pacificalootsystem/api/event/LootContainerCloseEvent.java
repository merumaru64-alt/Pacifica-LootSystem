package com.districtx.pacificalootsystem.api.event;

import com.districtx.pacificalootsystem.api.LinkedLoot;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;

/** Fired when a loot inventory session ends. */
public final class LootContainerCloseEvent extends Event {
    private static final HandlerList HANDLERS = new HandlerList();
    private final Player player;
    private final LinkedLoot linkedLoot;
    public LootContainerCloseEvent(Player player, LinkedLoot linkedLoot) { this.player = player; this.linkedLoot = linkedLoot; }
    public Player getPlayer() { return player; }
    public LinkedLoot getLinkedLoot() { return linkedLoot; }
    @Override public HandlerList getHandlers() { return HANDLERS; }
    public static HandlerList getHandlerList() { return HANDLERS; }
}