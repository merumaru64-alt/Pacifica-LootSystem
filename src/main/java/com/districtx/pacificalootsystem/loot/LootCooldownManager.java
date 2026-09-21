package com.districtx.pacificalootsystem.loot;

import com.districtx.pacificalootsystem.PacificaLootSystem;
import com.districtx.pacificalootsystem.api.LootTable;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;

import java.io.File;
import java.io.IOException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public final class LootCooldownManager {
    private final PacificaLootSystem plugin; private final Map<String, Long> lastLoot = new ConcurrentHashMap<>(); private File file; private FileConfiguration config;
    public LootCooldownManager(PacificaLootSystem plugin) { this.plugin = plugin; }
    public void load() { file = new File(plugin.getDataFolder(), "cooldowns.yml"); config = YamlConfiguration.loadConfiguration(file); lastLoot.clear(); for (String key : config.getKeys(false)) lastLoot.put(key, config.getLong(key)); }
    public void save() { if (config == null) config = new YamlConfiguration(); for (Map.Entry<String, Long> entry : lastLoot.entrySet()) config.set(entry.getKey(), entry.getValue()); try { config.save(file); } catch (IOException exception) { plugin.getLogger().warning("Could not save cooldowns: " + exception.getMessage()); } }
    private String key(Player player, LootTable table) { return table.isGlobalMode() ? "global." + table.getId() : player.getUniqueId() + "." + table.getId(); }
    private String sharedKey(LootTable table) { return "global." + table.getId(); }
    private boolean enabled() { return plugin.getConfig().getBoolean("cooldown.enabled", true); }
    public boolean isOnCooldown(Player player, LootTable table) { return enabled() && table != null && table.getCooldownSeconds() > 0 && System.currentTimeMillis() < lastLoot.getOrDefault(key(player, table), 0L) + table.getCooldownSeconds() * 1000L; }
    public boolean isOnGlobalCooldown(LootTable table) { return enabled() && table != null && table.getCooldownSeconds() > 0 && System.currentTimeMillis() < lastLoot.getOrDefault(sharedKey(table), 0L) + table.getCooldownSeconds() * 1000L; }
    public long remaining(Player player, LootTable table) { return secondsRemaining(lastLoot.getOrDefault(key(player, table), 0L), table); }
    public long remaining(LootTable table) {
        return table == null || !enabled() ? 0 : secondsRemaining(lastLoot.getOrDefault(sharedKey(table), 0L), table);
    }
    private long secondsRemaining(long startedAt, LootTable table) {
        long milliseconds = startedAt + table.getCooldownSeconds() * 1000L - System.currentTimeMillis();
        return milliseconds <= 0 ? 0 : (milliseconds + 999) / 1000;
    }
    public void mark(Player player, LootTable table) { if (table != null && table.getCooldownSeconds() > 0) { lastLoot.put(key(player, table), System.currentTimeMillis()); save(); } }
    public void markGlobal(LootTable table) { if (enabled() && table != null && table.getCooldownSeconds() > 0) { lastLoot.put(sharedKey(table), System.currentTimeMillis()); save(); } }
    public void reset(Player player, LootTable table) { if (table != null) { lastLoot.remove(key(player, table)); save(); } }
    public void resetTable(LootTable table) { if (table != null) { lastLoot.keySet().removeIf(key -> key.endsWith("." + table.getId())); save(); } }
}