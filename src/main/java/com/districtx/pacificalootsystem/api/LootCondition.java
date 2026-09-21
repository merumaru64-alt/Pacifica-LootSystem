package com.districtx.pacificalootsystem.api;

public interface LootCondition {
    String getId();
    boolean test(LootContext context);
}