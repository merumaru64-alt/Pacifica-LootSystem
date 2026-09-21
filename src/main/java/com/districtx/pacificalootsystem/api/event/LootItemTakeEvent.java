package com.districtx.pacificalootsystem.api.event;

import com.districtx.pacificalootsystem.api.LinkedLoot;
import org.bukkit.entity.Player;
import org.bukkit.event.Cancellable;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;
import org.bukkit.inventory.ItemStack;

/** Fired when a player interacts with an item in a loot inventory. */
public final class LootItemTakeEvent extends Event implements Cancellable {
    private static final HandlerList HANDLERS = new HandlerList();
    private final Player player;
    private final LinkedLoot linkedLoot;
    private final ItemStack item;
    private boolean cancelled;
    public LootItemTakeEvent(Player player, LinkedLoot linkedLoot, ItemStack item) { this.player = player; this.linkedLoot = linkedLoot; this.item = item; }
    public Player getPlayer() { return player; }
    public LinkedLoot getLinkedLoot() { return linkedLoot; }
    public ItemStack getItem() { return item; }
    @Override public boolean isCancelled() { return cancelled; }
    @Override public void setCancelled(boolean cancelled) { this.cancelled = cancelled; }
    @Override public HandlerList getHandlers() { return HANDLERS; }
    public static HandlerList getHandlerList() { return HANDLERS; }
}