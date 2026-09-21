package com.districtx.pacificalootsystem.api;

/** Controls linked-container hologram lifecycle. */
public interface LootHologramService {
    /** Starts periodic hologram maintenance. */
    void start();
    /** Stops maintenance and removes holograms. */
    void stop();
    /** Reconciles holograms with linked containers. */
    void refresh();
}