package com.districtx.pacificalootsystem.api;

import org.bukkit.Location;

import java.util.List;
import java.util.Collection;
import java.util.Collections;
import java.util.Optional;
import java.util.UUID;

public interface LinkedLootService {
    LinkedLoot link(UUID lootTableId, Location location);
    void unlink(Location location);
    Optional<LinkedLoot> find(Location location);
    Optional<LinkedLoot> findById(UUID linkedLootId);
    List<LinkedLoot> getLinkedLoot(UUID lootTableId);
    List<LinkedLoot> findByLootTable(UUID lootTableId);
    boolean isLinked(Location location);
    void unlinkAll(UUID lootTableId);
    LootContainerState getState(UUID linkedLootId);
    void saveState(UUID linkedLootId, LootContainerState state);
    boolean isOnCooldown(UUID linkedLootId);
    long getCooldownRemaining(UUID linkedLootId);
    void startCooldown(UUID linkedLootId, long durationSeconds);
    void clearCooldown(UUID linkedLootId);

    default Optional<LinkedLoot> getById(UUID linkedLootId) { return findById(linkedLootId); }
    default Optional<LinkedLoot> getByLocation(Location location) { return find(location); }
    default Collection<LinkedLoot> getLinkedLoot() { return Collections.emptyList(); }
    default void link(Location location, String lootTableName) { throw new UnsupportedOperationException("Use link(UUID, Location)"); }
}