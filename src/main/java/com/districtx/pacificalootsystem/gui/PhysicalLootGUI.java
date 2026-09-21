package com.districtx.pacificalootsystem.gui;

import com.districtx.pacificalootsystem.PacificaLootSystem;
import com.districtx.pacificalootsystem.api.PhysicalLootSession;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;

public final class PhysicalLootGUI extends BaseGUI {
    private final com.districtx.pacificalootsystem.loot.PhysicalLootService service;
    private final PhysicalLootSession session;

    public PhysicalLootGUI(PacificaLootSystem plugin, com.districtx.pacificalootsystem.loot.PhysicalLootService service,
                           PhysicalLootSession session, String title) {
        super(plugin);
        this.service = service;
        this.session = session;
        this.inventory = Bukkit.createInventory(this, 27, title);
        session.getContents().forEach((slot, item) -> inventory.setItem(slot, item.clone()));
    }

    public PhysicalLootSession getSession() { return session; }

    @Override
    protected void click(InventoryClickEvent event) {
    }
}