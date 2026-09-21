package com.districtx.pacificalootsystem.api;

import lombok.Data;
import lombok.NoArgsConstructor;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.List;

@Data
@NoArgsConstructor
public class LootResult {
    private final List<ItemStack> items = new ArrayList<>();
    private final List<Boolean> itemAutoPickup = new ArrayList<>();
    private final List<String> commands = new ArrayList<>();
    private double money;
    private int experience;
    private int levels;
    private boolean successful;
    private String failureReason;

    public void addItem(ItemStack item, boolean autoPickup) {
        items.add(item);
        itemAutoPickup.add(autoPickup);
    }

    public boolean isAutoPickup(int index) {
        return index >= 0 && index < itemAutoPickup.size() && Boolean.TRUE.equals(itemAutoPickup.get(index));
    }

    public boolean hasRewards() { return !items.isEmpty() || !commands.isEmpty() || money > 0 || experience > 0 || levels > 0; }
}