package com.districtx.pacificalootsystem.util;

import org.bukkit.ChatColor;

import java.util.ArrayList;
import java.util.List;

public final class TextUtil {
    private TextUtil() { }
    public static String color(String text) { return ChatColor.translateAlternateColorCodes('&', text == null ? "" : text); }
    public static List<String> color(List<String> lines) {
        List<String> result = new ArrayList<>();
        if (lines != null) for (String line : lines) result.add(color(line));
        return result;
    }
}