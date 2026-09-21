package com.districtx.pacificalootsystem.api;

import com.districtx.pacificalootsystem.PacificaLootSystem;
import com.districtx.pacificalootsystem.condition.ConditionRegistry;
import com.districtx.pacificalootsystem.condition.RewardRegistry;
import com.districtx.pacificalootsystem.condition.TriggerRegistry;
import com.districtx.pacificalootsystem.service.BukkitLootEventService;
import org.bukkit.entity.Player;

import java.util.Collection;

public final class LootSystemAPI implements PacificaLootAPI {
    private final PacificaLootSystem plugin;
    private final LootEventService eventService = new BukkitLootEventService();
    public LootSystemAPI(PacificaLootSystem plugin) { this.plugin = plugin; }
    public LootTable getLootTable(String id) { return plugin.getTableManager().get(id); }
    public Collection<LootTable> getLootTables() { return plugin.getTableManager().all(); }
    public LootResult generate(Player player, String id) { LootTable table = getLootTable(id); return plugin.getLootGenerator().generate(new LootContext(player, player, player.getLocation(), player.getInventory().getItemInMainHand(), table, "API")); }
    public LootResult generate(LootContext context, String id) { context.setTable(getLootTable(id)); return plugin.getLootGenerator().generate(context); }
    public boolean canLoot(Player player, String id) { return plugin.getLootService().canLoot(player, id); }
    public boolean isOnCooldown(Player player, String id) { return plugin.getCooldownManager().isOnCooldown(player, getLootTable(id)); }
    public void resetCooldown(Player player, String id) { plugin.getCooldownManager().reset(player, getLootTable(id)); }
    public void giveLoot(Player player, LootResult result) { plugin.getLootService().deliver(player, result, null); }
    public LinkedLootService getLinkedLootService() { return plugin.getLinkedLootService(); }
    public com.districtx.pacificalootsystem.loot.PhysicalLootService getPhysicalLootService() { return plugin.getPhysicalLootService(); }
    public com.districtx.pacificalootsystem.loot.LootBagService getLootBagService() { return plugin.getLootBagService(); }
    public EconomyService getEconomyService() { return plugin.getEconomyService(); }
    @Override public LootTableService getLootTableService() { return plugin.getTableManager(); }
    @Override public LootGenerationService getLootGenerationService() { return plugin.getLootGenerator(); }
    @Override public LootRewardService getLootRewardService() { return plugin.getLootService(); }
    @Override public LootContainerService getLootContainerService() { return plugin.getPhysicalLootService(); }
    @Override public LootHologramService getLootHologramService() { return plugin.getHologramManager(); }
    @Override public LootEventService getEventService() { return eventService; }
    public void registerCondition(LootCondition condition) { ConditionRegistry.register(condition); }
    public void registerRewardType(LootRewardType rewardType) { RewardRegistry.register(rewardType); }
    public void registerTrigger(LootTrigger trigger) { TriggerRegistry.register(trigger); }
}