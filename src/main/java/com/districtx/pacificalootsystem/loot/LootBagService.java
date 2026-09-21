package com.districtx.pacificalootsystem.loot;

import com.districtx.pacificalootsystem.PacificaLootSystem;
import com.districtx.pacificalootsystem.api.LootContext;
import com.districtx.pacificalootsystem.api.LootResult;
import com.districtx.pacificalootsystem.api.LootTable;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataType;

public final class LootBagService {
    private final PacificaLootSystem plugin;
    private final NamespacedKey tableKey;

    public LootBagService(PacificaLootSystem plugin) {
        this.plugin = plugin;
        this.tableKey = new NamespacedKey(plugin, "loot_table");
    }

    public boolean isLootBag(ItemStack item) {
        return item != null && item.hasItemMeta()
            && item.getItemMeta().getPersistentDataContainer().has(tableKey, PersistentDataType.STRING);
    }

    public boolean use(Player player, ItemStack lootBag) {
        if (!plugin.getConfig().getBoolean("loot-bag.enabled", true) || !isLootBag(lootBag)
            || !player.hasPermission("pacifica.lootsystem.lootbag")) return false;
        String tableId = lootBag.getItemMeta().getPersistentDataContainer().get(tableKey, PersistentDataType.STRING);
        LootTable table = plugin.getTableManager().get(tableId);
        if (table == null || !table.isEnabled()) return false;
        LootResult result = plugin.getLootGenerator().generate(new LootContext(player, null, player.getLocation(),
            player.getInventory().getItemInMainHand(), table, "LOOT_BAG"));
        if (!result.hasRewards()) return false;
        plugin.getLootService().deliver(player, result, table);
        com.districtx.pacificalootsystem.util.MessageUtil.send(plugin, player, "loot-bag.used");
        return true;
    }
}