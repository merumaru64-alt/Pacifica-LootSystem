package com.districtx.pacificalootsystem.api.event;

import com.districtx.pacificalootsystem.api.LinkedLoot;
import com.districtx.pacificalootsystem.api.LootTable;
import org.bukkit.entity.Player;
import org.bukkit.event.Cancellable;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;

/** Fired before a linked-container XP reward is committed to Pacifica-Core. */
public final class LootXpRewardEvent extends Event implements Cancellable {
    private static final HandlerList HANDLERS = new HandlerList();
    private final Player player;
    private final LinkedLoot linkedLoot;
    private final LootTable lootTable;
    private final int baseXp;
    private int finalXp;
    private final String appliedRank;
    private final double bonusPercent;
    private boolean cancelled;

    public LootXpRewardEvent(Player player, LinkedLoot linkedLoot, LootTable lootTable, int baseXp,
                             int finalXp, String appliedRank, double bonusPercent) {
        this.player = player;
        this.linkedLoot = linkedLoot;
        this.lootTable = lootTable;
        this.baseXp = baseXp;
        this.finalXp = finalXp;
        this.appliedRank = appliedRank;
        this.bonusPercent = bonusPercent;
    }

    public Player getPlayer() { return player; }
    public LinkedLoot getLinkedLoot() { return linkedLoot; }
    public LootTable getLootTable() { return lootTable; }
    public int getBaseXp() { return baseXp; }
    public int getFinalXp() { return finalXp; }
    public void setFinalXp(int finalXp) { if (finalXp >= 0) this.finalXp = finalXp; }
    public String getAppliedRank() { return appliedRank; }
    public double getBonusPercent() { return bonusPercent; }
    @Override public boolean isCancelled() { return cancelled; }
    @Override public void setCancelled(boolean cancelled) { this.cancelled = cancelled; }
    @Override public HandlerList getHandlers() { return HANDLERS; }
    public static HandlerList getHandlerList() { return HANDLERS; }
}