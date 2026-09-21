package com.districtx.pacificalootsystem.command;

import com.cryptomorin.xseries.XMaterial;
import com.districtx.pacificalootsystem.PacificaLootSystem;
import com.districtx.pacificalootsystem.api.Collectable;
import com.districtx.pacificalootsystem.api.LinkedLoot;
import com.districtx.pacificalootsystem.api.LootEntry;
import com.districtx.pacificalootsystem.api.MoneyReward;
import com.districtx.pacificalootsystem.api.LootResult;
import com.districtx.pacificalootsystem.api.LootTable;
import com.districtx.pacificalootsystem.gui.ItemEditorGUI;
import com.districtx.pacificalootsystem.gui.LootTableEditorGUI;
import com.districtx.pacificalootsystem.util.ItemUtil;
import com.districtx.pacificalootsystem.util.MessageUtil;
import com.districtx.pacificalootsystem.util.TextUtil;
import org.bukkit.Bukkit;
import org.bukkit.NamespacedKey;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

public final class LootCommand implements CommandExecutor, TabCompleter {
    private final PacificaLootSystem plugin;
    public LootCommand(PacificaLootSystem plugin) { this.plugin = plugin; }
    private boolean admin(CommandSender sender, String permission) { if (sender.hasPermission("pacifica.lootsystem.admin") || sender.hasPermission(permission)) return true; sender.sendMessage("§cYou need " + permission + "."); return false; }
    @Override public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length == 0 || args[0].equalsIgnoreCase("help")) { help(sender); return true; }
        String sub = args[0].toLowerCase(Locale.ROOT);
        if (sub.equals("list")) { sender.sendMessage("§dLoot tables: §f" + plugin.getTableManager().all().stream().map(LootTable::getId).collect(Collectors.joining(", "))); return true; }
        String permission = permissionFor(sub, args);
        if (!admin(sender, permission)) return true;
        switch (sub) {
            case "create" -> { if (args.length < 2) { sender.sendMessage("§c/loot create <id>"); return true; } if (plugin.getTableManager().create(args[1])) MessageUtil.send(plugin, sender, "loot-created", Map.of("loot_table", args[1])); else sender.sendMessage("§cThat loot table already exists."); }
            case "delete" -> { if (args.length < 2) return usage(sender, "/loot delete <id>"); LootTable table = plugin.getTableManager().get(args[1]); if (table != null) plugin.getLinkManager().unlinkAll(table.getUuid()); if (plugin.getTableManager().delete(args[1])) MessageUtil.send(plugin, sender, "loot-deleted", Map.of("loot_table", args[1])); else invalid(sender, args[1]); }
            case "edit" -> { if (!(sender instanceof Player player)) { sender.sendMessage("§cPlayers only."); return true; } if (args.length < 2) return usage(sender, "/loot edit <id>"); LootTable table = plugin.getTableManager().get(args[1]); if (table == null) { invalid(sender, args[1]); return true; } new LootTableEditorGUI(plugin, table).open(player); }
            case "collectable", "collectible", "collection" -> addCollectable(sender, args);
            case "add" -> addItem(sender, args);
            case "addhand" -> addHand(sender, args);
            case "give", "test" -> give(sender, args, sub.equals("test"));
            case "reset" -> reset(sender, args);
            case "resetall" -> { if (args.length < 2) return usage(sender, "/loot resetall <id>"); plugin.getCooldownManager().resetTable(plugin.getTableManager().get(args[1])); sender.sendMessage("§aAll cooldowns reset."); }
            case "link" -> { if (!(sender instanceof Player player)) { sender.sendMessage("§cPlayers only."); return true; } if (args.length < 2 || plugin.getTableManager().get(args[1]) == null) { invalid(sender, args.length < 2 ? "" : args[1]); return true; } plugin.getLinkManager().beginLink(player, args[1]); }
            case "unlink" -> { if (sender instanceof Player player) plugin.getLinkManager().beginUnlink(player); else sender.sendMessage("§cPlayers only."); }
            case "links" -> links(sender, args);
            case "money" -> money(sender, args);
            case "physical" -> physical(sender, args);
            case "bag" -> bag(sender, args);
            case "reload" -> { plugin.reloadPlugin(); MessageUtil.send(plugin, sender, "loot-reloaded"); }
            case "save" -> { plugin.getTableManager().save(); sender.sendMessage("§aLoot tables saved."); }
            case "info" -> info(sender, args);
            default -> help(sender);
        }
        return true;
    }
    private void addCollectable(CommandSender sender, String[] args) {
        if (args.length < 4 || !args[1].equalsIgnoreCase("add")) { usage(sender, "/loot collectable add <loot-name> <collectable-name>"); return; }
        LootTable table = plugin.getTableManager().get(args[2]);
        if (table == null) { sender.sendMessage("§cLoot table not found."); return; }
        String name = args[3].trim();
        if (name.isEmpty()) { sender.sendMessage("§cCollectable name cannot be empty."); return; }
        if (findCollectable(table, name) != null) { sender.sendMessage("§cThat collectable already exists in this loot table."); return; }
        Collectable collectable = new Collectable(name);
        table.getCollectables().put(collectable.getId(), collectable);
        plugin.getTableManager().save();
        MessageUtil.send(plugin, sender, "collectable-created", Map.of("loot_table", table.getId(), "collectable", name));
        if (sender instanceof Player player) new LootTableEditorGUI(plugin, table).open(player);
    }
    private void addItem(CommandSender sender, String[] args) {
        if (!(sender instanceof Player player)) { sender.sendMessage("§cPlayers only."); return; }
        if (args.length < 3) { usage(sender, "/loot add <loot-name> <collectable-name>"); return; }
        LootTable table = plugin.getTableManager().get(args[1]);
        if (table == null) { invalid(sender, args[1]); return; }
        Collectable collectable = findCollectable(table, args[2]);
        if (collectable == null) { missingCollectable(sender, table, args[2]); return; }
        new ItemEditorGUI(plugin, table, collectable, null, player.getInventory().getItemInMainHand()).open(player);
    }
    private void addHand(CommandSender sender, String[] args) {
        if (!(sender instanceof Player player)) { sender.sendMessage("§cPlayers only."); return; }
        if (args.length < 3) { usage(sender, "/loot addhand <loot-name> <collectable-name>"); return; }
        LootTable table = plugin.getTableManager().get(args[1]);
        if (table == null) { invalid(sender, args[1]); return; }
        Collectable collectable = findCollectable(table, args[2]);
        if (collectable == null) { missingCollectable(sender, table, args[2]); return; }
        ItemStack hand = player.getInventory().getItemInMainHand();
        if (hand.getType().isAir()) { sender.sendMessage("§cYou must hold an item in your main hand."); return; }
        LootEntry entry = new LootEntry(hand);
        collectable.getEntries().put(entry.getId(), entry);
        table.getEntries().put(entry.getId(), entry);
        plugin.getTableManager().save();
        MessageUtil.send(plugin, sender, "loot-entry-added", Map.of("loot_table", table.getId()));
    }
    private Collectable findCollectable(LootTable table, String name) {
        if (name == null) return null;
        return table.getCollectables().values().stream().filter(value -> value.getName() != null && value.getName().equalsIgnoreCase(name)).findFirst().orElse(null);
    }
    private void missingCollectable(CommandSender sender, LootTable table, String name) {
        sender.sendMessage("§cCollectable §f" + name + " §cdoes not exist in loot table §f" + table.getDisplayName() + "§c.");
    }
    private String permissionFor(String sub, String[] args) {
        if (sub.equals("link")) return "pacifica.lootsystem.admin.link";
        if (sub.equals("unlink")) return "pacifica.lootsystem.admin.unlink";
        if (sub.equals("links")) return "pacifica.lootsystem.admin.links";
        if (sub.equals("money")) return "pacifica.lootsystem.admin.money";
        if (sub.equals("physical")) return "pacifica.lootsystem.admin.physical";
        if (sub.equals("reload")) return "pacifica.lootsystem.admin.reload";
        if (sub.equals("collectable") || sub.equals("collectible") || sub.equals("collection")) return "pacifica.lootsystem.admin.collectable.add";
        if (sub.equals("add")) return "pacifica.lootsystem.admin.add";
        if (sub.equals("addhand")) return "pacifica.lootsystem.admin.addhand";
        return "pacifica.lootsystem.admin";
    }
    private void give(CommandSender sender, String[] args, boolean test) { if (args.length < 3) { usage(sender, "/loot " + (test ? "test" : "give") + " <player> <id>"); return; } Player player = Bukkit.getPlayerExact(args[1]); if (player == null) { sender.sendMessage("§cPlayer not found."); return; } if (plugin.getTableManager().get(args[2]) == null) { invalid(sender, args[2]); return; } LootResult result = plugin.getLootService().claim(player, args[2], true, test); if (result.hasRewards()) MessageUtil.send(plugin, sender, "loot-given", Map.of("player", player.getName())); else sender.sendMessage("§eNo rewards were selected."); }
    private void reset(CommandSender sender, String[] args) { if (args.length < 3) { usage(sender, "/loot reset <player> <id>"); return; } Player player = Bukkit.getPlayerExact(args[1]); LootTable table = plugin.getTableManager().get(args[2]); if (player == null || table == null) { sender.sendMessage("§cPlayer or loot table not found."); return; } plugin.getCooldownManager().reset(player, table); sender.sendMessage("§aCooldown reset."); }
    private void bag(CommandSender sender, String[] args) { if (!(sender instanceof Player player)) { sender.sendMessage("§cPlayers only."); return; } if (args.length < 2) { usage(sender, "/loot bag <id> [amount]"); return; } if (plugin.getTableManager().get(args[1]) == null) { invalid(sender, args[1]); return; } ItemStack bag = XMaterial.matchXMaterial("CHEST").map(XMaterial::parseItem).orElseThrow(); ItemMeta meta = bag.getItemMeta(); meta.setDisplayName(TextUtil.color("&d&lLoot Bag")); meta.setLore(List.of(TextUtil.color("&7Right-click to open."), TextUtil.color("&7Contains: &f" + args[1]))); meta.getPersistentDataContainer().set(new NamespacedKey(plugin, "loot_table"), PersistentDataType.STRING, args[1]); bag.setItemMeta(meta); if (args.length > 2) try { bag.setAmount(Math.max(1, Math.min(bag.getMaxStackSize(), Integer.parseInt(args[2])))); } catch (NumberFormatException ignored) { } player.getInventory().addItem(bag); }
    private void info(CommandSender sender, String[] args) { if (args.length < 2) { usage(sender, "/loot info <id>"); return; } LootTable table = plugin.getTableManager().get(args[1]); if (table == null) { invalid(sender, args[1]); return; } sender.sendMessage("§d" + table.getId() + " §7(" + table.getSelectionMode() + ") entries=" + table.getEntries().size() + " rewards=" + table.getMinimumRewards() + "-" + table.getMaximumRewards()); }
    private void links(CommandSender sender, String[] args) {
        if (args.length < 2) { usage(sender, "/loot links <id>"); return; }
        LootTable table = plugin.getTableManager().get(args[1]);
        if (table == null) { invalid(sender, args[1]); return; }
        List<LinkedLoot> links = plugin.getLinkManager().getLinkedLoot(table.getUuid());
        sender.sendMessage("§dLoot Table: §f" + table.getDisplayName());
        sender.sendMessage("§dLinked Containers:");
        for (int index = 0; index < links.size(); index++) {
            LinkedLoot link = links.get(index);
            if (link.getLocation() == null) continue;
            sender.sendMessage("§f" + (index + 1) + ". §e" + link.getBlockType() + " §7@ " + link.getLocation().getWorld().getName()
                + " " + link.getX() + "," + link.getY() + "," + link.getZ());
        }
    }
    private void money(CommandSender sender, String[] args) {
        if (args.length >= 2 && args[1].equalsIgnoreCase("remove")) {
            if (args.length < 3) { usage(sender, "/loot money remove <loot-name>"); return; }
            LootTable table = plugin.getTableManager().get(args[2]);
            if (table == null) { invalid(sender, args[2]); return; }
            table.getMoneyRewards().clear();
            plugin.getTableManager().save();
            MessageUtil.send(plugin, sender, "money-removed", Map.of("loot_table", table.getId()));
            return;
        }
        if (args.length < 4) { usage(sender, "/loot money <loot-name> <minimum> <maximum>"); return; }
        LootTable table = plugin.getTableManager().get(args[1]);
        if (table == null) { invalid(sender, args[1]); return; }
        try {
            double minimum = Double.parseDouble(args[2]);
            double maximum = Double.parseDouble(args[3]);
            if (!Double.isFinite(minimum) || !Double.isFinite(maximum) || minimum < 0 || maximum < minimum) throw new NumberFormatException();
            table.getMoneyRewards().clear();
            UUID id = UUID.randomUUID();
            table.getMoneyRewards().put(id, new MoneyReward(id, table.getUuid(), null, minimum, maximum, true));
            plugin.getTableManager().save();
            MessageUtil.send(plugin, sender, "money-set", Map.of("loot_table", table.getId(), "minimum", String.format("%.2f", minimum), "maximum", String.format("%.2f", maximum)));
        } catch (NumberFormatException exception) {
            sender.sendMessage("§cMinimum and maximum must be valid non-negative amounts, with maximum >= minimum.");
        }
    }
    private void physical(CommandSender sender, String[] args) {
        if (args.length < 2) { usage(sender, "/loot physical <loot-name>"); return; }
        LootTable table = plugin.getTableManager().get(args[1]);
        if (table == null) { invalid(sender, args[1]); return; }
        table.setLootMode("PHYSICAL");
        plugin.getTableManager().save();
        MessageUtil.send(plugin, sender, "physical-set", Map.of("loot_table", table.getId()));
    }
    private void invalid(CommandSender sender, String id) { MessageUtil.send(plugin, sender, "loot-invalid", Map.of("loot_table", id)); }
    private boolean usage(CommandSender sender, String value) { sender.sendMessage("§c" + value); return true; }
    private void help(CommandSender sender) { sender.sendMessage("§d/loot link|unlink|links|money|physical|reload"); }
    @Override public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (args.length == 1) return Arrays.asList("help", "create", "delete", "list", "info", "edit", "add", "addhand", "collectable", "collectible", "collection", "give", "test", "reset", "resetall", "link", "unlink", "links", "money", "physical", "bag", "reload", "save").stream()
            .filter(value -> isPublic(value) || canUse(sender, value, args))
            .filter(value -> value.startsWith(args[0].toLowerCase(Locale.ROOT))).toList();
        String sub = args[0].toLowerCase(Locale.ROOT);
        if (!isPublic(sub) && !canUse(sender, sub, args)) return new ArrayList<>();
        if (args.length == 2 && isCollectableCommand(sub)) return List.of("add");
        if (args.length == 2) return plugin.getTableManager().all().stream().map(LootTable::getId).toList();
        if (args.length == 3 && (sub.equals("add") || sub.equals("addhand"))) return collectables(args[1]);
        if (args.length == 3 && isCollectableCommand(sub)) return plugin.getTableManager().all().stream().map(LootTable::getId).toList();
        if (args.length == 4 && isCollectableCommand(sub)) return collectables(args[2]);
        return new ArrayList<>();
    }
    private boolean isPublic(String sub) { return sub.equalsIgnoreCase("help") || sub.equalsIgnoreCase("list"); }
    private boolean isCollectableCommand(String sub) { return sub.equalsIgnoreCase("collectable") || sub.equalsIgnoreCase("collectible") || sub.equalsIgnoreCase("collection"); }
    private boolean canUse(CommandSender sender, String sub, String[] args) { return sender.hasPermission("pacifica.lootsystem.admin") || sender.hasPermission(permissionFor(sub, args)); }
    private List<String> collectables(String id) { LootTable table = plugin.getTableManager().get(id); return table == null ? new ArrayList<>() : table.getCollectables().values().stream().map(Collectable::getName).toList(); }
}