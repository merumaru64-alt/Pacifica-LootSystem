package com.districtx.pacificalootsystem.loot;

import com.cryptomorin.xseries.XSound;
import com.districtx.pacificalootsystem.PacificaLootSystem;
import com.districtx.pacificalootsystem.api.LootContext;
import com.districtx.pacificalootsystem.api.LootResult;
import com.districtx.pacificalootsystem.api.LootTable;
import com.districtx.pacificalootsystem.api.LootRewardService;
import com.districtx.pacificalootsystem.api.event.LootGeneratedEvent;
import com.districtx.pacificalootsystem.api.event.LootMoneyRewardEvent;
import com.districtx.pacificalootsystem.util.MessageUtil;
import com.districtx.pacificalootsystem.util.TextUtil;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.HashMap;
import java.util.Map;
import net.milkbowl.vault.economy.EconomyResponse;

public final class LootService implements LootRewardService {
    private final PacificaLootSystem plugin;
    public LootService(PacificaLootSystem plugin) { this.plugin = plugin; }
    public boolean canLoot(Player player, String id) { LootTable table = plugin.getTableManager().get(id); return table != null && table.isEnabled() && (table.getPermission() == null || table.getPermission().isBlank() || player.hasPermission(table.getPermission())) && !plugin.getCooldownManager().isOnCooldown(player, table); }
    public synchronized LootResult claim(Player player, String id, boolean bypass, boolean test) {
        return claimInternal(player, id, bypass, test, false);
    }
    public synchronized LootResult claimLinked(Player player, String id, boolean bypass, boolean test) {
        return failed("use-linked-container");
    }
    private LootResult claimInternal(Player player, String id, boolean bypass, boolean test, boolean linked) {
        LootTable table = plugin.getTableManager().get(id); if (table == null) return failed("invalid"); if (!table.isEnabled()) return failed("disabled");
        if (!bypass && table.getPermission() != null && !table.getPermission().isBlank() && !player.hasPermission(table.getPermission())) return failed("permission");
        if (!test && !bypass && (linked ? plugin.getCooldownManager().isOnGlobalCooldown(table) : plugin.getCooldownManager().isOnCooldown(player, table))) return failed("cooldown");
        LootResult result = plugin.getLootGenerator().generate(new LootContext(player, null, player.getLocation(), player.getInventory().getItemInMainHand(), table, "COMMAND"), bypass || player.hasPermission("pacifica.lootsystem.bypass.conditions"));
        if (result.hasRewards() && !test && !bypass) { if (linked) plugin.getCooldownManager().markGlobal(table); else plugin.getCooldownManager().mark(player, table); }
        if (result.hasRewards()) {
            boolean physical = !test && !bypass && "PHYSICAL".equalsIgnoreCase(table.getLootMode())
                && plugin.getConfig().getBoolean("physical-loot.enabled", true);
            if (physical) {
                deliverNonItemRewards(player, result, table);
                if (result.getItems().isEmpty() || !plugin.getPhysicalLootService().open(player, table, result)) deliverItems(player, result.getItems());
            } else {
                plugin.getApi().getEventService().call(new LootGeneratedEvent(player, table, result));
                deliver(player, result, table);
            }
        }
        return result;
    }
    public void deliver(Player player, LootResult result, LootTable table) {
        deliverItems(player, result.getItems());
        deliverNonItemRewards(player, result, table);
        if (result.hasRewards()) XSound.matchXSound(plugin.getConfig().getString("sounds.loot-success", "ENTITY_PLAYER_LEVELUP")).ifPresent(sound -> sound.play(player));
    }
    public void deliverItems(Player player, java.util.List<ItemStack> items) {
        for (ItemStack item : items) {
            if (item == null || item.getType().isAir()) continue;
            Map<Integer, ItemStack> leftovers = player.getInventory().addItem(item.clone());
            leftovers.values().forEach(leftover -> player.getWorld().dropItemNaturally(player.getLocation(), leftover));
        }
    }
    public void deliverNonItemRewards(Player player, LootResult result, LootTable table) { if (result.getExperience() > 0) player.giveExp(result.getExperience()); if (result.getLevels() > 0) player.giveExpLevels(result.getLevels()); if (result.getMoney() > 0) { LootMoneyRewardEvent moneyEvent = new LootMoneyRewardEvent(player, table, result.getMoney()); plugin.getApi().getEventService().call(moneyEvent); if (!moneyEvent.isCancelled()) deposit(player, result.getMoney(), table); } for (String command : result.getCommands()) Bukkit.dispatchCommand(Bukkit.getConsoleSender(), placeholders(command, player, table)); }
    private void deposit(Player player, double amount, LootTable table) {
        if (!plugin.getConfig().getBoolean("economy.enabled", true) || !plugin.getConfig().getBoolean("economy.deposit.enabled", true) || !plugin.getEconomyService().isAvailable()) {
            MessageUtil.send(plugin, player, "economy.unavailable");
            return;
        }
        EconomyResponse response = plugin.getEconomyService().deposit(player, amount);
        if (!response.transactionSuccess()) {
            plugin.getLogger().warning("Money deposit failed for " + player.getName() + ": " + response.errorMessage);
            MessageUtil.send(plugin, player, "economy.deposit-failed");
            return;
        }
        if (plugin.getConfig().getBoolean("economy.deposit.notify-player", true)) {
            String lootName = table == null ? "loot" : (table.getDisplayName() == null ? table.getId() : table.getDisplayName());
            MessageUtil.send(plugin, player, "economy.received", Map.of("money", String.format("%.2f", response.amount), "loot-name", lootName));
        }
    }
    private String placeholders(String command, Player player, LootTable table) { String value = command.startsWith("/") ? command.substring(1) : command; Map<String, String> values = new HashMap<>(); values.put("player", player.getName()); values.put("player_name", player.getName()); values.put("player_uuid", player.getUniqueId().toString()); values.put("world", player.getWorld().getName()); values.put("x", String.valueOf(player.getLocation().getBlockX())); values.put("y", String.valueOf(player.getLocation().getBlockY())); values.put("z", String.valueOf(player.getLocation().getBlockZ())); values.put("loot_table", table == null ? "" : table.getId()); for (Map.Entry<String, String> entry : values.entrySet()) value = value.replace("%" + entry.getKey() + "%", entry.getValue()); return TextUtil.color(value); }
    private LootResult failed(String reason) { LootResult result = new LootResult(); result.setFailureReason(reason); return result; }
}