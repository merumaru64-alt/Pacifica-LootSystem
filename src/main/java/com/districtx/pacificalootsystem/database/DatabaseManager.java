package com.districtx.pacificalootsystem.database;

import com.districtx.pacificalootsystem.PacificaLootSystem;

import java.io.File;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;

public final class DatabaseManager {
    private final PacificaLootSystem plugin;
    private Connection connection;

    public DatabaseManager(PacificaLootSystem plugin) {
        this.plugin = plugin;
        try {
            Class.forName("org.sqlite.JDBC");
            connection = DriverManager.getConnection("jdbc:sqlite:" + new File(plugin.getDataFolder(), "pacifica.db"));
            try (Statement statement = connection.createStatement()) {
                statement.executeUpdate("CREATE TABLE IF NOT EXISTS linked_loot (id CHAR(36) PRIMARY KEY, loot_table_id CHAR(36) NOT NULL, world_id CHAR(36) NOT NULL, x INT NOT NULL, y INT NOT NULL, z INT NOT NULL, block_type VARCHAR(64) NOT NULL, cooldown_ends_at BIGINT NOT NULL DEFAULT 0, created_at BIGINT NOT NULL, updated_at BIGINT NOT NULL, UNIQUE (world_id, x, y, z))");
                statement.executeUpdate("CREATE TABLE IF NOT EXISTS loot_container_states (linked_loot_id CHAR(36) NOT NULL PRIMARY KEY, updated_at BIGINT NOT NULL)");
                statement.executeUpdate("CREATE TABLE IF NOT EXISTS loot_container_contents (linked_loot_id CHAR(36) NOT NULL, slot INTEGER NOT NULL, item_data TEXT NOT NULL, updated_at BIGINT NOT NULL, PRIMARY KEY (linked_loot_id, slot))");
            }
        } catch (ClassNotFoundException | SQLException exception) {
            plugin.getLogger().warning("SQLite is unavailable; using YAML container persistence: " + exception.getMessage());
            connection = null;
        }
    }

    public boolean isAvailable() {
        try {
            return connection != null && !connection.isClosed();
        } catch (SQLException exception) {
            return false;
        }
    }

    public Connection connection() {
        return connection;
    }

    public void close() {
        if (!isAvailable()) return;
        try {
            connection.close();
        } catch (SQLException exception) {
            plugin.getLogger().warning("Could not close SQLite database: " + exception.getMessage());
        }
    }
}