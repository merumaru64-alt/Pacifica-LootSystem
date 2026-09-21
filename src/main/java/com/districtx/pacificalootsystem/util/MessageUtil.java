package com.districtx.pacificalootsystem.util;

import com.districtx.pacificalootsystem.PacificaLootSystem;
import org.bukkit.command.CommandSender;

import java.util.Map;

public final class MessageUtil {
    private MessageUtil() { }
    public static void send(PacificaLootSystem plugin, CommandSender sender, String key, Map<String, String> values) {
        String message = plugin.getMessages().getString(key, key);
        String prefix = plugin.getMessages().getString("prefix", "");
        boolean hasPrefixPlaceholder = message.contains("%prefix%");
        message = message.replace("%prefix%", "");
        for (Map.Entry<String, String> value : values.entrySet()) message = message.replace("%" + value.getKey() + "%", value.getValue());
        sender.sendMessage(TextUtil.color((hasPrefixPlaceholder ? "" : prefix) + message));
    }
    public static void send(PacificaLootSystem plugin, CommandSender sender, String key) { send(plugin, sender, key, Map.of()); }
}