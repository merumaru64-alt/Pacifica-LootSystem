package com.districtx.pacificalootsystem.gui;

import com.districtx.pacificalootsystem.PacificaLootSystem;
import com.districtx.pacificalootsystem.api.Collectable;
import com.districtx.pacificalootsystem.api.LootEntry;
import com.districtx.pacificalootsystem.api.LootTable;
import com.districtx.pacificalootsystem.util.ItemUtil;
import com.districtx.pacificalootsystem.util.MessageUtil;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;

import java.util.ArrayList;
import java.util.List;

public final class LootEntryEditorGUI extends BaseGUI {
    private final LootTable table;
    private final Collectable collectable;
    private final LootEntry entry;

    public LootEntryEditorGUI(PacificaLootSystem plugin, LootTable table, Collectable collectable, LootEntry entry) {
        super(plugin);
        this.table = table;
        this.collectable = collectable;
        this.entry = entry;
        render();
    }

    private void render() {
        inventory = org.bukkit.Bukkit.createInventory(this, 27, org.bukkit.ChatColor.DARK_PURPLE + "Loot Entry");
        inventory.setItem(11, ItemUtil.icon("WRITABLE_BOOK", "&bEdit Item", List.of("&7Amount, chance, weight, name, lore")));
        inventory.setItem(13, ItemUtil.icon("COPY", "&eDuplicate", List.of("&7Creates a new entry ID")));
        inventory.setItem(15, ItemUtil.icon("TNT", "&cDelete", List.of("&7Requires confirmation")));
        inventory.setItem(20, ItemUtil.icon(entry.isEnabled() ? "LIME_DYE" : "GRAY_DYE", entry.isEnabled() ? "&aEnabled" : "&cDisabled", List.of("&7Click to toggle")));
        inventory.setItem(21, ItemUtil.icon("IRON_INGOT", "&bChance", List.of("&7Current: " + entry.getChance() + "%")));
        inventory.setItem(22, ItemUtil.icon("ARROW", "&eBack", List.of()));
    }

    @Override protected void click(InventoryClickEvent event) {
        Player player = (Player) event.getWhoClicked();
        switch (event.getRawSlot()) {
            case 11 -> new ItemEditorGUI(plugin, table, collectable, entry, entry.getItem()).open(player);
            case 13 -> {
                LootEntry duplicate = entry.duplicate();
                collectable.getEntries().put(duplicate.getId(), duplicate);
                table.getEntries().put(duplicate.getId(), duplicate);
                plugin.getTableManager().save();
                MessageUtil.send(plugin, player, "loot-entry-duplicated");
                new CollectableEditorGUI(plugin, table, collectable).open(player);
            }
            case 15 -> new ConfirmationGUI(plugin, "&cDelete this loot entry?", () -> {
                collectable.getEntries().remove(entry.getId());
                table.getEntries().remove(entry.getId());
                plugin.getTableManager().save();
                MessageUtil.send(plugin, player, "loot-entry-deleted");
                new CollectableEditorGUI(plugin, table, collectable).open(player);
            }, () -> new LootEntryEditorGUI(plugin, table, collectable, entry).open(player)).open(player);
            case 20 -> {
                entry.setEnabled(!entry.isEnabled());
                plugin.getTableManager().save();
                render();
                player.openInventory(inventory);
            }
            case 21 -> plugin.requestInput(player, "Enter chance from 0 to 100", value -> {
                try {
                    double chance = Double.parseDouble(value);
                    if (chance < 0 || chance > 100) throw new NumberFormatException();
                    entry.setChance(chance);
                    plugin.getTableManager().save();
                    render();
                    player.openInventory(inventory);
                } catch (NumberFormatException exception) {
                    player.sendMessage("§cChance must be between 0 and 100.");
                }
            });
            case 22 -> new CollectableEditorGUI(plugin, table, collectable).open(player);
            default -> { }
        }
    }
}