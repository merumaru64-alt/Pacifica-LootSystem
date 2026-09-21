package com.districtx.pacificalootsystem.condition;

import com.districtx.pacificalootsystem.api.LootRewardType;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public final class RewardRegistry {
    private static final Map<String, LootRewardType> TYPES = new ConcurrentHashMap<>();
    private RewardRegistry() { }
    public static void register(LootRewardType type) { TYPES.put(type.getId().toLowerCase(), type); }
    public static LootRewardType get(String id) { return TYPES.get(id.toLowerCase()); }
}