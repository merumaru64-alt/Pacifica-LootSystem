package com.districtx.pacificalootsystem.api;

import net.milkbowl.vault.economy.EconomyResponse;
import org.bukkit.entity.Player;

public interface EconomyService {
    boolean isAvailable();
    double generateAmount(double minimum, double maximum);
    EconomyResponse deposit(Player player, double amount);
    double getBalance(Player player);
}