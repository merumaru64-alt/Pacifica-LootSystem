package com.districtx.pacificalootsystem.link;

import com.districtx.pacificalootsystem.PacificaLootSystem;
import com.districtx.pacificalootsystem.api.LinkedLoot;
import com.districtx.pacificalootsystem.api.LootTable;
import com.districtx.pacificalootsystem.api.LootHologramService;
import com.districtx.pacificalootsystem.util.DurationUtil;
import com.districtx.pacificalootsystem.util.TextUtil;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.block.Container;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public final class LootHologramManager implements LootHologramService {
    private static final String TAG = "pacifica-loot-hologram";
    private final PacificaLootSystem plugin;
    private final Map<String, UUID[]> displays = new HashMap<>();
    private int taskId = -1;

    public LootHologramManager(PacificaLootSystem plugin) {
        this.plugin = plugin;
    }

    public void start() {
        removeTaggedDisplays();
        refresh();
        taskId = Bukkit.getScheduler().runTaskTimer(plugin, this::tick, 0L, 20L).getTaskId();
    }

    public void stop() {
        if (taskId != -1) {
            Bukkit.getScheduler().cancelTask(taskId);
            taskId = -1;
        }
        removeDisplays();
        removeTaggedDisplays();
    }

    public void refresh() {
        removeDisplays();
        removeTaggedDisplays();
        tick();
    }

    private void tick() {
        if (!plugin.getConfig().getBoolean("hologram.enabled", true)) {
            removeDisplays();
            removeTaggedDisplays();
            return;
        }
        for (LinkedLoot link : plugin.getLinkManager().getAllLinkedLoot()) {
            LootTable table = plugin.getTableManager().all().stream()
                .filter(value -> value.getUuid().equals(link.getLootTableId())).findFirst().orElse(null);
            Location location = link.getLocation();
            if (table == null || location == null || !(location.getBlock().getState() instanceof Container)) {
                remove(link.getId().toString());
                removeTaggedDisplays(link.getId().toString());
                continue;
            }
            ArmorStand[] pair = displays(link.getId().toString(), location);
            if (pair == null) continue;
            String name = table.getDisplayName() == null ? table.getId() : table.getDisplayName();
            long remaining = plugin.getLinkManager().getCooldownRemaining(link.getId());
            String path = remaining > 0 ? "hologram.lines.cooldown" : "hologram.lines.ready";
            String configured = plugin.getConfig().getString(path, remaining > 0
                ? "&6&l%loot-name% Crates|&7Restocking in &c%timer-cooldown%"
                : "&6&l%loot-name% Crates|&a&lRestocked");
            String text = configured.replace("%loot-name%", name).replace("%timer-cooldown%", DurationUtil.format(remaining));
            String[] lines = text.split("\\|", -1);
            pair[0].setCustomName(TextUtil.color(lines.length == 0 ? "" : lines[0]));
            pair[1].setCustomName(TextUtil.color(lines.length < 2 ? "" : lines[1]));
        }
    }

    private ArmorStand[] displays(String key, Location location) {
        UUID[] ids = displays.get(key);
        if (ids != null) {
            Entity first = Bukkit.getEntity(ids[0]);
            Entity second = Bukkit.getEntity(ids[1]);
            if (first instanceof ArmorStand firstStand && second instanceof ArmorStand secondStand
                && first.isValid() && second.isValid()) return new ArmorStand[]{firstStand, secondStand};
            remove(key);
        }
        removeTaggedDisplays(key);
        ArmorStand first = create(location.clone().add(0.5, 1.8, 0.5), key);
        ArmorStand second = create(location.clone().add(0.5, 1.5, 0.5), key);
        displays.put(key, new UUID[]{first.getUniqueId(), second.getUniqueId()});
        return new ArmorStand[]{first, second};
    }

    private ArmorStand create(Location location, String key) {
        ArmorStand stand = (ArmorStand) location.getWorld().spawnEntity(location, EntityType.ARMOR_STAND);
        stand.addScoreboardTag(TAG);
        stand.addScoreboardTag(TAG + ":" + key);
        stand.setVisible(false);
        stand.setInvulnerable(true);
        stand.setGravity(false);
        stand.setMarker(true);
        stand.setSilent(true);
        stand.setCustomNameVisible(true);
        stand.setPersistent(false);
        return stand;
    }

    private void remove(String key) {
        UUID[] ids = displays.remove(key);
        if (ids == null) return;
        for (UUID id : ids) {
            Entity entity = Bukkit.getEntity(id);
            if (entity != null) entity.remove();
        }
    }

    private void removeDisplays() {
        for (String key : new HashMap<>(displays).keySet()) remove(key);
    }

    private void removeTaggedDisplays(String key) {
        String pairTag = TAG + ":" + key;
        for (World world : Bukkit.getWorlds()) {
            for (Entity entity : world.getEntities()) {
                if (entity.getScoreboardTags().contains(pairTag)) entity.remove();
            }
        }
    }

    private void removeTaggedDisplays() {
        for (World world : Bukkit.getWorlds()) {
            for (Entity entity : world.getEntities()) {
                if (entity.getScoreboardTags().contains(TAG)) entity.remove();
            }
        }
    }
}