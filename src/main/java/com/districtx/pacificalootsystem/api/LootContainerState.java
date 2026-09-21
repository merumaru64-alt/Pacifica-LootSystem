package com.districtx.pacificalootsystem.api;

import lombok.Getter;
import lombok.Setter;
import org.bukkit.inventory.ItemStack;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

@Getter
@Setter
public final class LootContainerState {
    private UUID linkedLootId;
    private Map<Integer, ItemStack> contents;
    private long updatedAt;

    public LootContainerState(UUID linkedLootId) {
        this(linkedLootId, new LinkedHashMap<>(), System.currentTimeMillis());
    }

    public LootContainerState(UUID linkedLootId, Map<Integer, ItemStack> contents, long updatedAt) {
        this.linkedLootId = linkedLootId;
        this.contents = new LinkedHashMap<>();
        contents.forEach((slot, item) -> {
            if (slot != null && slot >= 0 && slot < 27 && item != null && !item.getType().isAir()) this.contents.put(slot, item.clone());
        });
        this.updatedAt = updatedAt;
    }

    public boolean isEmpty() {
        return contents.isEmpty();
    }
}