package com.districtx.pacificalootsystem.link;

import com.districtx.pacificalootsystem.PacificaLootSystem;
import com.districtx.pacificalootsystem.api.LinkedLoot;
import com.districtx.pacificalootsystem.api.LinkedLootService;
import com.districtx.pacificalootsystem.api.LootContainerState;
import com.districtx.pacificalootsystem.api.LootTable;
import com.districtx.pacificalootsystem.api.event.LinkedLootCreateEvent;
import com.districtx.pacificalootsystem.api.event.LinkedLootRemoveEvent;
import com.districtx.pacificalootsystem.database.DatabaseManager;
import com.districtx.pacificalootsystem.database.LootContainerRepository;
import com.districtx.pacificalootsystem.util.MessageUtil;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.block.Container;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Collections;
import java.util.Map;
import java.util.UUID;

public final class LootLinkManager implements LinkedLootService {
    private final PacificaLootSystem plugin;
    private final Map<String, LinkedLoot> links = new LinkedHashMap<>();
    private final Map<UUID, LootContainerState> states = new LinkedHashMap<>();
    private final Map<UUID, String> pending = new HashMap<>();
    private File file;
    private FileConfiguration config;
    private File stateFile;
    private FileConfiguration stateConfig;
    private final DatabaseManager database;
    private final LootContainerRepository containerRepository;

    public LootLinkManager(PacificaLootSystem plugin) {
        this.plugin = plugin;
        database = new DatabaseManager(plugin);
        containerRepository = new LootContainerRepository(database);
    }

    public void load() {
        file = new File(plugin.getDataFolder(), "links.yml");
        config = YamlConfiguration.loadConfiguration(file);
        links.clear();
        ConfigurationSection section = config.getConfigurationSection("links");
        if (section != null) {
            for (String rawKey : section.getKeys(false)) {
                ConfigurationSection value = section.getConfigurationSection(rawKey);
                if (value != null) {
                    LinkedLoot link = readLink(rawKey, value);
                    if (link != null && link.getLocation() != null) links.put(key(link.getLocation()), link);
                    continue;
                }
                LinkedLoot legacy = readLegacyLink(rawKey, section.getString(rawKey));
                if (legacy != null) links.put(key(legacy.getLocation()), legacy);
            }
        }
        for (LinkedLoot link : links.values()) {
            containerRepository.loadCooldown(link.getId()).ifPresent(link::setCooldownEndsAt);
        }
        loadStates();
        save();
    }

    public void save() {
        if (config == null) config = new YamlConfiguration();
        config.set("links", null);
        for (LinkedLoot link : links.values()) {
            String path = "links." + link.getId();
            config.set(path + ".loot-table-id", link.getLootTableId().toString());
            config.set(path + ".world-id", link.getWorldId().toString());
            config.set(path + ".x", link.getX());
            config.set(path + ".y", link.getY());
            config.set(path + ".z", link.getZ());
            config.set(path + ".block-type", link.getBlockType());
            config.set(path + ".cooldown-ends-at", link.getCooldownEndsAt());
            config.set(path + ".xp-reward-claimed", link.isXpRewardClaimed());
            config.set(path + ".created-at", link.getCreatedAt());
            config.set(path + ".updated-at", link.getUpdatedAt());
            containerRepository.saveLinkedLoot(link);
        }
        try {
            config.save(file);
        } catch (IOException exception) {
            plugin.getLogger().warning("Could not save links.yml: " + exception.getMessage());
        }
        saveStates();
    }

    @Override
    public LinkedLoot link(UUID lootTableId, Location location) {
        if (lootTableId == null || location == null || location.getWorld() == null || !isAllowed(location) || isLinked(location)) return null;
        int maximum = plugin.getConfig().getInt("linked-loot.max-linked-containers-per-loot-table", -1);
        if (maximum >= 0 && getLinkedLoot(lootTableId).size() >= maximum) return null;
        LinkedLoot linked = new LinkedLoot(UUID.randomUUID(), lootTableId, location.getWorld().getUID(),
            location.getBlockX(), location.getBlockY(), location.getBlockZ(), location.getBlock().getType().name(), System.currentTimeMillis());
        links.put(key(location), linked);
        save();
        if (plugin.getApi() != null) plugin.getApi().getEventService().call(new LinkedLootCreateEvent(linked));
        return linked;
    }

    @Override
    public void unlink(Location location) {
        if (location != null) {
            LinkedLoot removed = links.remove(key(location));
            if (removed != null) {
                if (plugin.getApi() != null) plugin.getApi().getEventService().call(new LinkedLootRemoveEvent(removed));
                if (!plugin.getConfig().getBoolean("linked-loot.unlink.preserve-items", false)) {
                    states.remove(removed.getId());
                    containerRepository.delete(removed.getId());
                }
                save();
                if (plugin.getHologramManager() != null) plugin.getHologramManager().refresh();
            }
        }
    }

    @Override
    public java.util.Optional<LinkedLoot> find(Location location) {
        return java.util.Optional.ofNullable(location == null ? null : links.get(key(location)));
    }

    @Override
    public java.util.Optional<LinkedLoot> findById(UUID linkedLootId) {
        if (linkedLootId == null) return java.util.Optional.empty();
        return links.values().stream().filter(link -> linkedLootId.equals(link.getId())).findFirst();
    }

    @Override
    public List<LinkedLoot> getLinkedLoot(UUID lootTableId) {
        List<LinkedLoot> result = new ArrayList<>();
        for (LinkedLoot link : links.values()) if (link.getLootTableId().equals(lootTableId)) result.add(link);
        return result;
    }

    @Override
    public List<LinkedLoot> findByLootTable(UUID lootTableId) {
        return getLinkedLoot(lootTableId);
    }

    public List<LinkedLoot> getAllLinkedLoot() { return new ArrayList<>(links.values()); }

    @Override
    public java.util.Collection<LinkedLoot> getLinkedLoot() { return Collections.unmodifiableList(getAllLinkedLoot()); }

    @Override
    public void link(Location location, String lootTableName) {
        LootTable table = plugin.getTableManager().get(lootTableName);
        if (table == null) throw new IllegalArgumentException("Unknown loot table: " + lootTableName);
        if (link(table.getUuid(), location) == null) throw new IllegalStateException("The location cannot be linked.");
    }

    @Override
    public boolean isLinked(Location location) { return find(location).isPresent(); }

    @Override
    public void unlinkAll(UUID lootTableId) {
        if (lootTableId != null) {
            List<UUID> removed = links.values().stream().filter(link -> link.getLootTableId().equals(lootTableId)).map(LinkedLoot::getId).toList();
            List<LinkedLoot> removedLinks = links.values().stream().filter(link -> link.getLootTableId().equals(lootTableId)).toList();
            if (links.values().removeIf(link -> link.getLootTableId().equals(lootTableId))) {
                if (plugin.getApi() != null) removedLinks.forEach(link -> plugin.getApi().getEventService().call(new LinkedLootRemoveEvent(link)));
                if (!plugin.getConfig().getBoolean("linked-loot.unlink.preserve-items", false)) {
                    removed.forEach(states::remove);
                    removed.forEach(containerRepository::delete);
                }
                save();
                if (plugin.getHologramManager() != null) plugin.getHologramManager().refresh();
            }
        }
    }

    @Override
    public LootContainerState getState(UUID linkedLootId) {
        if (linkedLootId == null) return new LootContainerState(null);
        LootContainerState state = states.get(linkedLootId);
        if (state == null) {
            state = new LootContainerState(linkedLootId);
            states.put(linkedLootId, state);
        }
        return new LootContainerState(state.getLinkedLootId(), state.getContents(), state.getUpdatedAt());
    }

    @Override
    public void saveState(UUID linkedLootId, LootContainerState state) {
        if (linkedLootId == null || state == null) return;
        state.setLinkedLootId(linkedLootId);
        state.setUpdatedAt(System.currentTimeMillis());
        states.put(linkedLootId, new LootContainerState(linkedLootId, state.getContents(), state.getUpdatedAt()));
        LinkedLoot link = findById(linkedLootId).orElse(null);
        if (link != null) {
            link.setUpdatedAt(state.getUpdatedAt());
            containerRepository.saveLinkedLoot(link);
        }
        if (!containerRepository.save(state)) saveStates();
    }

    @Override
    public boolean isOnCooldown(UUID linkedLootId) {
        return plugin.getConfig().getBoolean("cooldown.enabled", true)
            && findById(linkedLootId).map(link -> link.getCooldownEndsAt() > System.currentTimeMillis()).orElse(false);
    }

    @Override
    public long getCooldownRemaining(UUID linkedLootId) {
        if (!plugin.getConfig().getBoolean("cooldown.enabled", true)) return 0;
        long remaining = findById(linkedLootId).map(LinkedLoot::getCooldownEndsAt).orElse(0L) - System.currentTimeMillis();
        return remaining <= 0 ? 0 : (remaining + 999) / 1000;
    }

    @Override
    public void startCooldown(UUID linkedLootId, long durationSeconds) {
        LinkedLoot link = findById(linkedLootId).orElse(null);
        if (link == null) return;
        link.setCooldownEndsAt(!plugin.getConfig().getBoolean("cooldown.enabled", true) || durationSeconds <= 0
            ? 0L : System.currentTimeMillis() + durationSeconds * 1000L);
        link.setUpdatedAt(System.currentTimeMillis());
        save();
    }

    @Override
    public void clearCooldown(UUID linkedLootId) {
        LinkedLoot link = findById(linkedLootId).orElse(null);
        if (link == null) return;
        link.setCooldownEndsAt(0L);
        link.setUpdatedAt(System.currentTimeMillis());
        save();
    }

    public void beginLink(Player player, String table) {
        pending.put(player.getUniqueId(), table);
        player.sendMessage("§eRight-click a supported container to link it to §f" + table + "§e.");
    }

    public void beginUnlink(Player player) {
        pending.put(player.getUniqueId(), "");
        player.sendMessage("§eRight-click a linked container to unlink it.");
    }

    public void handleInteract(PlayerInteractEvent event) {
        if (!plugin.getConfig().getBoolean("linked-loot.enabled", true)) return;
        if (event.getAction() != Action.RIGHT_CLICK_BLOCK || event.getClickedBlock() == null) return;
        Player player = event.getPlayer();
        String requested = pending.remove(player.getUniqueId());
        if (requested == null) return;
        event.setCancelled(true);
        Location location = event.getClickedBlock().getLocation();
        if (requested.isBlank()) {
            if (!isLinked(location)) {
                player.sendMessage("§cThat block is not linked.");
                return;
            }
            unlink(location);
            plugin.getHologramManager().refresh();
            MessageUtil.send(plugin, player, "linked-loot.unlinked");
            return;
        }
        LootTable table = plugin.getTableManager().get(requested);
        if (table == null) {
            MessageUtil.send(plugin, player, "loot-invalid", Map.of("loot_table", requested));
            return;
        }
        if (!isAllowed(location)) {
            MessageUtil.send(plugin, player, "linked-loot.invalid-block");
            return;
        }
        if (isLinked(location)) {
            MessageUtil.send(plugin, player, "linked-loot.already-linked");
            return;
        }
        if (link(table.getUuid(), location) == null) {
            player.sendMessage("§cThis loot table has reached its linked-container limit.");
            return;
        }
        plugin.getHologramManager().refresh();
        String lootName = table.getDisplayName() == null ? table.getId() : table.getDisplayName();
        MessageUtil.send(plugin, player, "linked-loot.linked", Map.of("loot-name", lootName, "block-type", location.getBlock().getType().name()));
    }

    public String get(Location location) {
        LinkedLoot link = find(location).orElse(null);
        if (link == null) return null;
        return getTableId(link.getLootTableId());
    }

    public Map<String, String> all() {
        Map<String, String> result = new LinkedHashMap<>();
        for (LinkedLoot link : links.values()) {
            String table = getTableId(link.getLootTableId());
            if (table != null && link.getLocation() != null) result.put(key(link.getLocation()), table);
        }
        return result;
    }

    private String getTableId(UUID tableId) {
        for (LootTable table : plugin.getTableManager().all()) if (table.getUuid().equals(tableId)) return table.getId();
        return null;
    }

    private boolean isAllowed(Location location) {
        if (!(location.getBlock().getState() instanceof Container)) return false;
        List<String> allowed = plugin.getConfig().getStringList("linked-loot.allowed-blocks");
        if (allowed.isEmpty()) allowed = List.of("CHEST", "BARREL");
        String type = location.getBlock().getType().name();
        return allowed.stream().anyMatch(value -> value.equalsIgnoreCase(type));
    }

    private LinkedLoot readLink(String rawId, ConfigurationSection section) {
        try {
            UUID id = UUID.fromString(rawId);
            UUID tableId = UUID.fromString(section.getString("loot-table-id", ""));
            UUID worldId = UUID.fromString(section.getString("world-id", ""));
            LinkedLoot link = new LinkedLoot(id, tableId, worldId, section.getInt("x"), section.getInt("y"), section.getInt("z"),
                section.getString("block-type", "UNKNOWN"), section.getLong("cooldown-ends-at", 0),
                section.getLong("created-at", 0), section.getLong("updated-at", section.getLong("created-at", 0)));
            link.setXpRewardClaimed(section.getBoolean("xp-reward-claimed", false));
            return link;
        } catch (IllegalArgumentException ignored) {
            return null;
        }
    }

    private LinkedLoot readLegacyLink(String rawLocation, String tableId) {
        if (tableId == null) return null;
        LootTable table = plugin.getTableManager().get(tableId);
        String[] parts = rawLocation.split(",", -1);
        if (table == null || parts.length != 4) return null;
        org.bukkit.World world = Bukkit.getWorld(parts[0]);
        if (world == null) return null;
        try {
            Location location = new Location(world, Integer.parseInt(parts[1]), Integer.parseInt(parts[2]), Integer.parseInt(parts[3]));
            return new LinkedLoot(UUID.randomUUID(), table.getUuid(), world.getUID(), location.getBlockX(), location.getBlockY(), location.getBlockZ(), location.getBlock().getType().name(), System.currentTimeMillis());
        } catch (NumberFormatException ignored) {
            return null;
        }
    }

    private String key(Location location) {
        if (location == null || location.getWorld() == null) return "";
        return location.getWorld().getUID() + "," + location.getBlockX() + "," + location.getBlockY() + "," + location.getBlockZ();
    }

    private void loadStates() {
        stateFile = new File(plugin.getDataFolder(), "loot-container-contents.yml");
        stateConfig = YamlConfiguration.loadConfiguration(stateFile);
        states.clear();
        ConfigurationSection section = stateConfig.getConfigurationSection("containers");
        if (section != null) {
            for (String rawId : section.getKeys(false)) {
                try {
                    UUID linkedLootId = UUID.fromString(rawId);
                    Map<Integer, org.bukkit.inventory.ItemStack> contents = new LinkedHashMap<>();
                    ConfigurationSection items = section.getConfigurationSection(rawId + ".contents");
                    if (items != null) for (String rawSlot : items.getKeys(false)) {
                        org.bukkit.inventory.ItemStack item = items.getItemStack(rawSlot);
                        if (item != null && !item.getType().isAir()) contents.put(Integer.parseInt(rawSlot), item);
                    }
                    states.put(linkedLootId, new LootContainerState(linkedLootId, contents, section.getLong(rawId + ".updated-at", 0)));
                } catch (IllegalArgumentException ignored) { }
            }
        }
        for (LinkedLoot link : links.values()) {
            containerRepository.load(link.getId()).ifPresent(state -> states.put(link.getId(), state));
        }
    }

    private void saveStates() {
        if (stateConfig == null) stateConfig = new YamlConfiguration();
        stateConfig.set("containers", null);
        for (LootContainerState state : states.values()) {
            String path = "containers." + state.getLinkedLootId();
            stateConfig.set(path + ".updated-at", state.getUpdatedAt());
            for (Map.Entry<Integer, org.bukkit.inventory.ItemStack> item : state.getContents().entrySet()) {
                stateConfig.set(path + ".contents." + item.getKey(), item.getValue());
            }
        }
        try {
            stateConfig.save(stateFile);
        } catch (IOException exception) {
            plugin.getLogger().warning("Could not save loot-container-contents.yml: " + exception.getMessage());
        }
    }

    public void close() {
        database.close();
    }
}