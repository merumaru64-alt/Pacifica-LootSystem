package com.districtx.pacificalootsystem.api;

public interface LootRewardType {
    String getId();
    LootReward create(LootEntry entry);
}