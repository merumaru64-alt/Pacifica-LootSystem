package com.districtx.pacificalootsystem.api;

import lombok.Getter;
import lombok.Setter;
import org.bukkit.inventory.ItemStack;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

@Getter
@Setter
public final class PhysicalLootSession {
    private UUID sessionId;
    private UUID playerId;
    private UUID linkedLootId;
    private UUID lootTableId;
    private Map<Integer, ItemStack> contents;
    private boolean generatedNewLoot;
    private long openedAt;

    public PhysicalLootSession(UUID sessionId, UUID playerId, UUID linkedLootId, UUID lootTableId,
                               Map<Integer, ItemStack> contents, boolean generatedNewLoot, long openedAt) {
        this.sessionId = sessionId;
        this.playerId = playerId;
        this.linkedLootId = linkedLootId;
        this.lootTableId = lootTableId;
        this.contents = new LinkedHashMap<>();
        contents.forEach((slot, item) -> {
            if (item != null && !item.getType().isAir()) this.contents.put(slot, item.clone());
        });
        this.generatedNewLoot = generatedNewLoot;
        this.openedAt = openedAt;
    }

    public boolean hasRemainingRewards() {
        return !contents.isEmpty();
    }
}