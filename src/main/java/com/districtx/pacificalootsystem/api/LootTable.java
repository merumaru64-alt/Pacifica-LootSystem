package com.districtx.pacificalootsystem.api;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class LootTable {
    private UUID uuid = UUID.randomUUID();
    private String id;
    private String displayName;
    private String description = "";
    private boolean enabled = true;
    private String selectionMode = "RANDOM";
    private int minimumRewards = 1;
    private int maximumRewards = 1;
    private long cooldownSeconds;
    private boolean globalMode;
    private String lootMode = "PHYSICAL";
    private final Map<UUID, MoneyReward> moneyRewards = new LinkedHashMap<>();
    private double cost;
    private String permission;
    private List<String> conditions = new ArrayList<>();
    private final Map<UUID, Collectable> collectables = new LinkedHashMap<>();
    private final Map<java.util.UUID, LootEntry> entries = new LinkedHashMap<>();
    public LootTable(String id, String displayName) { this.id = id; this.displayName = displayName; }
}