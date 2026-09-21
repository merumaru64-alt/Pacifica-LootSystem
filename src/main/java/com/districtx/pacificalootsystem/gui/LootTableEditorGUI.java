package com.districtx.pacificalootsystem.gui;

import com.districtx.pacificalootsystem.PacificaLootSystem;
import com.districtx.pacificalootsystem.api.Collectable;
import com.districtx.pacificalootsystem.api.LootTable;
import com.districtx.pacificalootsystem.util.ItemUtil;
import com.districtx.pacificalootsystem.util.MessageUtil;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.List;

public final class LootTableEditorGUI extends BaseGUI {
    private final LootTable table;
    private int page;
    private final List<Collectable> pageCollectables = new ArrayList<>();

    public LootTableEditorGUI(PacificaLootSystem plugin, LootTable table) {
        super(plugin);
        this.table = table;
        render();
    }

    private void render() {
        inventory = org.bukkit.Bukkit.createInventory(this, 54, org.bukkit.ChatColor.DARK_PURPLE + "Loot: " + table.getId());
        pageCollectables.clear();
        List<Collectable> collectables = new ArrayList<>(table.getCollectables().values());
        int from = page * 45;
        for (int i = from; i < Math.min(collectables.size(), from + 45); i++) {
            Collectable collectable = collectables.get(i);
            pageCollectables.add(collectable);
            inventory.setItem(i - from, icon(collectable));
        }
        inventory.setItem(45, ItemUtil.icon("ARROW", "&ePrevious", List.of("&7Page " + (page + 1))));
        inventory.setItem(49, ItemUtil.icon("COMPARATOR", "&bSettings", List.of("&7Mode: " + table.getSelectionMode(), "&7Rewards: " + table.getMinimumRewards() + "-" + table.getMaximumRewards())));
        inventory.setItem(50, ItemUtil.icon("EMERALD", "&aSave", List.of("&7Persist this loot table")));
        inventory.setItem(53, ItemUtil.icon("BARRIER", "&cClose", List.of()));
        if (from + 45 < collectables.size()) inventory.setItem(53, ItemUtil.icon("ARROW", "&eNext", List.of("&7Page " + (page + 2))));
    }

    private ItemStack icon(Collectable collectable) {
        String name = collectable.getDisplayName() == null ? collectable.getName() : collectable.getDisplayName();
        List<String> lore = plugin.getGui().getStringList("collectable.lore");
        if (lore.isEmpty()) lore = List.of("&7Collectable: &f%collectable-name%", "&7Rewards: &f%reward-count%", "&7Item Rewards: &f%minimum%- %maximum%", "&7Chance: &f%chance%%", "&7Weight: &f%weight%");
        List<String> values = new ArrayList<>();
        for (String line : lore) values.add(line.replace("%collectable-name%", collectable.getName()).replace("%reward-count%", String.valueOf(collectable.getEntries().size())).replace("%minimum%", String.valueOf(collectable.getMinimumRewards())).replace("%maximum%", String.valueOf(collectable.getMaximumRewards())).replace("%chance%", String.valueOf(collectable.getChance())).replace("%weight%", String.valueOf(collectable.getWeight())).replace("%enabled%", collectable.isEnabled() ? "Enabled" : "Disabled"));
        String icon = plugin.getGui().getString("collectable.icon", "ENDER_CHEST");
        String title = plugin.getGui().getString("collectable.name", "%display-name%").replace("%display-name%", name).replace("%collectable-name%", collectable.getName());
        return ItemUtil.icon(icon, title, values);
    }

    @Override protected void click(InventoryClickEvent event) {
        Player player = (Player) event.getWhoClicked();
        int slot = event.getRawSlot();
        if (slot < 45 && slot < pageCollectables.size()) {
            new CollectableEditorGUI(plugin, table, pageCollectables.get(slot)).open(player);
            return;
        }
        if (slot == 45 && page > 0) {
            page--;
            render();
            player.updateInventory();
            return;
        }
        if (slot == 49) {
            new SettingsGUI(plugin, table).open(player);
            return;
        }
        if (slot == 50) {
            plugin.getTableManager().save();
            MessageUtil.send(plugin, player, "loot-saved", java.util.Map.of("loot_table", table.getId()));
            return;
        }
        if (slot == 53) {
            if (page * 45 + 45 < table.getCollectables().size()) {
                page++;
                render();
                player.updateInventory();
            } else {
                player.closeInventory();
            }
        }
    }
}