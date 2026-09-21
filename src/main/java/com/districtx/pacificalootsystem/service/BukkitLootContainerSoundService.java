package com.districtx.pacificalootsystem.service;

import com.cryptomorin.xseries.XSound;
import com.districtx.pacificalootsystem.api.LootContainerType;
import org.bukkit.entity.Player;

public final class BukkitLootContainerSoundService implements LootContainerSoundService {
    @Override
    public void playOpenSound(Player player, LootContainerType type) {
        sound(type, true).ifPresent(value -> value.play(player));
    }

    @Override
    public void playCloseSound(Player player, LootContainerType type) {
        sound(type, false).ifPresent(value -> value.play(player));
    }

    private java.util.Optional<XSound> sound(LootContainerType type, boolean open) {
        if (type == LootContainerType.CHEST) return XSound.matchXSound(open ? "BLOCK_CHEST_OPEN" : "BLOCK_CHEST_CLOSE");
        if (type == LootContainerType.BARREL) return XSound.matchXSound(open ? "BLOCK_BARREL_OPEN" : "BLOCK_BARREL_CLOSE");
        return java.util.Optional.empty();
    }
}