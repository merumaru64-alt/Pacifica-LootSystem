package com.districtx.pacificalootsystem.condition;

import com.districtx.pacificalootsystem.api.LootCondition;
import com.districtx.pacificalootsystem.api.LootContext;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public final class ConditionRegistry {
    private static final Map<String, LootCondition> CONDITIONS = new ConcurrentHashMap<>();
    private ConditionRegistry() { }
    public static void register(LootCondition condition) { CONDITIONS.put(condition.getId().toLowerCase(), condition); }
    public static boolean test(String value, LootContext context) {
        if (value == null || value.isBlank()) return true;
        int separator = value.indexOf(':'); if (separator < 0) return true;
        String id = value.substring(0, separator).toLowerCase();
        String argument = value.substring(separator + 1);
        if ("permission".equals(id)) return context.getPlayer() != null && context.getPlayer().hasPermission(argument);
        if ("world".equals(id)) return context.getLocation() != null && context.getLocation().getWorld() != null && context.getLocation().getWorld().getName().equalsIgnoreCase(argument);
        if ("level".equals(id)) { try { return context.getPlayer() != null && context.getPlayer().getLevel() >= Integer.parseInt(argument); } catch (NumberFormatException ignored) { return false; } }
        LootCondition condition = CONDITIONS.get(id);
        return condition == null || condition.test(context);
    }
}