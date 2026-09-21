package com.districtx.pacificalootsystem.api;

/** Generates rewards from a loot-table context. */
public interface LootGenerationService {
    /** Generates a result using the table and context values. */
    LootResult generate(LootContext context);
}