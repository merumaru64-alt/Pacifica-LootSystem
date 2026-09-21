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
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.List;

public final class CollectableEditorGUI extends BaseGUI {
    private final LootTable table;
    private final Collectable collectable;
    private int page;
    private final List<LootEntry> pageEntries = new ArrayList<>();

    public CollectableEditorGUI(PacificaLootSystem plugin, LootTable table, Collectable collectable) {
        super(plugin);
        this.table = table;
        this.collectable = collectable;
        render();
    }

    private void render() {
        inventory = org.bukkit.Bukkit.createInventory(this, 54, org.bukkit.ChatColor.DARK_PURPLE + "Collectable: " + collectable.getName());
        pageEntries.clear();
        List<LootEntry> entries = new ArrayList<>(collectable.getEntries().values());
        int from = page * 36;
        for (int i = from; i < Math.min(entries.size(), from + 36); i++) {
            LootEntry entry = entries.get(i);
            pageEntries.add(entry);
            inventory.setItem(9 + i - from, icon(entry));
        }
        inventory.setItem(45, ItemUtil.icon("ARROW", "&ePrevious", List.of("&7Page " + (page + 1))));
        inventory.setItem(46, ItemUtil.icon("PAPER", "&bInformation", List.of(
                "&b&l" + collectable.getName(),
                "&7Loot Table: &f" + table.getDisplayName(),
                "&7Rewards: &f" + collectable.getEntries().size(),
                "&7Reward Amount: &f" + collectable.getMinimumRewards() + "-" + collectable.getMaximumRewards(),
                "&7Chance: &f" + collectable.getChance() + "%",
                "&7Weight: &f" + collectable.getWeight(),
                "&7Cooldown:",
                "&fInherited from Loot Table"
        )));
        inventory.setItem(47, ItemUtil.icon("HOPPER", "&aAdd Item", List.of("&7Open the item editor")));
        inventory.setItem(48, ItemUtil.icon("GOLD_INGOT", "&aAdd Item From Hand", List.of("&7Uses your main-hand item")));
        inventory.setItem(49, ItemUtil.icon("COMPARATOR", "&bCollectable Settings", List.of("&7Configure this collectable")));
        inventory.setItem(50, ItemUtil.icon("ENDER_EYE", "&dTest Collectable", List.of("&7Test this collectable")));
        inventory.setItem(51, ItemUtil.icon("EMERALD", "&aSave", List.of("&7Persist this collectable")));
        inventory.setItem(52, ItemUtil.icon("ARROW", "&eNext", List.of("&7Page " + (page + 2))));
        inventory.setItem(53, ItemUtil.icon("BARRIER", "&cBack", List.of()));
        if (from + 36 >= entries.size()) inventory.setItem(52, ItemUtil.icon("GRAY_DYE", "&7Next", List.of("&7No more pages")));
    }

    private ItemStack icon(LootEntry entry) {
        ItemStack item = entry.getItem() == null ? ItemUtil.icon("COMMAND_BLOCK", "&e" + entry.getType(), List.of()) : entry.getItem().clone();
        ItemMeta meta = item.getItemMeta();
        List<String> lore = new ArrayList<>();
        if (meta.hasLore()) lore.addAll(meta.getLore());
        lore.add("&7Amount: " + entry.getMinimumAmount() + "-" + entry.getMaximumAmount());
        lore.add("&7Chance: " + entry.getChance() + "%");
        lore.add("&7Weight: " + entry.getWeight());
        lore.add("&7Entry ID: " + entry.getId());
        lore.add(entry.isEnabled() ? "&aEnabled" : "&cDisabled");
        meta.setLore(com.districtx.pacificalootsystem.util.TextUtil.color(lore));
        item.setItemMeta(meta);
        return item;
    }

    @Override protected void click(InventoryClickEvent event) {
        Player player = (Player) event.getWhoClicked();
        int slot = event.getRawSlot();
        if (slot >= 9 && slot < 45 && slot - 9 < pageEntries.size()) {
            new LootEntryEditorGUI(plugin, table, collectable, pageEntries.get(slot - 9)).open(player);
            return;
        }
        if (slot == 45 && page > 0) {
            page--;
            render();
            player.updateInventory();
            return;
        }
        if (slot == 47) {
            new ItemEditorGUI(plugin, table, collectable, null, player.getInventory().getItemInMainHand()).open(player);
            return;
        }
        if (slot == 48) {
            ItemStack hand = player.getInventory().getItemInMainHand();
            if (hand.getType().isAir()) {
                player.sendMessage("§cYou must hold an item in your main hand.");
                return;
            }
            LootEntry entry = new LootEntry(hand);
            collectable.getEntries().put(entry.getId(), entry);
            table.getEntries().put(entry.getId(), entry);
            plugin.getTableManager().save();
            MessageUtil.send(plugin, player, "loot-entry-added", java.util.Map.of("loot_table", table.getId()));
            render();
            player.openInventory(inventory);
            return;
        }
        if (slot == 49) {
            new CollectableSettingsGUI(plugin, table, collectable).open(player);
            return;
        }
        if (slot == 50) {
            player.sendMessage("§eCollectable testing is not available in this editor yet.");
            return;
        }
        if (slot == 51) {
            plugin.getTableManager().save();
            MessageUtil.send(plugin, player, "loot-saved", java.util.Map.of("loot_table", table.getId()));
            return;
        }
        if (slot == 52 && page * 36 + 36 < collectable.getEntries().size()) {
            page++;
            render();
            player.updateInventory();
            return;
        }
        if (slot == 53) new LootTableEditorGUI(plugin, table).open(player);
    }
}