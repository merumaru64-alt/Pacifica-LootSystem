package com.districtx.pacificalootsystem.api;

public interface LootTrigger {
    String getId();
    void trigger(LootContext context);
}