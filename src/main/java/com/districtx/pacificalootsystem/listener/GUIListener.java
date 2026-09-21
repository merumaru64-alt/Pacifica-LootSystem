package com.districtx.pacificalootsystem.listener;

import com.districtx.pacificalootsystem.gui.BaseGUI;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.event.inventory.InventoryMoveItemEvent;
import org.bukkit.event.inventory.InventoryPickupItemEvent;
import com.districtx.pacificalootsystem.gui.PhysicalLootGUI;

public final class GUIListener implements Listener {
    @EventHandler public void onClick(InventoryClickEvent event) {
        if (event.getView().getTopInventory().getHolder() instanceof PhysicalLootGUI) return;
        if (!(event.getView().getTopInventory().getHolder() instanceof BaseGUI gui)) return;
        event.setCancelled(true); if (event.getRawSlot() < event.getView().getTopInventory().getSize()) gui.handleClick(event);
    }
    @EventHandler public void onDrag(InventoryDragEvent event) {
        if (event.getView().getTopInventory().getHolder() instanceof PhysicalLootGUI) return;
        if (event.getView().getTopInventory().getHolder() instanceof BaseGUI) event.setCancelled(true);
    }
    @EventHandler public void onMove(InventoryMoveItemEvent event) { if (event.getDestination().getHolder() instanceof PhysicalLootGUI || event.getSource().getHolder() instanceof PhysicalLootGUI) event.setCancelled(true); }
    @EventHandler public void onPickup(InventoryPickupItemEvent event) { if (event.getInventory().getHolder() instanceof PhysicalLootGUI) event.setCancelled(true); }
}