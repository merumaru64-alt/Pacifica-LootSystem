package com.districtx.pacificalootsystem.internal.rank;

import com.districtx.pacificalootsystem.PacificaLootSystem;
import com.districtx.pacificalootsystem.api.LootRankBonusService;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

import java.util.Locale;
import java.util.Optional;
import java.util.UUID;

/** Optional LuckPerms adapter, isolated from class loading when LuckPerms is not installed. */
public final class LuckPermsLootRankBonusService implements LootRankBonusService {
    private final PacificaLootSystem plugin;
    private boolean warningLogged;

    public LuckPermsLootRankBonusService(PacificaLootSystem plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean isAvailable() {
        Plugin luckPerms = Bukkit.getPluginManager().getPlugin("LuckPerms");
        return luckPerms != null && luckPerms.isEnabled();
    }

    @Override
    public Optional<String> getApplicableRank(Player player) {
        if (player == null || !isAvailable()) return Optional.empty();
        Plugin luckPerms = Bukkit.getPluginManager().getPlugin("LuckPerms");
        try {
            ClassLoader loader = luckPerms.getClass().getClassLoader();
            Class<?> providerType = loader.loadClass("net.luckperms.api.LuckPermsProvider");
            Object api = providerType.getMethod("get").invoke(null);
            Class<?> apiType = loader.loadClass("net.luckperms.api.LuckPerms");
            Object userManager = apiType.getMethod("getUserManager").invoke(api);
            Class<?> userManagerType = loader.loadClass("net.luckperms.api.model.user.UserManager");
            Object user = userManagerType.getMethod("getUser", UUID.class).invoke(userManager, player.getUniqueId());
            if (user == null) return Optional.empty();
            Class<?> userType = loader.loadClass("net.luckperms.api.model.user.User");
            Object group = userType.getMethod("getPrimaryGroup").invoke(user);
            return group instanceof String rank && !rank.isBlank() ? Optional.of(rank) : Optional.empty();
        } catch (ReflectiveOperationException | LinkageError | RuntimeException exception) {
            if (!warningLogged) {
                plugin.getLogger().warning("LuckPerms primary-group lookup failed: " + exception.getMessage());
                warningLogged = true;
            }
            return Optional.empty();
        }
    }

    @Override
    public double getXpBonusPercent(Player player) {
        String rank = getApplicableRank(player).orElse("").toLowerCase(Locale.ROOT);
        String path = switch (rank) {
            case "vip" -> "xp-rewards.rank-bonuses.vip";
            case "godfather" -> "xp-rewards.rank-bonuses.godfather";
            default -> null;
        };
        if (path == null) return 0.0;
        double bonus = plugin.getConfig().getDouble(path, rank.equals("vip") ? 0.07 : 0.05);
        return Double.isFinite(bonus) && bonus > 0.0 ? bonus : 0.0;
    }
}