package com.districtx.pacificalootsystem.api;

import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;

/** Economy operations exposed without requiring integrations to link against Vault classes. */
public interface LootEconomyService {
    /** Returns whether a working Vault-compatible economy provider is available now. */
    boolean isAvailable();
    /** Returns whether the service currently has a usable provider. */
    boolean hasEconomyProvider();
    /** Generates a non-negative amount within the supplied bounds using configured currency precision. */
    double generateAmount(double minimum, double maximum);
    /** Deposits money to an online player and reports whether the provider completed the transaction. */
    boolean deposit(Player player, double amount);
    /** Deposits money to an offline or online account and reports transaction success. */
    boolean deposit(OfflinePlayer player, double amount);
    /** Returns the online player's balance, or zero when the provider is unavailable. */
    double getBalance(Player player);
    /** Formats an amount using the configured currency precision. */
    String format(double amount);
    /** Returns the active provider name, or "unavailable" when none is active. */
    String getProviderName();
    /** Rechecks Vault and the currently registered Economy provider. */
    void refresh();
}