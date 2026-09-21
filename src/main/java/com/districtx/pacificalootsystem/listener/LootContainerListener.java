package com.districtx.pacificalootsystem.listener;

import com.districtx.pacificalootsystem.PacificaLootSystem;
import com.districtx.pacificalootsystem.api.LinkedLoot;
import com.districtx.pacificalootsystem.api.LootContainerType;
import org.bukkit.event.block.Action;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockExplodeEvent;
import org.bukkit.event.entity.EntityExplodeEvent;

public final class LootContainerListener implements Listener {
    private final PacificaLootSystem plugin;
    public LootContainerListener(PacificaLootSystem plugin) { this.plugin = plugin; }
    @EventHandler public void onInteract(PlayerInteractEvent event) {
        if (!plugin.getConfig().getBoolean("linked-loot.enabled", true)) return;
        plugin.getLinkManager().handleInteract(event);
        if (event.isCancelled() || event.getAction() != Action.RIGHT_CLICK_BLOCK || event.getClickedBlock() == null) return;
        LinkedLoot linked = plugin.getLinkManager().find(event.getClickedBlock().getLocation()).orElse(null);
        LootContainerType liveType = LootContainerType.from(event.getClickedBlock().getType().name());
        if (linked == null || liveType == null || liveType != LootContainerType.from(linked.getBlockType())) return;
        event.setCancelled(true);
        plugin.getPhysicalLootService().open(event.getPlayer(), linked);
    }
    @EventHandler public void onBreak(BlockBreakEvent event) {
        if (!plugin.getLinkManager().isLinked(event.getBlock().getLocation())) return;
        boolean protectedBlock = plugin.getConfig().getBoolean("linked-loot.protect-blocks", true);
        if (protectedBlock && !event.getPlayer().hasPermission("pacifica.lootsystem.admin.bypass")) event.setCancelled(true);
        else plugin.getLinkManager().unlink(event.getBlock().getLocation());
    }
    @EventHandler public void onBlockExplode(BlockExplodeEvent event) {
        boolean protectedBlocks = plugin.getConfig().getBoolean("linked-loot.protect-from-explosions", true);
        event.blockList().removeIf(block -> {
            if (!plugin.getLinkManager().isLinked(block.getLocation())) return false;
            if (protectedBlocks) return true;
            plugin.getLinkManager().unlink(block.getLocation());
            return false;
        });
    }
    @EventHandler public void onEntityExplode(EntityExplodeEvent event) {
        boolean protectedBlocks = plugin.getConfig().getBoolean("linked-loot.protect-from-explosions", true);
        event.blockList().removeIf(block -> {
            if (!plugin.getLinkManager().isLinked(block.getLocation())) return false;
            if (protectedBlocks) return true;
            plugin.getLinkManager().unlink(block.getLocation());
            return false;
        });
    }
}