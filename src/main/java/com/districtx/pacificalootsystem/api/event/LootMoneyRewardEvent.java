package com.districtx.pacificalootsystem.api.event;

import com.districtx.pacificalootsystem.api.LootTable;
import com.districtx.pacificalootsystem.api.LootMoneyReward;
import org.bukkit.entity.Player;
import org.bukkit.event.Cancellable;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;

import java.math.BigDecimal;

/** Fired before generated money is deposited. */
public final class LootMoneyRewardEvent extends Event implements Cancellable {
    private static final HandlerList HANDLERS = new HandlerList();
    private final Player player;
    private final LootTable lootTable;
    private final LootMoneyReward reward;
    private final double amount;
    private boolean cancelled;
    public LootMoneyRewardEvent(Player player, LootTable lootTable, double amount) { this(player, lootTable, null, amount); }
    public LootMoneyRewardEvent(Player player, LootTable lootTable, LootMoneyReward reward, double amount) { this.player = player; this.lootTable = lootTable; this.reward = reward; this.amount = amount; }
    public Player getPlayer() { return player; }
    public LootTable getLootTable() { return lootTable; }
    public LootMoneyReward getReward() { return reward; }
    public double getAmount() { return amount; }
    public BigDecimal getDecimalAmount() { return BigDecimal.valueOf(amount); }
    @Override public boolean isCancelled() { return cancelled; }
    @Override public void setCancelled(boolean cancelled) { this.cancelled = cancelled; }
    @Override public HandlerList getHandlers() { return HANDLERS; }
    public static HandlerList getHandlerList() { return HANDLERS; }
}