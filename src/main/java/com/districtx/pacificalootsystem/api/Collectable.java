package com.districtx.pacificalootsystem.api;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class Collectable {
    private UUID id = UUID.randomUUID();
    private String name;
    private String displayName;
    private double chance = 100.0;
    private double weight = 1.0;
    private int minimumRewards;
    private int maximumRewards = 1;
    private String selectionMode = "RANDOM";
    private boolean enabled = true;
    private final Map<UUID, LootEntry> entries = new LinkedHashMap<>();

    public Collectable(String name) {
        this.name = name;
        this.displayName = name;
    }
}