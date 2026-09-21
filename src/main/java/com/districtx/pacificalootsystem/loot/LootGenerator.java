package com.districtx.pacificalootsystem.loot;

import com.districtx.pacificalootsystem.api.LootContext;
import com.districtx.pacificalootsystem.api.Collectable;
import com.districtx.pacificalootsystem.api.LootEntry;
import com.districtx.pacificalootsystem.api.LootResult;
import com.districtx.pacificalootsystem.api.LootTable;
import com.districtx.pacificalootsystem.api.LootGenerationService;
import com.districtx.pacificalootsystem.api.MoneyReward;
import com.districtx.pacificalootsystem.PacificaLootSystem;
import com.districtx.pacificalootsystem.condition.ConditionRegistry;
import com.districtx.pacificalootsystem.util.TextUtil;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

public final class LootGenerator implements LootGenerationService {
    private final PacificaLootSystem plugin;
    public LootGenerator() { this(null); }
    public LootGenerator(PacificaLootSystem plugin) { this.plugin = plugin; }
    public LootResult generate(LootContext context) { return generate(context, false); }
    public LootResult generate(LootContext context, boolean bypassConditions) {
        LootResult result = new LootResult(); LootTable table = context.getTable();
        if (table == null || !table.isEnabled()) { result.setFailureReason("disabled"); return result; }
        if (!bypassConditions && context.getPlayer() != null && table.getPermission() != null && !table.getPermission().isBlank() && !context.getPlayer().hasPermission(table.getPermission())) { result.setFailureReason("permission"); return result; }
        if (!bypassConditions && !conditionsPass(table.getConditions(), context)) { result.setFailureReason("condition"); return result; }
        List<Collectable> eligible = new ArrayList<>();
        for (Collectable collectable : table.getCollectables().values()) if (collectable.isEnabled() && collectable.getChance() > 0 && roll(collectable.getChance())) eligible.add(collectable);
        if (eligible.isEmpty()) { addMoneyRewards(result, table, null); result.setSuccessful(true); return result; }
        Collectable collectable = selectCollectable(eligible, table.getSelectionMode());
        List<LootEntry> candidates = new ArrayList<>();
        for (LootEntry entry : collectable.getEntries().values()) if (entry.isEnabled() && entry.getChance() > 0 && roll(entry.getChance()) && (bypassConditions || conditionsPass(entry.getConditions(), context))) candidates.add(entry);
        if (candidates.isEmpty()) { addMoneyRewards(result, table, collectable.getId()); result.setSuccessful(true); return result; }
        int minimum = Math.max(0, collectable.getMinimumRewards()); int maximum = Math.max(minimum, collectable.getMaximumRewards());
        int count = minimum + (maximum == minimum ? 0 : ThreadLocalRandom.current().nextInt(maximum - minimum + 1));
        for (LootEntry entry : select(candidates, collectable.getSelectionMode(), count)) addReward(result, entry);
        addMoneyRewards(result, table, collectable.getId());
        result.setSuccessful(true); return result;
    }
    private void addMoneyRewards(LootResult result, LootTable table, java.util.UUID collectableId) {
        for (MoneyReward reward : table.getMoneyRewards().values()) {
            if (!reward.isEnabled() || (reward.getCollectableId() != null && !reward.getCollectableId().equals(collectableId))) continue;
            result.setMoney(result.getMoney() + (plugin == null ? reward.generateAmount() : plugin.getEconomyService().generateAmount(reward.getMinimumAmount(), reward.getMaximumAmount())));
        }
    }
    private boolean conditionsPass(List<String> conditions, LootContext context) { if (conditions == null) return true; for (String condition : conditions) if (!ConditionRegistry.test(condition, context)) return false; return true; }
    private boolean roll(double chance) { return chance >= 100 || ThreadLocalRandom.current().nextDouble(100) < chance; }
    private Collectable selectCollectable(List<Collectable> collectables, String mode) {
        return "WEIGHTED".equalsIgnoreCase(mode) ? weightedCollectable(collectables) : collectables.get(ThreadLocalRandom.current().nextInt(collectables.size()));
    }
    private Collectable weightedCollectable(List<Collectable> collectables) {
        double total = collectables.stream().mapToDouble(Collectable::getWeight).filter(value -> value > 0).sum();
        if (total <= 0) return collectables.get(ThreadLocalRandom.current().nextInt(collectables.size()));
        double value = ThreadLocalRandom.current().nextDouble(total);
        for (Collectable collectable : collectables) { value -= Math.max(0, collectable.getWeight()); if (value < 0) return collectable; }
        return collectables.get(collectables.size() - 1);
    }
    private List<LootEntry> select(List<LootEntry> candidates, String mode, int count) {
        if ("ALL".equalsIgnoreCase(mode)) return new ArrayList<>(candidates);
        List<LootEntry> pool = new ArrayList<>(candidates); List<LootEntry> selected = new ArrayList<>();
        boolean unique = "UNIQUE".equalsIgnoreCase(mode); int target = Math.min(count, unique ? pool.size() : count);
        for (int i = 0; i < target && !pool.isEmpty(); i++) { LootEntry chosen = "WEIGHTED".equalsIgnoreCase(mode) ? weighted(pool) : pool.get(ThreadLocalRandom.current().nextInt(pool.size())); selected.add(chosen); if (unique) pool.remove(chosen); }
        return selected;
    }
    private LootEntry weighted(List<LootEntry> entries) {
        double total = entries.stream().mapToDouble(LootEntry::getWeight).filter(value -> value > 0).sum(); if (total <= 0) return entries.get(ThreadLocalRandom.current().nextInt(entries.size()));
        double value = ThreadLocalRandom.current().nextDouble(total); for (LootEntry entry : entries) { value -= Math.max(0, entry.getWeight()); if (value < 0) return entry; } return entries.get(entries.size() - 1);
    }
    private void addReward(LootResult result, LootEntry entry) {
        int amount = randomInt(entry.getMinimumAmount(), entry.getMaximumAmount());
        switch (entry.getType().toUpperCase()) {
            case "ITEM" -> {
                if (entry.getItem() == null) return; ItemStack base = entry.getItem().clone();
                if ((entry.getName() != null && !entry.getName().isBlank()) || (entry.getLore() != null && !entry.getLore().isEmpty())) {
                    org.bukkit.inventory.meta.ItemMeta meta = base.getItemMeta(); if (entry.getName() != null && !entry.getName().isBlank()) meta.setDisplayName(TextUtil.color(entry.getName())); if (entry.getLore() != null && !entry.getLore().isEmpty()) meta.setLore(TextUtil.color(entry.getLore())); base.setItemMeta(meta);
                }
                int remaining = amount; while (remaining > 0) { ItemStack stack = base.clone(); int stackAmount = Math.min(remaining, base.getMaxStackSize()); stack.setAmount(stackAmount); result.addItem(stack, entry.isAutoPickup()); remaining -= stackAmount; }
            }
            case "COMMAND" -> { if (entry.getCommand() != null) result.getCommands().add(entry.getCommand()); }
            case "MONEY" -> result.setMoney(result.getMoney() + (plugin == null ? randomDouble(entry.getMinimumMoney(), entry.getMaximumMoney()) : plugin.getEconomyService().generateAmount(entry.getMinimumMoney(), entry.getMaximumMoney())));
            case "EXP" -> { int experience = randomInt(entry.getMinimumExperience(), entry.getMaximumExperience()); if (entry.isExperienceLevels()) result.setLevels(result.getLevels() + experience); else result.setExperience(result.getExperience() + experience); }
            default -> { }
        }
    }
    private int randomInt(int min, int max) { return min + (max <= min ? 0 : ThreadLocalRandom.current().nextInt(max - min + 1)); }
    private double randomDouble(double min, double max) { return min + (max <= min ? 0 : ThreadLocalRandom.current().nextDouble(max - min)); }
}