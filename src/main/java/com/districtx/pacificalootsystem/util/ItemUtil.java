package com.districtx.pacificalootsystem.util;

import com.cryptomorin.xseries.XMaterial;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.List;

public final class ItemUtil {
    private ItemUtil() { }
    public static ItemStack icon(String material, String name, List<String> lore) {
        ItemStack item = XMaterial.matchXMaterial(material).map(XMaterial::parseItem)
                .orElseGet(() -> XMaterial.matchXMaterial("STONE").map(XMaterial::parseItem).orElseThrow());
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName(TextUtil.color(name)); meta.setLore(TextUtil.color(lore)); item.setItemMeta(meta);
        return item;
    }
    public static ItemStack preview(ItemStack item, String name, List<String> lore) {
        ItemStack result = item == null ? icon("PAPER", name, lore) : item.clone();
        ItemMeta meta = result.getItemMeta();
        if (name != null && !name.isBlank()) meta.setDisplayName(TextUtil.color(name));
        meta.setLore(TextUtil.color(lore)); result.setItemMeta(meta); return result;
    }
}