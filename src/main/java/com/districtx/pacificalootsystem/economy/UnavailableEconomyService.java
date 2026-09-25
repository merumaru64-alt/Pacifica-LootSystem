package com.districtx.pacificalootsystem.economy;

import com.districtx.pacificalootsystem.PacificaLootSystem;
import com.districtx.pacificalootsystem.api.EconomyService;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Locale;
import java.util.concurrent.ThreadLocalRandom;

public final class UnavailableEconomyService implements EconomyService {
    private final PacificaLootSystem plugin;

    public UnavailableEconomyService(PacificaLootSystem plugin) {
        this.plugin = plugin;
        plugin.getLogger().warning("Vault was not detected.");
        plugin.getLogger().warning("Money rewards are unavailable.");
    }

    @Override public boolean isAvailable() { return false; }
    @Override public boolean hasEconomyProvider() { return false; }
    @Override
    public double generateAmount(double minimum, double maximum) {
        double safeMinimum = Double.isFinite(minimum) ? Math.max(0, minimum) : 0;
        double safeMaximum = Double.isFinite(maximum) ? Math.max(safeMinimum, maximum) : safeMinimum;
        int decimals = Math.max(0, Math.min(6, plugin.getConfig().getInt("economy.decimals", 2)));
        BigDecimal low = BigDecimal.valueOf(safeMinimum).setScale(decimals, RoundingMode.DOWN);
        BigDecimal high = BigDecimal.valueOf(safeMaximum).setScale(decimals, RoundingMode.DOWN);
        try {
            long lower = low.movePointRight(decimals).longValueExact();
            long upper = high.movePointRight(decimals).longValueExact();
            if (upper <= lower) return low.doubleValue();
            if (upper < Long.MAX_VALUE) return BigDecimal.valueOf(ThreadLocalRandom.current().nextLong(lower, upper + 1), decimals).doubleValue();
        } catch (ArithmeticException ignored) {
            if (high.compareTo(low) <= 0) return low.doubleValue();
        }
        BigDecimal offset = high.subtract(low).multiply(BigDecimal.valueOf(ThreadLocalRandom.current().nextDouble()));
        return low.add(offset).setScale(decimals, RoundingMode.DOWN).doubleValue();
    }
    @Override public boolean deposit(Player player, double amount) { return false; }
    @Override public boolean deposit(OfflinePlayer player, double amount) { return false; }
    @Override public double getBalance(Player player) { return 0; }
    @Override public String getProviderName() { return "unavailable"; }
    @Override public void refresh() { }

    @Override
    public String format(double amount) {
        int decimals = Math.max(0, Math.min(6, plugin.getConfig().getInt("economy.decimals", 2)));
        return Double.isFinite(amount) ? String.format(Locale.US, "%,." + decimals + "f", amount) : "0";
    }
}