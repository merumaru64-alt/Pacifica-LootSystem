package com.districtx.pacificalootsystem;

import com.districtx.pacificalootsystem.api.LootSystemAPI;
import com.districtx.pacificalootsystem.api.PacificaLootAPI;
import com.districtx.pacificalootsystem.command.LootCommand;
import com.districtx.pacificalootsystem.listener.FishingListener;
import com.districtx.pacificalootsystem.listener.GUIListener;
import com.districtx.pacificalootsystem.listener.LootBagListener;
import com.districtx.pacificalootsystem.listener.LootContainerListener;
import com.districtx.pacificalootsystem.listener.MobLootListener;
import com.districtx.pacificalootsystem.loot.LootCooldownManager;
import com.districtx.pacificalootsystem.loot.LootGenerator;
import com.districtx.pacificalootsystem.loot.LootService;
import com.districtx.pacificalootsystem.loot.PhysicalLootService;
import com.districtx.pacificalootsystem.loot.LootBagService;
import com.districtx.pacificalootsystem.loot.LootTableManager;
import com.districtx.pacificalootsystem.api.EconomyService;
import com.districtx.pacificalootsystem.economy.VaultEconomyService;
import com.districtx.pacificalootsystem.link.LootLinkManager;
import com.districtx.pacificalootsystem.link.LootHologramManager;
import org.bukkit.Bukkit;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerChatEvent;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.plugin.ServicePriority;

import java.io.File;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;

public final class PacificaLootSystem extends JavaPlugin implements Listener {
    private LootTableManager tableManager;
    private LootGenerator lootGenerator;
    private LootCooldownManager cooldownManager;
    private LootService lootService;
    private PhysicalLootService physicalLootService;
    private LootBagService lootBagService;
    private EconomyService economyService;
    private LootLinkManager linkManager;
    private LootHologramManager hologramManager;
    private LootSystemAPI api;
    private FileConfiguration messages;
    private FileConfiguration gui;
    private final Map<UUID, Consumer<String>> inputs = new ConcurrentHashMap<>();

    @Override public void onEnable() {
        saveDefaultConfig();
        if (!new File(getDataFolder(), "loot-tables.yml").exists()) saveResource("loot-tables.yml", false);
        if (!new File(getDataFolder(), "links.yml").exists()) saveResource("links.yml", false);
        if (!new File(getDataFolder(), "messages.yml").exists()) saveResource("messages.yml", false);
        if (!new File(getDataFolder(), "gui.yml").exists()) saveResource("gui.yml", false);
        messages = YamlConfiguration.loadConfiguration(new File(getDataFolder(), "messages.yml"));
        gui = YamlConfiguration.loadConfiguration(new File(getDataFolder(), "gui.yml"));
        tableManager = new LootTableManager(this); tableManager.load();
        lootGenerator = new LootGenerator(this); cooldownManager = new LootCooldownManager(this); cooldownManager.load(); economyService = new VaultEconomyService(this); physicalLootService = new PhysicalLootService(this); lootBagService = new LootBagService(this); lootService = new LootService(this);
        linkManager = new LootLinkManager(this); linkManager.load(); hologramManager = new LootHologramManager(this); hologramManager.start(); api = new LootSystemAPI(this);
        Bukkit.getServicesManager().register(PacificaLootAPI.class, api, this, ServicePriority.Normal);
        LootCommand lootCommand = new LootCommand(this); getCommand("loot").setExecutor(lootCommand); getCommand("loot").setTabCompleter(lootCommand);
        Bukkit.getPluginManager().registerEvents(new GUIListener(), this); Bukkit.getPluginManager().registerEvents(physicalLootService, this); Bukkit.getPluginManager().registerEvents(new LootContainerListener(this), this); Bukkit.getPluginManager().registerEvents(new LootBagListener(this), this); Bukkit.getPluginManager().registerEvents(new MobLootListener(this), this); Bukkit.getPluginManager().registerEvents(new FishingListener(this), this); Bukkit.getPluginManager().registerEvents(this, this);
        getLogger().info("Pacifica-LootSystem enabled with " + tableManager.all().size() + " loot tables.");
    }
    @Override public void onDisable() { if (api != null) Bukkit.getServicesManager().unregister(PacificaLootAPI.class, api); if (physicalLootService != null) physicalLootService.shutdown(); if (hologramManager != null) hologramManager.stop(); if (tableManager != null) tableManager.save(); if (cooldownManager != null) cooldownManager.save(); if (linkManager != null) { linkManager.save(); linkManager.close(); } }
    public void reloadPlugin() { reloadConfig(); messages = YamlConfiguration.loadConfiguration(new File(getDataFolder(), "messages.yml")); gui = YamlConfiguration.loadConfiguration(new File(getDataFolder(), "gui.yml")); tableManager.load(); linkManager.load(); hologramManager.refresh(); }
    public LootTableManager getTableManager() { return tableManager; }
    public LootGenerator getLootGenerator() { return lootGenerator; }
    public LootCooldownManager getCooldownManager() { return cooldownManager; }
    public LootService getLootService() { return lootService; }
    public PhysicalLootService getPhysicalLootService() { return physicalLootService; }
    public LootBagService getLootBagService() { return lootBagService; }
    public EconomyService getEconomyService() { return economyService; }
    public LootLinkManager getLinkManager() { return linkManager; }
    public com.districtx.pacificalootsystem.api.LinkedLootService getLinkedLootService() { return linkManager; }
    public LootHologramManager getHologramManager() { return hologramManager; }
    public LootSystemAPI getApi() { return api; }
    public FileConfiguration getMessages() { return messages; }
    public FileConfiguration getGui() { return gui; }
    public void requestInput(Player player, String prompt, Consumer<String> consumer) { inputs.put(player.getUniqueId(), consumer); player.sendMessage("§e" + prompt + " §7(Type cancel to abort)"); }
    @EventHandler public void onChat(AsyncPlayerChatEvent event) { Consumer<String> consumer = inputs.remove(event.getPlayer().getUniqueId()); if (consumer == null) return; event.setCancelled(true); String message = event.getMessage(); if (message.equalsIgnoreCase("cancel")) { event.getPlayer().sendMessage("§eInput cancelled."); return; } Bukkit.getScheduler().runTask(this, () -> consumer.accept(message)); }
}