package com.districtx.pacificalootsystem.api;

public interface LootReward {
    String getType();
    void give(LootContext context);
}