package com.districtx.pacificalootsystem.gui;

import com.districtx.pacificalootsystem.PacificaLootSystem;
import com.districtx.pacificalootsystem.api.Collectable;
import com.districtx.pacificalootsystem.api.LootEntry;
import com.districtx.pacificalootsystem.api.LootTable;
import com.districtx.pacificalootsystem.util.ItemUtil;
import com.districtx.pacificalootsystem.util.MessageUtil;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;

import java.util.List;

public final class CollectableSettingsGUI extends BaseGUI {
    private final LootTable table;
    private final Collectable collectable;

    public CollectableSettingsGUI(PacificaLootSystem plugin, LootTable table, Collectable collectable) {
        super(plugin);
        this.table = table;
        this.collectable = collectable;
        render();
    }

    private void render() {
        inventory = org.bukkit.Bukkit.createInventory(this, 27, org.bukkit.ChatColor.DARK_PURPLE + "Collectable Settings");
        inventory.setItem(10, ItemUtil.icon("NAME_TAG", "&bDisplay Name", List.of("&7Current: " + collectable.getDisplayName(), "&7Click to enter a name")));
        inventory.setItem(11, ItemUtil.icon("IRON_INGOT", "&bChance", List.of("&7Current: " + collectable.getChance() + "%", "&70 to 100")));
        inventory.setItem(12, ItemUtil.icon("GOLD_INGOT", "&bWeight", List.of("&7Current: " + collectable.getWeight(), "&7Zero disables weighted selection")));
        inventory.setItem(13, ItemUtil.icon("PAPER", "&bReward Amount", List.of("&7Current: " + collectable.getMinimumRewards() + "-" + collectable.getMaximumRewards(), "&7Enter min-max")));
        inventory.setItem(14, ItemUtil.icon("COMPARATOR", "&bSelection Mode", List.of("&7Current: " + collectable.getSelectionMode(), "&7Click to cycle")));
        inventory.setItem(15, ItemUtil.icon(collectable.isEnabled() ? "LIME_DYE" : "GRAY_DYE", collectable.isEnabled() ? "&aEnabled" : "&cDisabled", List.of("&7Click to toggle")));
        inventory.setItem(16, ItemUtil.icon("COPY", "&eDuplicate", List.of("&7Deep-clone this collectable")));
        inventory.setItem(17, ItemUtil.icon("TNT", "&cDelete", List.of("&7Requires confirmation")));
        inventory.setItem(22, ItemUtil.icon("EMERALD", "&aSave", List.of()));
        inventory.setItem(26, ItemUtil.icon("ARROW", "&eBack", List.of()));
    }

    @Override protected void click(InventoryClickEvent event) {
        Player player = (Player) event.getWhoClicked();
        switch (event.getRawSlot()) {
            case 10 -> plugin.requestInput(player, "Enter display name", value -> { collectable.setDisplayName(value); render(); player.openInventory(inventory); });
            case 11 -> plugin.requestInput(player, "Enter chance from 0 to 100", value -> {
                try {
                    double chance = Double.parseDouble(value);
                    if (chance < 0 || chance > 100) throw new NumberFormatException();
                    collectable.setChance(chance);
                    render();
                    player.openInventory(inventory);
                } catch (NumberFormatException exception) {
                    player.sendMessage("§cChance must be between 0 and 100.");
                }
            });
            case 12 -> plugin.requestInput(player, "Enter weight zero or greater", value -> {
                try {
                    double weight = Double.parseDouble(value);
                    if (weight < 0) throw new NumberFormatException();
                    collectable.setWeight(weight);
                    render();
                    player.openInventory(inventory);
                } catch (NumberFormatException exception) {
                    player.sendMessage("§cWeight must be zero or greater.");
                }
            });
            case 13 -> plugin.requestInput(player, "Enter reward amount min-max", value -> {
                try {
                    String[] parts = value.split("-");
                    int minimum = Integer.parseInt(parts[0]);
                    int maximum = parts.length > 1 ? Integer.parseInt(parts[1]) : minimum;
                    if (minimum < 0 || maximum < minimum) throw new NumberFormatException();
                    collectable.setMinimumRewards(minimum);
                    collectable.setMaximumRewards(maximum);
                    render();
                    player.openInventory(inventory);
                } catch (NumberFormatException exception) {
                    player.sendMessage("§cUse a non-negative amount such as 0 or 0-2.");
                }
            });
            case 14 -> {
                String[] modes = {"RANDOM", "WEIGHTED", "UNIQUE", "ALL"};
                int current = 0;
                for (int i = 0; i < modes.length; i++) if (modes[i].equalsIgnoreCase(collectable.getSelectionMode())) current = i;
                collectable.setSelectionMode(modes[(current + 1) % modes.length]);
                render();
                player.openInventory(inventory);
            }
            case 15 -> { collectable.setEnabled(!collectable.isEnabled()); render(); player.openInventory(inventory); }
            case 16 -> duplicate(player);
            case 17 -> delete(player);
            case 22 -> {
                plugin.getTableManager().save();
                MessageUtil.send(plugin, player, "loot-saved", java.util.Map.of("loot_table", table.getId()));
                new CollectableEditorGUI(plugin, table, collectable).open(player);
            }
            case 26 -> new CollectableEditorGUI(plugin, table, collectable).open(player);
            default -> { }
        }
    }

    private void duplicate(Player player) {
        String name = collectable.getName() + " Copy";
        int suffix = 2;
        while (find(name) != null) name = collectable.getName() + " Copy " + suffix++;
        Collectable duplicate = new Collectable(name);
        duplicate.setDisplayName(collectable.getDisplayName() + " Copy");
        duplicate.setChance(collectable.getChance());
        duplicate.setWeight(collectable.getWeight());
        duplicate.setMinimumRewards(collectable.getMinimumRewards());
        duplicate.setMaximumRewards(collectable.getMaximumRewards());
        duplicate.setSelectionMode(collectable.getSelectionMode());
        duplicate.setEnabled(collectable.isEnabled());
        for (LootEntry entry : collectable.getEntries().values()) {
            LootEntry copy = entry.duplicate();
            duplicate.getEntries().put(copy.getId(), copy);
            table.getEntries().put(copy.getId(), copy);
        }
        table.getCollectables().put(duplicate.getId(), duplicate);
        plugin.getTableManager().save();
        MessageUtil.send(plugin, player, "collectable-duplicated");
        new LootTableEditorGUI(plugin, table).open(player);
    }

    private void delete(Player player) {
        new ConfirmationGUI(plugin, "&cDelete this collectable and all rewards?", () -> {
            table.getCollectables().remove(collectable.getId());
            for (LootEntry entry : collectable.getEntries().values()) table.getEntries().remove(entry.getId());
            plugin.getTableManager().save();
            MessageUtil.send(plugin, player, "collectable-deleted");
            new LootTableEditorGUI(plugin, table).open(player);
        }, () -> new CollectableSettingsGUI(plugin, table, collectable).open(player)).open(player);
    }

    private Collectable find(String name) {
        return table.getCollectables().values().stream().filter(value -> value.getName() != null && value.getName().equalsIgnoreCase(name)).findFirst().orElse(null);
    }
}