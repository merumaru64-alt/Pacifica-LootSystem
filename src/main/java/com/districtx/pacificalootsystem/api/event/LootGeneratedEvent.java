package com.districtx.pacificalootsystem.api.event;

import com.districtx.pacificalootsystem.api.LootResult;
import com.districtx.pacificalootsystem.api.LootTable;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;

/** Fired after a loot result has been generated. */
public final class LootGeneratedEvent extends Event {
    private static final HandlerList HANDLERS = new HandlerList();
    private final Player player;
    private final LootTable lootTable;
    private final LootResult result;
    public LootGeneratedEvent(Player player, LootTable lootTable, LootResult result) { this.player = player; this.lootTable = lootTable; this.result = result; }
    public Player getPlayer() { return player; }
    public LootTable getLootTable() { return lootTable; }
    public LootResult getResult() { return result; }
    @Override public HandlerList getHandlers() { return HANDLERS; }
    public static HandlerList getHandlerList() { return HANDLERS; }
}