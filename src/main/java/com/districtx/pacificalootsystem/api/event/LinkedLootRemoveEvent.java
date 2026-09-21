package com.districtx.pacificalootsystem.api.event;

import com.districtx.pacificalootsystem.api.LinkedLoot;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;

/** Fired when a physical container link is removed. */
public final class LinkedLootRemoveEvent extends Event {
    private static final HandlerList HANDLERS = new HandlerList();
    private final LinkedLoot linkedLoot;
    public LinkedLootRemoveEvent(LinkedLoot linkedLoot) { this.linkedLoot = linkedLoot; }
    public LinkedLoot getLinkedLoot() { return linkedLoot; }
    @Override public HandlerList getHandlers() { return HANDLERS; }
    public static HandlerList getHandlerList() { return HANDLERS; }
}