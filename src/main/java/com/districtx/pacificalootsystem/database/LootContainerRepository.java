package com.districtx.pacificalootsystem.database;

import com.districtx.pacificalootsystem.api.LootContainerState;
import com.districtx.pacificalootsystem.api.LinkedLoot;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.inventory.ItemStack;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

public final class LootContainerRepository {
    private final DatabaseManager database;

    public LootContainerRepository(DatabaseManager database) {
        this.database = database;
    }

    public Optional<LootContainerState> load(UUID linkedLootId) {
        if (!database.isAvailable()) return Optional.empty();
        Map<Integer, ItemStack> contents = new LinkedHashMap<>();
        long updatedAt;
        try {
            try (PreparedStatement state = database.connection().prepareStatement(
                "SELECT updated_at FROM loot_container_states WHERE linked_loot_id = ?")) {
                state.setString(1, linkedLootId.toString());
                try (ResultSet result = state.executeQuery()) {
                    if (!result.next()) return Optional.empty();
                    updatedAt = result.getLong("updated_at");
                }
            }
            try (PreparedStatement statement = database.connection().prepareStatement(
                "SELECT slot, item_data FROM loot_container_contents WHERE linked_loot_id = ?")) {
                statement.setString(1, linkedLootId.toString());
                try (ResultSet result = statement.executeQuery()) {
                    while (result.next()) {
                        ItemStack item = deserialize(result.getString("item_data"));
                        if (item != null && !item.getType().isAir()) contents.put(result.getInt("slot"), item);
                    }
                }
            }
            return Optional.of(new LootContainerState(linkedLootId, contents, updatedAt));
        } catch (SQLException exception) {
            return Optional.empty();
        }
    }

    public Optional<Long> loadCooldown(UUID linkedLootId) {
        if (!database.isAvailable()) return Optional.empty();
        try (PreparedStatement statement = database.connection().prepareStatement(
            "SELECT cooldown_ends_at FROM linked_loot WHERE id = ?")) {
            statement.setString(1, linkedLootId.toString());
            try (ResultSet result = statement.executeQuery()) {
                return result.next() ? Optional.of(result.getLong("cooldown_ends_at")) : Optional.empty();
            }
        } catch (SQLException exception) {
            return Optional.empty();
        }
    }

    public boolean saveLinkedLoot(LinkedLoot linkedLoot) {
        if (!database.isAvailable()) return false;
        try (PreparedStatement statement = database.connection().prepareStatement(
            "INSERT OR REPLACE INTO linked_loot (id, loot_table_id, world_id, x, y, z, block_type, cooldown_ends_at, created_at, updated_at) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)")) {
            statement.setString(1, linkedLoot.getId().toString());
            statement.setString(2, linkedLoot.getLootTableId().toString());
            statement.setString(3, linkedLoot.getWorldId().toString());
            statement.setInt(4, linkedLoot.getX());
            statement.setInt(5, linkedLoot.getY());
            statement.setInt(6, linkedLoot.getZ());
            statement.setString(7, linkedLoot.getBlockType());
            statement.setLong(8, linkedLoot.getCooldownEndsAt());
            statement.setLong(9, linkedLoot.getCreatedAt());
            statement.setLong(10, linkedLoot.getUpdatedAt());
            statement.executeUpdate();
            return true;
        } catch (SQLException exception) {
            return false;
        }
    }

    public boolean save(LootContainerState state) {
        if (!database.isAvailable()) return false;
        Connection connection = database.connection();
        try {
            connection.setAutoCommit(false);
            try (PreparedStatement metadata = connection.prepareStatement(
                "INSERT OR REPLACE INTO loot_container_states (linked_loot_id, updated_at) VALUES (?, ?)")) {
                metadata.setString(1, state.getLinkedLootId().toString());
                metadata.setLong(2, state.getUpdatedAt());
                metadata.executeUpdate();
            }
            try (PreparedStatement delete = connection.prepareStatement(
                "DELETE FROM loot_container_contents WHERE linked_loot_id = ?")) {
                delete.setString(1, state.getLinkedLootId().toString());
                delete.executeUpdate();
            }
            try (PreparedStatement insert = connection.prepareStatement(
                "INSERT INTO loot_container_contents (linked_loot_id, slot, item_data, updated_at) VALUES (?, ?, ?, ?)")) {
                for (Map.Entry<Integer, ItemStack> entry : state.getContents().entrySet()) {
                    insert.setString(1, state.getLinkedLootId().toString());
                    insert.setInt(2, entry.getKey());
                    insert.setString(3, serialize(entry.getValue()));
                    insert.setLong(4, state.getUpdatedAt());
                    insert.addBatch();
                }
                insert.executeBatch();
            }
            connection.commit();
            connection.setAutoCommit(true);
            return true;
        } catch (SQLException exception) {
            try { connection.rollback(); connection.setAutoCommit(true); } catch (SQLException ignored) { }
            return false;
        }
    }

    public void delete(UUID linkedLootId) {
        if (!database.isAvailable()) return;
        try (PreparedStatement statement = database.connection().prepareStatement(
            "DELETE FROM loot_container_contents WHERE linked_loot_id = ?")) {
            statement.setString(1, linkedLootId.toString());
            statement.executeUpdate();
            try (PreparedStatement state = database.connection().prepareStatement(
                "DELETE FROM loot_container_states WHERE linked_loot_id = ?")) {
                state.setString(1, linkedLootId.toString());
                state.executeUpdate();
            }
            try (PreparedStatement linked = database.connection().prepareStatement(
                "DELETE FROM linked_loot WHERE id = ?")) {
                linked.setString(1, linkedLootId.toString());
                linked.executeUpdate();
            }
        } catch (SQLException ignored) { }
    }

    private String serialize(ItemStack item) {
        YamlConfiguration config = new YamlConfiguration();
        config.set("item", item);
        return config.saveToString();
    }

    private ItemStack deserialize(String value) {
        try {
            YamlConfiguration config = new YamlConfiguration();
            config.loadFromString(value);
            return config.getItemStack("item");
        } catch (Exception ignored) {
            return null;
        }
    }
}