package com.districtx.pacificalootsystem.api;

public enum LootContainerType {
    CHEST,
    BARREL;

    public static LootContainerType from(String value) {
        if (value == null) return null;
        for (LootContainerType type : values()) if (type.name().equalsIgnoreCase(value)) return type;
        return null;
    }
}