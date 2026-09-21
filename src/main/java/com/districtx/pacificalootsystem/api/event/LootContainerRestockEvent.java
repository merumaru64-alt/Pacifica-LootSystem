package com.districtx.pacificalootsystem.api.event;

import com.districtx.pacificalootsystem.api.LinkedLoot;
import org.bukkit.entity.Player;
import org.bukkit.event.Cancellable;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;

/** Fired before an expired linked container is regenerated. */
public final class LootContainerRestockEvent extends Event implements Cancellable {
    private static final HandlerList HANDLERS = new HandlerList();
    private final Player player;
    private final LinkedLoot linkedLoot;
    private boolean cancelled;
    public LootContainerRestockEvent(Player player, LinkedLoot linkedLoot) { this.player = player; this.linkedLoot = linkedLoot; }
    public Player getPlayer() { return player; }
    public LinkedLoot getLinkedLoot() { return linkedLoot; }
    @Override public boolean isCancelled() { return cancelled; }
    @Override public void setCancelled(boolean cancelled) { this.cancelled = cancelled; }
    @Override public HandlerList getHandlers() { return HANDLERS; }
    public static HandlerList getHandlerList() { return HANDLERS; }
}