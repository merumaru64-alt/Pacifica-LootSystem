package com.districtx.pacificalootsystem.gui;

import com.districtx.pacificalootsystem.PacificaLootSystem;
import com.districtx.pacificalootsystem.api.Collectable;
import com.districtx.pacificalootsystem.api.LootEntry;
import com.districtx.pacificalootsystem.api.LootTable;
import com.districtx.pacificalootsystem.util.ItemUtil;
import com.districtx.pacificalootsystem.util.MessageUtil;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public final class ItemEditorGUI extends BaseGUI {
    private final LootTable table;
    private final Collectable collectable;
    private final boolean creating;
    private final LootEntry entry;

    public ItemEditorGUI(PacificaLootSystem plugin, LootTable table, Collectable collectable, LootEntry source) {
        this(plugin, table, collectable, source, null);
    }

    public ItemEditorGUI(PacificaLootSystem plugin, LootTable table, Collectable collectable, LootEntry source, ItemStack initial) {
        super(plugin);
        this.table = table;
        this.collectable = collectable;
        creating = source == null;
        ItemStack item = initial == null ? ItemUtil.icon("STONE", "&fNew Loot Item", List.of()) : initial.clone();
        entry = source == null ? new LootEntry(item) : source.copy();
        render();
    }

    private void render() {
        inventory = org.bukkit.Bukkit.createInventory(this, 54, org.bukkit.ChatColor.DARK_PURPLE + "Item Editor");
        inventory.setItem(13, ItemUtil.preview(entry.getItem(), entry.getName() == null ? "&fPreview" : entry.getName(), entry.getLore()));
        inventory.setItem(45, ItemUtil.icon("ARROW", "&eBack", List.of()));
        inventory.setItem(46, ItemUtil.icon("PAPER", "&bAmount", List.of("&7Current: " + entry.getMinimumAmount() + "-" + entry.getMaximumAmount(), "&7Enter 3 or 2-6")));
        inventory.setItem(47, ItemUtil.icon("IRON_INGOT", "&bChance", List.of("&7Current: " + entry.getChance() + "%")));
        inventory.setItem(48, ItemUtil.icon("GOLD_INGOT", "&bWeight", List.of("&7Current: " + entry.getWeight())));
        inventory.setItem(49, ItemUtil.icon("NAME_TAG", "&bCustom Name", List.of("&7Current: " + (entry.getName() == null ? "none" : entry.getName()))));
        inventory.setItem(50, ItemUtil.icon("BOOK", "&bLore", List.of("&7Lines: " + entry.getLore().size(), "&7Separate lines with |")));
        inventory.setItem(51, ItemUtil.icon("HOPPER", "&bAuto Pickup", List.of("&7Status: " + (entry.isAutoPickup() ? "&aEnabled" : "&cDisabled"), "&7Click to toggle")));
        inventory.setItem(52, ItemUtil.icon("EMERALD", "&aSave Item", List.of()));
        inventory.setItem(53, ItemUtil.icon("BARRIER", "&cCancel", List.of()));
    }

    @Override protected void click(InventoryClickEvent event) {
        Player player = (Player) event.getWhoClicked();
        switch (event.getRawSlot()) {
            case 45, 53 -> new CollectableEditorGUI(plugin, table, collectable).open(player);
            case 46 -> plugin.requestInput(player, "Enter amount (fixed or min-max)", value -> {
                try {
                    String[] parts = value.split("-");
                    int min = Integer.parseInt(parts[0]);
                    int max = parts.length > 1 ? Integer.parseInt(parts[1]) : min;
                    if (min < 1 || max < min) throw new NumberFormatException();
                    entry.setMinimumAmount(min);
                    entry.setMaximumAmount(max);
                    render();
                    player.openInventory(inventory);
                } catch (NumberFormatException exception) {
                    player.sendMessage("§cUse a positive amount such as 2 or 2-6.");
                }
            });
            case 47 -> plugin.requestInput(player, "Enter chance from 0 to 100", value -> {
                try {
                    double chance = Double.parseDouble(value);
                    if (chance < 0 || chance > 100) throw new NumberFormatException();
                    entry.setChance(chance);
                    render();
                    player.openInventory(inventory);
                } catch (NumberFormatException exception) {
                    player.sendMessage("§cChance must be between 0 and 100.");
                }
            });
            case 48 -> plugin.requestInput(player, "Enter weight (zero disables weighted selection)", value -> {
                try {
                    double weight = Double.parseDouble(value);
                    if (weight < 0) throw new NumberFormatException();
                    entry.setWeight(weight);
                    render();
                    player.openInventory(inventory);
                } catch (NumberFormatException exception) {
                    player.sendMessage("§cWeight must be zero or greater.");
                }
            });
            case 49 -> plugin.requestInput(player, "Enter custom name, or - to clear", value -> {
                entry.setName(value.equals("-") ? null : value);
                render();
                player.openInventory(inventory);
            });
            case 50 -> plugin.requestInput(player, "Enter lore lines separated by |, or - to clear", value -> {
                entry.setLore(value.equals("-") ? new ArrayList<>() : new ArrayList<>(Arrays.asList(value.split("\\|", -1))));
                render();
                player.openInventory(inventory);
            });
            case 51 -> {
                entry.setAutoPickup(!entry.isAutoPickup());
                render();
                player.openInventory(inventory);
            }
            case 52 -> {
                if (entry.getItem() == null) {
                    player.sendMessage("§cThe item is missing.");
                    return;
                }
                collectable.getEntries().put(entry.getId(), entry);
                table.getEntries().put(entry.getId(), entry);
                plugin.getTableManager().save();
                MessageUtil.send(plugin, player, creating ? "loot-entry-added" : "loot-saved", java.util.Map.of("loot_table", table.getId()));
                new CollectableEditorGUI(plugin, table, collectable).open(player);
            }
            default -> { }
        }
    }
}