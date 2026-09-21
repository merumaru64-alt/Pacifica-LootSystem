package com.districtx.pacificalootsystem.listener;

import com.districtx.pacificalootsystem.PacificaLootSystem;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerFishEvent;

public final class FishingListener implements Listener {
    private final PacificaLootSystem plugin;
    public FishingListener(PacificaLootSystem plugin) { this.plugin = plugin; }
    @EventHandler public void onFish(PlayerFishEvent event) {
        if (!plugin.getConfig().getBoolean("fishing.enabled", false) || event.getState() != PlayerFishEvent.State.CAUGHT_FISH) return;
        String table = plugin.getConfig().getString("fishing.loot-table"); if (table != null) plugin.getLootService().claim(event.getPlayer(), table, false, false);
    }
}