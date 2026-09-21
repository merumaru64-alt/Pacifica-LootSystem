package com.districtx.pacificalootsystem.gui;

import com.districtx.pacificalootsystem.PacificaLootSystem;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;

public abstract class BaseGUI implements InventoryHolder {
    protected final PacificaLootSystem plugin;
    protected Inventory inventory;
    protected BaseGUI(PacificaLootSystem plugin) { this.plugin = plugin; }
    protected abstract void click(InventoryClickEvent event);
    public void open(Player player) { player.openInventory(inventory); }
    @Override public Inventory getInventory() { return inventory; }
    public final void handleClick(InventoryClickEvent event) { click(event); }
}