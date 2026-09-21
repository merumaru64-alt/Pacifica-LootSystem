package com.districtx.pacificalootsystem.loot;

import com.districtx.pacificalootsystem.PacificaLootSystem;
import com.districtx.pacificalootsystem.api.LinkedLoot;
import com.districtx.pacificalootsystem.api.LootContainerState;
import com.districtx.pacificalootsystem.api.LootContainerType;
import com.districtx.pacificalootsystem.api.LootContainerService;
import com.districtx.pacificalootsystem.api.event.LootContainerCloseEvent;
import com.districtx.pacificalootsystem.api.event.LootContainerOpenEvent;
import com.districtx.pacificalootsystem.api.event.LootContainerRestockEvent;
import com.districtx.pacificalootsystem.api.event.LootGeneratedEvent;
import com.districtx.pacificalootsystem.api.event.LootItemAutoPickupEvent;
import com.districtx.pacificalootsystem.api.event.LootItemTakeEvent;
import com.districtx.pacificalootsystem.api.LootContext;
import com.districtx.pacificalootsystem.api.LootResult;
import com.districtx.pacificalootsystem.api.LootTable;
import com.districtx.pacificalootsystem.api.PhysicalLootSession;
import com.districtx.pacificalootsystem.gui.PhysicalLootGUI;
import com.districtx.pacificalootsystem.util.TextUtil;
import com.districtx.pacificalootsystem.service.BukkitLootContainerSoundService;
import com.districtx.pacificalootsystem.service.LootContainerSoundService;
import org.bukkit.Bukkit;
import org.bukkit.block.Lidded;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.event.player.PlayerKickEvent;
import org.bukkit.event.player.PlayerChangedWorldEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class PhysicalLootService implements Listener, LootContainerService {
    private final PacificaLootSystem plugin;
    private final Map<UUID, PhysicalLootSession> sessions = new ConcurrentHashMap<>();
    private final Map<UUID, PhysicalLootGUI> openGuis = new ConcurrentHashMap<>();
    private final Map<UUID, UUID> linkedSessions = new ConcurrentHashMap<>();
    private final LootContainerSoundService soundService = new BukkitLootContainerSoundService();
    private final int restockTaskId;

    public PhysicalLootService(PacificaLootSystem plugin) {
        this.plugin = plugin;
        restockTaskId = Bukkit.getScheduler().runTaskTimer(plugin, this::clearExpiredContainers, 20L, 20L).getTaskId();
    }

    public void shutdown() {
        Bukkit.getScheduler().cancelTask(restockTaskId);
        for (UUID playerId : new ArrayList<>(sessions.keySet())) {
            Player player = plugin.getServer().getPlayer(playerId);
            if (player != null) saveAndRemove(player, openGuis.get(playerId));
            else {
                PhysicalLootSession session = sessions.get(playerId);
                if (session != null) saveSession(session);
                if (session != null) closeContainer(session.getLinkedLootId(), null);
            }
        }
        sessions.clear();
        openGuis.clear();
        linkedSessions.clear();
    }

    public boolean open(Player player, LinkedLoot linkedLoot) {
        if (player == null || linkedLoot == null || sessions.containsKey(player.getUniqueId())) return false;
        if (!player.hasPermission("pacifica.lootsystem.use")) return false;
        UUID linkedId = linkedLoot.getId();
        if (linkedSessions.putIfAbsent(linkedId, player.getUniqueId()) != null) {
            player.sendMessage("§cThis loot container is currently being accessed.");
            return false;
        }

        LootTable table = table(linkedLoot.getLootTableId());
        if (table == null || !table.isEnabled()) {
            linkedSessions.remove(linkedId, player.getUniqueId());
            return false;
        }

        LootContainerOpenEvent openEvent = new LootContainerOpenEvent(player, linkedLoot);
        plugin.getApi().getEventService().call(openEvent);
        if (openEvent.isCancelled()) {
            linkedSessions.remove(linkedId, player.getUniqueId());
            return false;
        }

        LootContainerState state = plugin.getLinkManager().getState(linkedId);
        boolean generated = false;
        if (!plugin.getLinkManager().isOnCooldown(linkedId)) {
            LootContainerRestockEvent restockEvent = new LootContainerRestockEvent(player, linkedLoot);
            plugin.getApi().getEventService().call(restockEvent);
            if (restockEvent.isCancelled()) {
                linkedSessions.remove(linkedId, player.getUniqueId());
                return false;
            }
            state.getContents().clear();
            plugin.getLinkManager().saveState(linkedId, state);
            LootResult result = plugin.getLootGenerator().generate(new LootContext(player, null, linkedLoot.getLocation(),
                player.getInventory().getItemInMainHand(), table, "PHYSICAL"));
            if (!result.isSuccessful()) {
                linkedSessions.remove(linkedId, player.getUniqueId());
                return false;
            }
            plugin.getApi().getEventService().call(new LootGeneratedEvent(player, table, result));
            List<ItemStack> containerItems = prepareItemsForContainer(player, result, linkedLoot);
            Map<Integer, ItemStack> generatedContents = randomizedContents(containerItems);
            state.setContents(generatedContents);
            plugin.getLinkManager().saveState(linkedId, state);
            if (generatedContents.size() < containerItems.size()) {
                plugin.getLootService().deliverItems(player, containerItems.subList(generatedContents.size(), containerItems.size()));
            }
            plugin.getLootService().deliverNonItemRewards(player, result, table);
            plugin.getLinkManager().startCooldown(linkedId, table.getCooldownSeconds());
            generated = true;
        }

        PhysicalLootSession session = new PhysicalLootSession(UUID.randomUUID(), player.getUniqueId(), linkedId,
            table.getUuid(), state.getContents(), generated, System.currentTimeMillis());
        sessions.put(player.getUniqueId(), session);
        PhysicalLootGUI gui = new PhysicalLootGUI(plugin, this, session, title(table));
        openGuis.put(player.getUniqueId(), gui);
        player.openInventory(gui.getInventory());
        openContainer(linkedLoot, player);
        return true;
    }

    public boolean open(Player player, LootTable table, LootResult result) {
        if (player == null || table == null || result == null || sessions.containsKey(player.getUniqueId())) return false;
        plugin.getApi().getEventService().call(new LootGeneratedEvent(player, table, result));
        List<ItemStack> containerItems = prepareItemsForContainer(player, result, null);
        Map<Integer, ItemStack> contents = randomizedContents(containerItems);
        if (contents.isEmpty() && containerItems.isEmpty()) return true;
        PhysicalLootSession session = new PhysicalLootSession(UUID.randomUUID(), player.getUniqueId(), null,
            table.getUuid(), contents, true, System.currentTimeMillis());
        sessions.put(player.getUniqueId(), session);
        PhysicalLootGUI gui = new PhysicalLootGUI(plugin, this, session, title(table));
        openGuis.put(player.getUniqueId(), gui);
        player.openInventory(gui.getInventory());
        return true;
    }

    private List<ItemStack> prepareItemsForContainer(Player player, LootResult result, LinkedLoot linkedLoot) {
        List<ItemStack> containerItems = new ArrayList<>();
        for (int index = 0; index < result.getItems().size(); index++) {
            ItemStack item = result.getItems().get(index);
            if (item == null || item.getType().isAir()) continue;
            if (!result.isAutoPickup(index)) {
                containerItems.add(item.clone());
                continue;
            }
            LootItemAutoPickupEvent autoPickupEvent = new LootItemAutoPickupEvent(player, linkedLoot, item.clone());
            plugin.getApi().getEventService().call(autoPickupEvent);
            if (autoPickupEvent.isCancelled()) {
                containerItems.add(item.clone());
                continue;
            }
            Map<Integer, ItemStack> leftovers = player.getInventory().addItem(item.clone());
            containerItems.addAll(leftovers.values());
        }
        return containerItems;
    }

    @EventHandler
    public void onClick(InventoryClickEvent event) {
        if (event.getView().getTopInventory().getHolder() instanceof PhysicalLootGUI gui
            && event.getWhoClicked() instanceof Player player) {
            int rawSlot = event.getRawSlot();
            if (rawSlot >= 0 && rawSlot < event.getView().getTopInventory().getSize()) {
                ItemStack item = event.getView().getTopInventory().getItem(rawSlot);
                if (item != null && !item.getType().isAir()) {
                    PhysicalLootSession session = sessions.get(player.getUniqueId());
                    LinkedLoot linkedLoot = session == null || session.getLinkedLootId() == null ? null
                        : plugin.getLinkManager().findById(session.getLinkedLootId()).orElse(null);
                    LootItemTakeEvent takeEvent = new LootItemTakeEvent(player, linkedLoot, item.clone());
                    plugin.getApi().getEventService().call(takeEvent);
                    if (takeEvent.isCancelled()) {
                        event.setCancelled(true);
                        return;
                    }
                }
            }
            queueSave(player, gui);
        }
    }

    @EventHandler
    public void onDrag(InventoryDragEvent event) {
        if (event.getView().getTopInventory().getHolder() instanceof PhysicalLootGUI gui
            && event.getWhoClicked() instanceof Player player) queueSave(player, gui);
    }

    @EventHandler
    public void onClose(InventoryCloseEvent event) {
        if (!(event.getInventory().getHolder() instanceof PhysicalLootGUI gui)) return;
        if (event.getPlayer() instanceof Player player) saveAndRemove(player, gui);
    }

    private void queueSave(Player player, PhysicalLootGUI gui) {
        UUID playerId = player.getUniqueId();
        Bukkit.getScheduler().runTask(plugin, () -> {
            PhysicalLootSession session = sessions.get(playerId);
            if (session != null && openGuis.get(playerId) == gui) syncAndSave(session, gui);
        });
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        saveAndRemove(event.getPlayer(), openGuis.get(event.getPlayer().getUniqueId()));
    }

    @EventHandler
    public void onKick(PlayerKickEvent event) {
        saveAndRemove(event.getPlayer(), openGuis.get(event.getPlayer().getUniqueId()));
    }

    @EventHandler
    public void onWorldChange(PlayerChangedWorldEvent event) {
        saveAndRemove(event.getPlayer(), openGuis.get(event.getPlayer().getUniqueId()));
    }

    private void saveAndRemove(Player player, PhysicalLootGUI gui) {
        if (player == null) return;
        PhysicalLootSession session = sessions.remove(player.getUniqueId());
        if (session == null) return;
        if (gui != null) syncAndSave(session, gui);
        else saveSession(session);
        LinkedLoot linkedLoot = session.getLinkedLootId() == null ? null
            : plugin.getLinkManager().findById(session.getLinkedLootId()).orElse(null);
        plugin.getApi().getEventService().call(new LootContainerCloseEvent(player, linkedLoot));
        closeContainer(session.getLinkedLootId(), player);
        openGuis.remove(player.getUniqueId());
        if (session.getLinkedLootId() != null) linkedSessions.remove(session.getLinkedLootId(), player.getUniqueId());
        if (session.getLinkedLootId() == null && gui != null) {
            for (ItemStack item : session.getContents().values()) {
                Map<Integer, ItemStack> leftovers = player.getInventory().addItem(item.clone());
                leftovers.values().forEach(leftover -> player.getWorld().dropItemNaturally(player.getLocation(), leftover));
            }
        }
    }

    private void syncAndSave(PhysicalLootSession session, PhysicalLootGUI gui) {
        session.getContents().clear();
        for (int slot = 0; slot < gui.getInventory().getSize(); slot++) {
            ItemStack item = gui.getInventory().getItem(slot);
            if (item != null && !item.getType().isAir()) session.getContents().put(slot, item.clone());
        }
        saveSession(session);
    }

    private void saveSession(PhysicalLootSession session) {
        if (session == null || session.getLinkedLootId() == null) return;
        plugin.getLinkManager().saveState(session.getLinkedLootId(),
            new LootContainerState(session.getLinkedLootId(), session.getContents(), System.currentTimeMillis()));
    }

    private void openContainer(LinkedLoot linkedLoot, Player player) {
        LootContainerType type = LootContainerType.from(linkedLoot.getBlockType());
        if (type == null || linkedLoot.getBlock() == null) return;
        if (linkedLoot.getBlock().getState() instanceof Lidded lidded) lidded.open();
        soundService.playOpenSound(player, type);
    }

    private void closeContainer(UUID linkedLootId, Player player) {
        if (linkedLootId == null) return;
        LinkedLoot linkedLoot = plugin.getLinkManager().findById(linkedLootId).orElse(null);
        if (linkedLoot == null) return;
        LootContainerType type = LootContainerType.from(linkedLoot.getBlockType());
        if (type == null || linkedLoot.getBlock() == null) return;
        if (linkedLoot.getBlock().getState() instanceof Lidded lidded) lidded.close();
        if (player != null) soundService.playCloseSound(player, type);
    }

    private void clearExpiredContainers() {
        for (LinkedLoot linkedLoot : plugin.getLinkManager().getAllLinkedLoot()) {
            UUID linkedId = linkedLoot.getId();
            if (plugin.getLinkManager().isOnCooldown(linkedId) || linkedSessions.containsKey(linkedId)) continue;
            LootContainerState state = plugin.getLinkManager().getState(linkedId);
            if (state.isEmpty()) continue;
            state.getContents().clear();
            plugin.getLinkManager().saveState(linkedId, state);
        }
    }

    private Map<Integer, ItemStack> randomizedContents(List<ItemStack> items) {
        List<Integer> slots = new ArrayList<>();
        List<Integer> configured = plugin.getGui().getIntegerList("physical-loot.allowed-slots");
        for (int slot = 0; slot < 27; slot++) if (configured.isEmpty() || configured.contains(slot)) slots.add(slot);
        Collections.shuffle(slots);
        Map<Integer, ItemStack> contents = new LinkedHashMap<>();
        int count = Math.min(slots.size(), items.size());
        for (int index = 0; index < count; index++) contents.put(slots.get(index), items.get(index).clone());
        return contents;
    }

    private LootTable table(UUID tableId) {
        return plugin.getTableManager().all().stream().filter(value -> value.getUuid().equals(tableId)).findFirst().orElse(null);
    }

    private String title(LootTable table) {
        String configured = plugin.getGui().getString("physical-loot.title", "&6&l%loot-name% Loot");
        String name = table.getDisplayName() == null ? table.getId() : table.getDisplayName();
        return TextUtil.color(configured.replace("%loot-name%", name));
    }

    public String formatTime(long seconds) {
        long minutes = seconds / 60;
        long remaining = seconds % 60;
        List<String> parts = new ArrayList<>();
        if (minutes > 0) parts.add(minutes + (minutes == 1 ? " minute" : " minutes"));
        if (remaining > 0) parts.add(remaining + (remaining == 1 ? " second" : " seconds"));
        return parts.isEmpty() ? "0 seconds" : String.join(" and ", parts);
    }
}