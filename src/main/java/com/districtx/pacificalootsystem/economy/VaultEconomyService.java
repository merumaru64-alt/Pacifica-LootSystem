package com.districtx.pacificalootsystem.economy;

import com.districtx.pacificalootsystem.PacificaLootSystem;
import com.districtx.pacificalootsystem.api.EconomyService;
import net.milkbowl.vault.economy.Economy;
import net.milkbowl.vault.economy.EconomyResponse;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.RegisteredServiceProvider;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Locale;
import java.util.Objects;
import java.util.concurrent.ThreadLocalRandom;

public final class VaultEconomyService implements EconomyService {
    private final PacificaLootSystem plugin;
    private volatile Economy economy;
    private boolean initialized;
    private boolean vaultDetected;
    private String lastProviderName;

    public VaultEconomyService(PacificaLootSystem plugin) {
        this.plugin = plugin;
        refresh();
    }

    @Override
    public synchronized void refresh() {
        Plugin vault = Bukkit.getPluginManager().getPlugin("Vault");
        boolean detected = vault != null && vault.isEnabled();
        Economy provider = null;
        if (detected) {
            RegisteredServiceProvider<Economy> registration = Bukkit.getServicesManager().getRegistration(Economy.class);
            if (registration != null) {
                Economy registeredProvider = registration.getProvider();
                if (registeredProvider != null) {
                    try {
                        if (registeredProvider.isEnabled()) provider = registeredProvider;
                    } catch (RuntimeException exception) {
                        plugin.getLogger().warning("Could not validate the Vault economy provider: " + exception.getMessage());
                    }
                }
            }
        }

        String providerName = provider == null ? null : providerName(provider);
        boolean stateChanged = !initialized || detected != vaultDetected || !Objects.equals(providerName, lastProviderName);
        economy = provider;
        vaultDetected = detected;
        lastProviderName = providerName;
        initialized = true;

        if (!stateChanged) return;
        if (!detected) {
            plugin.getLogger().warning("Vault was not detected.");
            plugin.getLogger().warning("Money rewards are unavailable.");
        } else if (provider == null) {
            plugin.getLogger().info("Vault detected, but no Economy provider is registered.");
            plugin.getLogger().info("Money rewards are temporarily unavailable.");
        } else {
            plugin.getLogger().info("Vault detected.");
            plugin.getLogger().info("Economy provider detected: " + providerName);
            plugin.getLogger().info("Money rewards enabled.");
        }
    }

    @Override
    public boolean isAvailable() {
        refresh();
        return economy != null;
    }

    @Override
    public boolean hasEconomyProvider() {
        return isAvailable();
    }

    @Override
    public double generateAmount(double minimum, double maximum) {
        double safeMinimum = Double.isFinite(minimum) ? Math.max(0, minimum) : 0;
        double safeMaximum = Double.isFinite(maximum) ? Math.max(safeMinimum, maximum) : safeMinimum;
        int decimals = Math.max(0, Math.min(6, plugin.getConfig().getInt("economy.decimals", 2)));
        BigDecimal low = BigDecimal.valueOf(safeMinimum).setScale(decimals, RoundingMode.DOWN);
        BigDecimal high = BigDecimal.valueOf(safeMaximum).setScale(decimals, RoundingMode.DOWN);
        long lower;
        long upper;
        try {
            lower = low.movePointRight(decimals).longValueExact();
            upper = high.movePointRight(decimals).longValueExact();
        } catch (ArithmeticException exception) {
            if (high.compareTo(low) <= 0) return low.doubleValue();
            BigDecimal offset = high.subtract(low).multiply(BigDecimal.valueOf(ThreadLocalRandom.current().nextDouble()));
            return low.add(offset).setScale(decimals, RoundingMode.DOWN).doubleValue();
        }
        if (upper <= lower) return low.doubleValue();
        if (upper == Long.MAX_VALUE) {
            BigDecimal offset = high.subtract(low).multiply(BigDecimal.valueOf(ThreadLocalRandom.current().nextDouble()));
            return low.add(offset).setScale(decimals, RoundingMode.DOWN).doubleValue();
        }
        long value = ThreadLocalRandom.current().nextLong(lower, upper + 1);
        return BigDecimal.valueOf(value, decimals).doubleValue();
    }

    @Override
    public boolean deposit(Player player, double amount) {
        return deposit((OfflinePlayer) player, amount);
    }

    @Override
    public boolean deposit(OfflinePlayer player, double amount) {
        Economy provider = currentProvider();
        if (provider == null || player == null || !Double.isFinite(amount) || amount < 0) return false;
        String providerName = providerName(provider);
        try {
            EconomyResponse response = provider.depositPlayer(player, amount);
            if (response != null && response.transactionSuccess()) return true;
            String reason = response == null ? "Provider returned no response" : response.errorMessage;
            logDepositFailure(player, amount, providerName, reason);
            return false;
        } catch (RuntimeException exception) {
            logDepositFailure(player, amount, providerName, exception.getMessage());
            return false;
        }
    }

    @Override
    public double getBalance(Player player) {
        Economy provider = currentProvider();
        if (provider == null) return 0;
        try {
            return provider.getBalance(player);
        } catch (RuntimeException exception) {
            plugin.getLogger().warning("Could not read balance from economy provider " + providerName(provider) + ": " + exception.getMessage());
            return 0;
        }
    }

    @Override
    public String format(double amount) {
        int decimals = Math.max(0, Math.min(6, plugin.getConfig().getInt("economy.decimals", 2)));
        if (!Double.isFinite(amount)) return "0";
        return String.format(Locale.US, "%,." + decimals + "f", amount);
    }

    @Override
    public String getProviderName() {
        Economy provider = currentProvider();
        return provider == null ? "unavailable" : providerName(provider);
    }

    private Economy currentProvider() {
        refresh();
        return economy;
    }

    private String providerName(Economy provider) {
        try {
            String name = provider.getName();
            return name == null || name.isBlank() ? provider.getClass().getSimpleName() : name;
        } catch (RuntimeException exception) {
            return provider.getClass().getSimpleName();
        }
    }

    private void logDepositFailure(OfflinePlayer player, double amount, String providerName, String reason) {
        String playerName = player.getName() == null ? player.getUniqueId().toString() : player.getName();
        plugin.getLogger().warning("Failed to deposit money reward. Player: " + playerName + ", Amount: " + format(amount)
            + ", Economy Provider: " + providerName + ", Reason: " + (reason == null || reason.isBlank() ? "unspecified" : reason));
    }
}