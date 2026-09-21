package com.districtx.pacificalootsystem.api;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class LootEntry implements LootItemReward {
    private UUID id = UUID.randomUUID();
    private String type = "ITEM";
    private ItemStack item;
    private int minimumAmount = 1;
    private int maximumAmount = 1;
    private boolean autoPickup;
    private double chance = 100.0;
    private double weight = 1.0;
    private String name;
    private List<String> lore = new ArrayList<>();
    private String command;
    private double minimumMoney;
    private double maximumMoney;
    private int minimumExperience;
    private int maximumExperience;
    private boolean experienceLevels;
    private boolean enabled = true;
    private List<String> conditions = new ArrayList<>();

    public LootEntry(ItemStack item) {
        this.item = item == null ? null : item.clone();
        if (item != null) { this.minimumAmount = item.getAmount(); this.maximumAmount = item.getAmount(); }
    }
    public LootEntry copy() {
        LootEntry copy = new LootEntry();
        copy.id = id; copy.type = type; copy.item = item == null ? null : item.clone();
        copy.minimumAmount = minimumAmount; copy.maximumAmount = maximumAmount; copy.autoPickup = autoPickup; copy.chance = chance; copy.weight = weight;
        copy.name = name; copy.lore = new ArrayList<>(lore == null ? List.of() : lore); copy.command = command;
        copy.minimumMoney = minimumMoney; copy.maximumMoney = maximumMoney; copy.minimumExperience = minimumExperience;
        copy.maximumExperience = maximumExperience; copy.experienceLevels = experienceLevels; copy.enabled = enabled;
        copy.conditions = new ArrayList<>(conditions == null ? List.of() : conditions);
        return copy;
    }
    public LootEntry duplicate() { LootEntry copy = copy(); copy.id = UUID.randomUUID(); return copy; }

    @Override
    public int generateAmount() {
        return minimumAmount + (maximumAmount <= minimumAmount ? 0 : ThreadLocalRandom.current().nextInt(maximumAmount - minimumAmount + 1));
    }
}