package com.districtx.pacificalootsystem.service;

import com.districtx.pacificalootsystem.api.LootContainerType;
import org.bukkit.entity.Player;

public interface LootContainerSoundService {
    void playOpenSound(Player player, LootContainerType type);
    void playCloseSound(Player player, LootContainerType type);
}