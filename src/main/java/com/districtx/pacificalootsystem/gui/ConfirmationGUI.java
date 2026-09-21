package com.districtx.pacificalootsystem.gui;

import com.districtx.pacificalootsystem.PacificaLootSystem;
import com.districtx.pacificalootsystem.util.ItemUtil;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;

import java.util.List;

public final class ConfirmationGUI extends BaseGUI {
    private final Runnable confirm;
    private final Runnable cancel;
    public ConfirmationGUI(PacificaLootSystem plugin, String message, Runnable confirm, Runnable cancel) {
        super(plugin); this.confirm = confirm; this.cancel = cancel; inventory = org.bukkit.Bukkit.createInventory(this, 27, "Confirm");
        inventory.setItem(11, ItemUtil.icon("LIME_WOOL", "&aCONFIRM", List.of(message))); inventory.setItem(15, ItemUtil.icon("RED_WOOL", "&cCANCEL", List.of()));
    }
    @Override protected void click(InventoryClickEvent event) { Player player = (Player) event.getWhoClicked(); if (event.getRawSlot() == 11) confirm.run(); else if (event.getRawSlot() == 15) cancel.run(); }
}