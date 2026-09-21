package com.districtx.pacificalootsystem.api;

import org.bukkit.event.Event;

/** Dispatches public loot events through Bukkit's event bus. */
public interface LootEventService {
    /** Fires an event on the server event bus. */
    void call(Event event);
}