package com.districtx.pacificalootsystem.gui;

public enum GUIAction {
    OPEN("physical_loot.open"),
    TAKE("physical_loot.take"),
    TAKE_PARTIAL("physical_loot.take_partial"),
    SHIFT_TAKE("physical_loot.shift_take"),
    DRAG("physical_loot.drag"),
    CLOSE("physical_loot.close"),
    SESSION_COMPLETE("physical_loot.session_complete");

    private final String id;

    GUIAction(String id) {
        this.id = id;
    }

    public String id() {
        return id;
    }
}