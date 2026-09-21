package com.districtx.pacificalootsystem.service;

import com.districtx.pacificalootsystem.api.LootEventService;
import org.bukkit.Bukkit;
import org.bukkit.event.Event;

/** Bukkit-backed implementation of the public loot event service. */
public final class BukkitLootEventService implements LootEventService {
    @Override
    public void call(Event event) {
        Bukkit.getPluginManager().callEvent(event);
    }
}