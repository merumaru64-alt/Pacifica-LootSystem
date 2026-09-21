package com.districtx.pacificalootsystem.api;

import java.util.Collection;
import java.util.Optional;

/** Read and persistence operations for reusable loot tables. */
public interface LootTableService {
    /** Finds a table by its case-insensitive identifier. */
    Optional<LootTable> getLootTable(String name);
    /** Tests whether a table exists. */
    boolean exists(String name);
    /** Returns a read-only view of all tables. */
    Collection<LootTable> getLootTables();
    /** Creates and persists a table when the identifier is unused. */
    void createLootTable(String name);
    /** Deletes and persists a table. */
    boolean deleteLootTable(String name);
    /** Persists the current table collection. */
    void save(LootTable lootTable);
}