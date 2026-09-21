package com.districtx.pacificalootsystem.listener;

import com.districtx.pacificalootsystem.PacificaLootSystem;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.EquipmentSlot;

public final class LootBagListener implements Listener {
    private final PacificaLootSystem plugin;
    public LootBagListener(PacificaLootSystem plugin) { this.plugin = plugin; }
    @EventHandler public void onInteract(PlayerInteractEvent event) {
        if (event.getAction() != Action.RIGHT_CLICK_AIR && event.getAction() != Action.RIGHT_CLICK_BLOCK) return;
        ItemStack item = event.getItem(); if (!plugin.getLootBagService().isLootBag(item)) return;
        event.setCancelled(true); if (!plugin.getLootBagService().use(event.getPlayer(), item)) return;
        if (item.getAmount() <= 1) { if (event.getHand() == EquipmentSlot.OFF_HAND) event.getPlayer().getInventory().setItemInOffHand(null); else event.getPlayer().getInventory().setItemInMainHand(null); } else item.setAmount(item.getAmount() - 1);
    }
}