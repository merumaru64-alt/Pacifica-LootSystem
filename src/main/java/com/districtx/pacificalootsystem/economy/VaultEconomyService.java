package com.districtx.pacificalootsystem.economy;

import com.districtx.pacificalootsystem.PacificaLootSystem;
import com.districtx.pacificalootsystem.api.EconomyService;
import net.milkbowl.vault.economy.Economy;
import net.milkbowl.vault.economy.EconomyResponse;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.plugin.RegisteredServiceProvider;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.concurrent.ThreadLocalRandom;

public final class VaultEconomyService implements EconomyService {
    private final PacificaLootSystem plugin;
    private final Economy economy;

    public VaultEconomyService(PacificaLootSystem plugin) {
        this.plugin = plugin;
        Economy found = null;
        if (Bukkit.getPluginManager().getPlugin("Vault") != null) {
            RegisteredServiceProvider<Economy> registration = Bukkit.getServicesManager().getRegistration(Economy.class);
            if (registration != null) found = registration.getProvider();
        }
        economy = found;
        if (economy == null) plugin.getLogger().warning("Economy provider not found. Money rewards have been disabled.");
        else plugin.getLogger().info("Vault economy provider detected: " + economy.getName());
    }

    @Override public boolean isAvailable() { return economy != null && economy.isEnabled(); }

    @Override
    public double generateAmount(double minimum, double maximum) {
        int decimals = Math.max(0, Math.min(6, plugin.getConfig().getInt("economy.decimals", 2)));
        BigDecimal low = BigDecimal.valueOf(Math.max(0, minimum)).setScale(decimals, RoundingMode.DOWN);
        BigDecimal high = BigDecimal.valueOf(Math.max(low.doubleValue(), maximum)).setScale(decimals, RoundingMode.DOWN);
        long lower = low.movePointRight(decimals).longValue();
        long upper = high.movePointRight(decimals).longValue();
        long value = upper <= lower ? lower : ThreadLocalRandom.current().nextLong(lower, upper + 1);
        return BigDecimal.valueOf(value, decimals).doubleValue();
    }

    @Override
    public EconomyResponse deposit(Player player, double amount) {
        if (!isAvailable()) return new EconomyResponse(0, 0, EconomyResponse.ResponseType.NOT_IMPLEMENTED, "No economy provider is available");
        return economy.depositPlayer(player, amount);
    }

    @Override public double getBalance(Player player) { return isAvailable() ? economy.getBalance(player) : 0; }
}