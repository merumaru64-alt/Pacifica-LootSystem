package com.districtx.pacificalootsystem.listener;

import com.districtx.pacificalootsystem.PacificaLootSystem;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDeathEvent;

public final class MobLootListener implements Listener {
    private final PacificaLootSystem plugin;
    public MobLootListener(PacificaLootSystem plugin) { this.plugin = plugin; }
    @EventHandler public void onDeath(EntityDeathEvent event) {
        LivingEntity entity = event.getEntity(); Player killer = entity.getKiller(); String path = "mobs." + entity.getType().name(); String table = plugin.getConfig().getString(path);
        if (table == null || killer == null || plugin.getTableManager().get(table) == null || !plugin.getTableManager().get(table).isEnabled()) return;
        String mode = plugin.getConfig().getString(path + ".vanilla", "ADD_TO_VANILLA"); if ("REPLACE_VANILLA".equalsIgnoreCase(mode)) event.getDrops().clear(); plugin.getLootService().claim(killer, table, true, false);
    }
}