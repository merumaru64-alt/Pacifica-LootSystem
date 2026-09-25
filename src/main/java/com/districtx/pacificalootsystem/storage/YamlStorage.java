package com.districtx.pacificalootsystem.storage;

import com.districtx.pacificalootsystem.PacificaLootSystem;
import com.districtx.pacificalootsystem.api.Collectable;
import com.districtx.pacificalootsystem.api.LootEntry;
import com.districtx.pacificalootsystem.api.LootTable;
import com.districtx.pacificalootsystem.api.MoneyReward;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

public final class YamlStorage {
    private final PacificaLootSystem plugin;
    private File file;
    private FileConfiguration config;

    public YamlStorage(PacificaLootSystem plugin) {
        this.plugin = plugin;
    }

    public Map<String, LootTable> loadTables() {
        file = new File(plugin.getDataFolder(), "loot-tables.yml");
        config = YamlConfiguration.loadConfiguration(file);
        Map<String, LootTable> result = new LinkedHashMap<>();
        ConfigurationSection section = config.getConfigurationSection("tables");
        if (section == null) return result;
        for (String id : section.getKeys(false)) {
            ConfigurationSection t = section.getConfigurationSection(id);
            if (t == null) continue;

            LootTable table = new LootTable(id, t.getString("display-name", id));
            String rawUuid = t.getString("uuid");
            if (rawUuid != null) {
                try {
                    table.setUuid(UUID.fromString(rawUuid));
                } catch (IllegalArgumentException ignored) {
                    table.setUuid(UUID.randomUUID());
                }
            }
            table.setDescription(t.getString("description", ""));
            table.setEnabled(t.getBoolean("enabled", true));
            table.setSelectionMode(t.getString("selection-mode", "RANDOM"));
            table.setMinimumRewards(Math.max(0, t.getInt("minimum-rewards", 1)));
            table.setMaximumRewards(Math.max(table.getMinimumRewards(), t.getInt("maximum-rewards", 1)));
            table.setMinimumXpReward(Math.max(0, t.getInt("xp-reward.minimum", 0)));
            table.setMaximumXpReward(Math.max(table.getMinimumXpReward(), t.getInt("xp-reward.maximum", table.getMinimumXpReward())));
            table.setCooldownSeconds(Math.max(0, t.getLong("cooldown-seconds", 0)));
            table.setGlobalMode(t.getBoolean("global-mode", false));
            table.setLootMode("PHYSICAL");
            table.setCost(Math.max(0, t.getDouble("cost", 0)));
            table.setPermission(t.getString("permission"));
            table.setConditions(new ArrayList<>(t.getStringList("conditions")));
            ConfigurationSection moneyRewards = t.getConfigurationSection("money-rewards");
            if (moneyRewards != null) for (String rawId : moneyRewards.getKeys(false)) {
                ConfigurationSection money = moneyRewards.getConfigurationSection(rawId);
                if (money == null) continue;
                try {
                    UUID moneyId = UUID.fromString(rawId);
                    table.getMoneyRewards().put(moneyId, new MoneyReward(moneyId, table.getUuid(), parseNullableUuid(money.getString("collectable-id")),
                        money.getDouble("minimum-amount", 0), money.getDouble("maximum-amount", 0), money.getBoolean("enabled", true)));
                } catch (IllegalArgumentException ignored) { }
            }

            ConfigurationSection collectables = t.getConfigurationSection("collectables");
            if (collectables != null) {
                for (String rawCollectableId : collectables.getKeys(false)) {
                    ConfigurationSection c = collectables.getConfigurationSection(rawCollectableId);
                    if (c == null) continue;
                    Collectable collectable = new Collectable(c.getString("name", rawCollectableId));
                    try {
                        collectable.setId(UUID.fromString(rawCollectableId));
                    } catch (IllegalArgumentException ignored) {
                        collectable.setId(UUID.randomUUID());
                    }
                    collectable.setDisplayName(c.getString("display-name", collectable.getName()));
                    collectable.setChance(Math.max(0, Math.min(100, c.getDouble("chance", 100))));
                    collectable.setWeight(Math.max(0, c.getDouble("weight", 1)));
                    collectable.setMinimumRewards(Math.max(0, c.getInt("minimum-rewards", 0)));
                    collectable.setMaximumRewards(Math.max(collectable.getMinimumRewards(), c.getInt("maximum-rewards", 1)));
                    collectable.setSelectionMode(c.getString("selection-mode", "RANDOM"));
                    collectable.setEnabled(c.getBoolean("enabled", true));

                    loadEntries(c.getConfigurationSection("entries"), collectable.getEntries());
                    table.getCollectables().put(collectable.getId(), collectable);
                    for (LootEntry entry : collectable.getEntries().values()) {
                        table.getEntries().put(entry.getId(), entry);
                    }
                }
            }

            ConfigurationSection legacyEntries = t.getConfigurationSection("entries");
            if (legacyEntries != null) {
                Collectable legacy = null;
                for (String rawId : legacyEntries.getKeys(false)) {
                    if (table.getEntries().containsKey(parseUuid(rawId))) continue;
                    ConfigurationSection e = legacyEntries.getConfigurationSection(rawId);
                    if (e == null) continue;
                    LootEntry entry = readEntry(e, rawId);
                    table.getEntries().put(entry.getId(), entry);
                    if (legacy == null) {
                        legacy = new Collectable("Legacy");
                        legacy.setMinimumRewards(table.getMinimumRewards());
                        legacy.setMaximumRewards(table.getMaximumRewards());
                        legacy.setSelectionMode(table.getSelectionMode());
                        table.getCollectables().put(legacy.getId(), legacy);
                    }
                    legacy.getEntries().put(entry.getId(), entry);
                }
            }
            result.put(id.toLowerCase(), table);
        }
        return result;
    }

    private void loadEntries(ConfigurationSection section, Map<UUID, LootEntry> target) {
        if (section == null) return;
        for (String rawId : section.getKeys(false)) {
            ConfigurationSection e = section.getConfigurationSection(rawId);
            if (e == null) continue;
            LootEntry entry = readEntry(e, rawId);
            target.put(entry.getId(), entry);
        }
    }

    private LootEntry readEntry(ConfigurationSection e, String rawId) {
        LootEntry entry = new LootEntry();
        entry.setId(parseUuid(rawId));
        entry.setType(e.getString("type", "ITEM"));
        entry.setItem(e.getItemStack("item"));
        entry.setMinimumAmount(Math.max(1, e.getInt("minimum-amount", 1)));
        entry.setMaximumAmount(Math.max(entry.getMinimumAmount(), e.getInt("maximum-amount", entry.getMinimumAmount())));
        entry.setAutoPickup(e.getBoolean("auto-pickup", false));
        entry.setChance(Math.max(0, Math.min(100, e.getDouble("chance", 100))));
        entry.setWeight(Math.max(0, e.getDouble("weight", 1)));
        entry.setName(e.getString("name"));
        entry.setLore(new ArrayList<>(e.getStringList("lore")));
        entry.setCommand(e.getString("command"));
        entry.setMinimumMoney(Math.max(0, e.getDouble("minimum-money", 0)));
        entry.setMaximumMoney(Math.max(entry.getMinimumMoney(), e.getDouble("maximum-money", entry.getMinimumMoney())));
        entry.setMinimumExperience(Math.max(0, e.getInt("minimum-experience", 0)));
        entry.setMaximumExperience(Math.max(entry.getMinimumExperience(), e.getInt("maximum-experience", entry.getMinimumExperience())));
        entry.setExperienceLevels(e.getBoolean("experience-levels", false));
        entry.setEnabled(e.getBoolean("enabled", true));
        entry.setConditions(new ArrayList<>(e.getStringList("conditions")));
        return entry;
    }

    private UUID parseUuid(String rawId) {
        try {
            return UUID.fromString(rawId);
        } catch (IllegalArgumentException ignored) {
            return UUID.randomUUID();
        }
    }

    private UUID parseNullableUuid(String rawId) {
        if (rawId == null || rawId.isBlank()) return null;
        try { return UUID.fromString(rawId); } catch (IllegalArgumentException ignored) { return null; }
    }

    public void saveTables(Map<String, LootTable> tables) {
        if (config == null) config = new YamlConfiguration();
        config.set("tables", null);
        for (LootTable table : tables.values()) {
            String p = "tables." + table.getId();
            config.set(p + ".uuid", table.getUuid().toString());
            config.set(p + ".display-name", table.getDisplayName());
            config.set(p + ".description", table.getDescription());
            config.set(p + ".enabled", table.isEnabled());
            config.set(p + ".selection-mode", table.getSelectionMode());
            config.set(p + ".minimum-rewards", table.getMinimumRewards());
            config.set(p + ".maximum-rewards", table.getMaximumRewards());
            config.set(p + ".xp-reward.minimum", table.getMinimumXpReward());
            config.set(p + ".xp-reward.maximum", table.getMaximumXpReward());
            config.set(p + ".cooldown-seconds", table.getCooldownSeconds());
            config.set(p + ".global-mode", table.isGlobalMode());
            config.set(p + ".loot-mode", "PHYSICAL");
            config.set(p + ".cost", table.getCost());
            config.set(p + ".permission", table.getPermission());
            config.set(p + ".conditions", table.getConditions());
            for (MoneyReward money : table.getMoneyRewards().values()) {
                String m = p + ".money-rewards." + money.getId();
                config.set(m + ".collectable-id", money.getCollectableId() == null ? null : money.getCollectableId().toString());
                config.set(m + ".minimum-amount", money.getMinimumAmount());
                config.set(m + ".maximum-amount", money.getMaximumAmount());
                config.set(m + ".enabled", money.isEnabled());
            }

            for (Collectable collectable : table.getCollectables().values()) {
                String c = p + ".collectables." + collectable.getId();
                config.set(c + ".name", collectable.getName());
                config.set(c + ".display-name", collectable.getDisplayName());
                config.set(c + ".chance", collectable.getChance());
                config.set(c + ".weight", collectable.getWeight());
                config.set(c + ".minimum-rewards", collectable.getMinimumRewards());
                config.set(c + ".maximum-rewards", collectable.getMaximumRewards());
                config.set(c + ".selection-mode", collectable.getSelectionMode());
                config.set(c + ".enabled", collectable.isEnabled());
                for (LootEntry entry : collectable.getEntries().values()) {
                    writeEntry(c + ".entries." + entry.getId(), entry);
                }
            }

            for (LootEntry entry : table.getEntries().values()) {
                writeEntry(p + ".entries." + entry.getId(), entry);
            }
        }
        try {
            config.save(file);
        } catch (IOException exception) {
            plugin.getLogger().severe("Could not save loot-tables.yml: " + exception.getMessage());
        }
    }

    private void writeEntry(String path, LootEntry entry) {
        config.set(path + ".type", entry.getType());
        config.set(path + ".item", entry.getItem());
        config.set(path + ".minimum-amount", entry.getMinimumAmount());
        config.set(path + ".maximum-amount", entry.getMaximumAmount());
        config.set(path + ".auto-pickup", entry.isAutoPickup());
        config.set(path + ".chance", entry.getChance());
        config.set(path + ".weight", entry.getWeight());
        config.set(path + ".name", entry.getName());
        config.set(path + ".lore", entry.getLore());
        config.set(path + ".command", entry.getCommand());
        config.set(path + ".minimum-money", entry.getMinimumMoney());
        config.set(path + ".maximum-money", entry.getMaximumMoney());
        config.set(path + ".minimum-experience", entry.getMinimumExperience());
        config.set(path + ".maximum-experience", entry.getMaximumExperience());
        config.set(path + ".experience-levels", entry.isExperienceLevels());
        config.set(path + ".enabled", entry.isEnabled());
        config.set(path + ".conditions", entry.getConditions());
    }
}