package com.districtx.pacificalootsystem.condition;

import com.districtx.pacificalootsystem.api.LootTrigger;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public final class TriggerRegistry {
    private static final Map<String, LootTrigger> TRIGGERS = new ConcurrentHashMap<>();
    private TriggerRegistry() { }
    public static void register(LootTrigger trigger) { TRIGGERS.put(trigger.getId().toLowerCase(), trigger); }
    public static LootTrigger get(String id) { return TRIGGERS.get(id.toLowerCase()); }
}