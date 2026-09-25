package com.districtx.pacificalootsystem.api;

import lombok.Getter;
import lombok.Setter;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.block.Block;

import java.util.UUID;

@Getter
@Setter
public final class LinkedLoot {
    private UUID id;
    private UUID lootTableId;
    private UUID worldId;
    private int x;
    private int y;
    private int z;
    private String blockType;
    private long cooldownEndsAt;
    private boolean xpRewardClaimed;
    private long createdAt;
    private long updatedAt;

    public LinkedLoot(UUID id, UUID lootTableId, UUID worldId, int x, int y, int z, String blockType, long createdAt) {
        this(id, lootTableId, worldId, x, y, z, blockType, 0L, createdAt, createdAt);
    }

    public LinkedLoot(UUID id, UUID lootTableId, UUID worldId, int x, int y, int z, String blockType,
                      long cooldownEndsAt, long createdAt, long updatedAt) {
        this.id = id;
        this.lootTableId = lootTableId;
        this.worldId = worldId;
        this.x = x;
        this.y = y;
        this.z = z;
        this.blockType = blockType;
        this.cooldownEndsAt = cooldownEndsAt;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    public Location getLocation() {
        org.bukkit.World world = Bukkit.getWorld(worldId);
        return world == null ? null : new Location(world, x, y, z);
    }

    public Block getBlock() {
        Location location = getLocation();
        return location == null ? null : location.getBlock();
    }
}