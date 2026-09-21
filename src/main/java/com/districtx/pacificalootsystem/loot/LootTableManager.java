package com.districtx.pacificalootsystem.loot;

import com.districtx.pacificalootsystem.PacificaLootSystem;
import com.districtx.pacificalootsystem.api.LootTable;
import com.districtx.pacificalootsystem.api.LootTableService;
import com.districtx.pacificalootsystem.storage.YamlStorage;

import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;

public final class LootTableManager implements LootTableService {
    private final YamlStorage storage;
    private final Map<String, LootTable> tables = new LinkedHashMap<>();
    public LootTableManager(PacificaLootSystem plugin) { storage = new YamlStorage(plugin); }
    public void load() { tables.clear(); tables.putAll(storage.loadTables()); }
    public void save() { storage.saveTables(tables); }
    public LootTable get(String id) { return id == null ? null : tables.get(id.toLowerCase(Locale.ROOT)); }
    public Collection<LootTable> all() { return tables.values(); }
    public boolean create(String id) { if (get(id) != null) return false; String key = id.toLowerCase(Locale.ROOT); tables.put(key, new LootTable(key, id)); save(); return true; }
    public boolean delete(String id) { if (id == null || tables.remove(id.toLowerCase(Locale.ROOT)) == null) return false; save(); return true; }
    @Override public java.util.Optional<LootTable> getLootTable(String name) { return java.util.Optional.ofNullable(get(name)); }
    @Override public boolean exists(String name) { return get(name) != null; }
    @Override public Collection<LootTable> getLootTables() { return java.util.Collections.unmodifiableCollection(tables.values()); }
    @Override public void createLootTable(String name) { create(name); }
    @Override public boolean deleteLootTable(String name) { return delete(name); }
    @Override public void save(LootTable lootTable) { save(); }
}