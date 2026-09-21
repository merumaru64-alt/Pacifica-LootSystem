package com.districtx.pacificalootsystem.api;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.bukkit.Location;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class LootContext {
    private Player player;
    private Entity entity;
    private Location location;
    private ItemStack tool;
    private LootTable table;
    private String trigger;
}