package com.districtx.pacificalootsystem.api;

import org.bukkit.Bukkit;
import org.bukkit.plugin.RegisteredServiceProvider;

/** Locates the registered Pacifica-LootSystem integration API. */
public final class PacificaLootProvider {
    private PacificaLootProvider() { }

    /** Returns the API when Pacifica-LootSystem is enabled, or null otherwise. */
    public static PacificaLootAPI getAPI() {
        RegisteredServiceProvider<PacificaLootAPI> registration = Bukkit.getServicesManager().getRegistration(PacificaLootAPI.class);
        return registration == null ? null : registration.getProvider();
    }

    /** Returns whether the integration API is currently available. */
    public static boolean isAvailable() {
        return getAPI() != null;
    }
}