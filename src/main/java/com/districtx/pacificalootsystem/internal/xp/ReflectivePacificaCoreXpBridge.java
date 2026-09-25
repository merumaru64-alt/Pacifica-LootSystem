package com.districtx.pacificalootsystem.internal.xp;

import com.districtx.pacificalootsystem.PacificaLootSystem;
import com.districtx.pacificalootsystem.api.PacificaCoreXpBridge;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

import java.util.UUID;

/** Uses Pacifica-Core's public provider and PlayerLevelService without linking its classes at startup. */
public final class ReflectivePacificaCoreXpBridge implements PacificaCoreXpBridge {
    private final PacificaLootSystem plugin;
    private boolean warningLogged;

    public ReflectivePacificaCoreXpBridge(PacificaLootSystem plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean isAvailable() {
        return playerLevelService() != null;
    }

    @Override
    public boolean awardXp(Player player, int amount) {
        if (player == null || amount <= 0) return false;
        try {
            Plugin core = corePlugin();
            Object service = playerLevelService(core);
            if (service == null) return false;
            ClassLoader loader = core.getClass().getClassLoader();
            Class<?> sourceType = loader.loadClass("com.districtx.pacificacore.api.ExperienceSource");
            // LOOT can apply Pacifica-Core's own rank modifier, stacking with this plugin's explicit bonus.
            Object source = sourceType.getMethod("valueOf", String.class).invoke(null, "API");
            Class<?> serviceType = loader.loadClass("com.districtx.pacificacore.api.PlayerLevelService");
            Object result = serviceType.getMethod("addExperience", Player.class, double.class, sourceType)
                    .invoke(service, player, (double) amount, source);
            Object added = result.getClass().getMethod("getExperienceAdded").invoke(result);
            return added instanceof Number number && number.doubleValue() > 0.0;
        } catch (ReflectiveOperationException | LinkageError | RuntimeException exception) {
            warn(exception);
            return false;
        }
    }

    @Override
    public int getPlayerXp(Player player) {
        if (player == null) return 0;
        try {
            Plugin core = corePlugin();
            Object service = playerLevelService(core);
            if (service == null) return 0;
            Class<?> serviceType = core.getClass().getClassLoader()
                    .loadClass("com.districtx.pacificacore.api.PlayerLevelService");
            Object experience = serviceType.getMethod("getExperience", UUID.class)
                    .invoke(service, player.getUniqueId());
            if (!(experience instanceof Number number) || !Double.isFinite(number.doubleValue())) return 0;
            return (int) Math.min(Integer.MAX_VALUE, Math.max(0, Math.floor(number.doubleValue())));
        } catch (ReflectiveOperationException | LinkageError | RuntimeException exception) {
            warn(exception);
            return 0;
        }
    }

    private Object playerLevelService() {
        try {
            return playerLevelService(corePlugin());
        } catch (ReflectiveOperationException | LinkageError | RuntimeException exception) {
            warn(exception);
            return null;
        }
    }

    private Plugin corePlugin() {
        Plugin core = Bukkit.getPluginManager().getPlugin("Pacifica-Core");
        return core != null && core.isEnabled() ? core : null;
    }

    private Object playerLevelService(Plugin core) throws ReflectiveOperationException {
        if (core == null) return null;
        ClassLoader loader = core.getClass().getClassLoader();
        Class<?> coreType = loader.loadClass("com.districtx.pacificacore.PacificaCore");
        Object api = coreType.getMethod("getAPI").invoke(null);
        if (api == null) return null;
        Class<?> apiType = loader.loadClass("com.districtx.pacificacore.api.PacificaCoreAPI");
        return apiType.getMethod("getPlayerLevelService").invoke(api);
    }

    private void warn(Throwable exception) {
        if (warningLogged) return;
        plugin.getLogger().warning("Pacifica-Core XP API is unavailable: " + exception.getMessage());
        warningLogged = true;
    }
}