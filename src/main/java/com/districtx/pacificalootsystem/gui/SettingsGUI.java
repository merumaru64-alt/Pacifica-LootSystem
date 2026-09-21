package com.districtx.pacificalootsystem.gui;

import com.districtx.pacificalootsystem.PacificaLootSystem;
import com.districtx.pacificalootsystem.api.LootTable;
import com.districtx.pacificalootsystem.api.MoneyReward;
import com.districtx.pacificalootsystem.util.ItemUtil;
import com.districtx.pacificalootsystem.util.MessageUtil;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;

import java.util.List;
import java.util.Map;
import java.util.UUID;

public final class SettingsGUI extends BaseGUI {
    private final LootTable table;
    public SettingsGUI(PacificaLootSystem plugin, LootTable table) { super(plugin); this.table = table; render(); }
    private void render() {
        inventory = org.bukkit.Bukkit.createInventory(this, 27, "Settings: " + table.getId());
        inventory.setItem(10, ItemUtil.icon("HOPPER", "&bSelection Mode", List.of("&7Current: " + table.getSelectionMode(), "&7Click cycles RANDOM, WEIGHTED, ALL, UNIQUE")));
        inventory.setItem(11, ItemUtil.icon("PAPER", "&bReward Bounds", List.of("&7Current: " + table.getMinimumRewards() + "-" + table.getMaximumRewards(), "&7Enter min-max in chat")));
        inventory.setItem(12, ItemUtil.icon("CLOCK", "&bCooldown", List.of("&7Seconds: " + table.getCooldownSeconds())));
        inventory.setItem(13, ItemUtil.icon("CHEST", "&bLoot Mode", List.of("&7Current: PHYSICAL")));
        inventory.setItem(14, ItemUtil.icon(table.isEnabled() ? "LIME_DYE" : "GRAY_DYE", table.isEnabled() ? "&aEnabled" : "&cDisabled", List.of("&7Click to toggle")));
        inventory.setItem(15, ItemUtil.icon(table.isGlobalMode() ? "REDSTONE_BLOCK" : "IRON_BLOCK", "&bLoot Scope", List.of(table.isGlobalMode() ? "&7Global" : "&7Individual", "&7Click to toggle")));
        MoneyReward money = table.getMoneyRewards().values().stream().filter(reward -> reward.getCollectableId() == null).findFirst().orElse(null);
        inventory.setItem(16, ItemUtil.icon("GOLD_INGOT", "&bMoney Reward", List.of("&7Minimum: " + (money == null ? "0" : money.getMinimumAmount()), "&7Maximum: " + (money == null ? "0" : money.getMaximumAmount()), "&7Enter min-max in chat")));
        inventory.setItem(22, ItemUtil.icon("EMERALD", "&aSave", List.of())); inventory.setItem(26, ItemUtil.icon("ARROW", "&eBack", List.of()));
    }
    @Override protected void click(InventoryClickEvent event) {
        Player player = (Player) event.getWhoClicked();
        switch (event.getRawSlot()) {
            case 10 -> { String[] modes = {"RANDOM", "WEIGHTED", "ALL", "UNIQUE", "ROUND"}; int current = 0; for (int i = 0; i < modes.length; i++) if (modes[i].equalsIgnoreCase(table.getSelectionMode())) current = i; table.setSelectionMode(modes[(current + 1) % modes.length]); render(); player.openInventory(inventory); }
            case 11 -> plugin.requestInput(player, "Enter reward bounds such as 1-3", value -> { try { String[] parts = value.split("-"); int min = Integer.parseInt(parts[0]); int max = parts.length > 1 ? Integer.parseInt(parts[1]) : min; if (min < 0 || max < min) throw new NumberFormatException(); table.setMinimumRewards(min); table.setMaximumRewards(max); render(); player.openInventory(inventory); } catch (NumberFormatException exception) { player.sendMessage("§cUse non-negative bounds such as 1-3."); } });
            case 12 -> plugin.requestInput(player, "Enter cooldown seconds, or 0", value -> { try { long seconds = Long.parseLong(value); if (seconds < 0) throw new NumberFormatException(); table.setCooldownSeconds(seconds); render(); player.openInventory(inventory); } catch (NumberFormatException exception) { player.sendMessage("§cCooldown must be a non-negative number."); } });
            case 13 -> { table.setLootMode("PHYSICAL"); render(); player.openInventory(inventory); }
            case 14 -> { table.setEnabled(!table.isEnabled()); render(); player.openInventory(inventory); }
            case 15 -> { table.setGlobalMode(!table.isGlobalMode()); render(); player.openInventory(inventory); }
            case 16 -> plugin.requestInput(player, "Enter money bounds such as 50-200", value -> {
                try {
                    String[] parts = value.split("-", -1);
                    double minimum = Double.parseDouble(parts[0]);
                    double maximum = parts.length > 1 ? Double.parseDouble(parts[1]) : minimum;
                    if (!Double.isFinite(minimum) || !Double.isFinite(maximum) || minimum < 0 || maximum < minimum) throw new NumberFormatException();
                    MoneyReward reward = table.getMoneyRewards().values().stream().filter(candidate -> candidate.getCollectableId() == null).findFirst().orElse(null);
                    if (reward == null) {
                        reward = new MoneyReward(UUID.randomUUID(), table.getUuid(), null, minimum, maximum, true);
                        table.getMoneyRewards().put(reward.getId(), reward);
                    } else {
                        reward.setMinimumAmount(minimum);
                        reward.setMaximumAmount(maximum);
                    }
                    render();
                    player.openInventory(inventory);
                } catch (NumberFormatException exception) {
                    player.sendMessage("§cUse non-negative money bounds such as 50-200.");
                }
            });
            case 22 -> { plugin.getTableManager().save(); MessageUtil.send(plugin, player, "loot-saved", Map.of("loot_table", table.getId())); }
            case 26 -> new LootTableEditorGUI(plugin, table).open(player);
            default -> { }
        }
    }
}