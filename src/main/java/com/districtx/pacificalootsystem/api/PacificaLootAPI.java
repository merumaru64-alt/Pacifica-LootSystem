package com.districtx.pacificalootsystem.api;

/** Stable integration entry point for Pacifica-LootSystem. */
public interface PacificaLootAPI {
    /** Returns the loot-table service. */
    LootTableService getLootTableService();
    /** Returns the linked-container service. */
    LinkedLootService getLinkedLootService();
    /** Returns the loot-generation service. */
    LootGenerationService getLootGenerationService();
    /** Returns the reward-delivery service. */
    LootRewardService getLootRewardService();
    /** Returns the physical-container service. */
    LootContainerService getLootContainerService();
    /** Returns the hologram service. */
    LootHologramService getLootHologramService();
    /** Returns the event dispatch service. */
    LootEventService getEventService();
    /** Returns the configured economy service. */
    EconomyService getEconomyService();
    /** Returns the XP reward and calculation service. */
    LootXpService getXpService();
    /** Returns the optional LuckPerms-backed primary-rank XP bonus service. */
    LootRankBonusService getRankBonusService();
    /** Returns the Vault-independent economy integration contract. */
    default LootEconomyService getLootEconomyService() { return getEconomyService(); }
}