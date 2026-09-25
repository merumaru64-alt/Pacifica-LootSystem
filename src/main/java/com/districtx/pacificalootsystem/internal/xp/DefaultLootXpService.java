package com.districtx.pacificalootsystem.internal.xp;

import com.cryptomorin.xseries.XSound;
import com.districtx.pacificalootsystem.PacificaLootSystem;
import com.districtx.pacificalootsystem.api.LinkedLoot;
import com.districtx.pacificalootsystem.api.LootRankBonusService;
import com.districtx.pacificalootsystem.api.LootTable;
import com.districtx.pacificalootsystem.api.LootXpService;
import com.districtx.pacificalootsystem.api.PacificaCoreXpBridge;
import com.districtx.pacificalootsystem.api.XpRewardResult;
import com.districtx.pacificalootsystem.api.event.LootXpRewardEvent;
import com.districtx.pacificalootsystem.util.TextUtil;
import org.bukkit.entity.Player;

import java.util.Optional;
import java.util.concurrent.ThreadLocalRandom;

public final class DefaultLootXpService implements LootXpService {
    private final PacificaLootSystem plugin;
    private final PacificaCoreXpBridge coreXpBridge;
    private final LootRankBonusService rankBonusService;

    public DefaultLootXpService(PacificaLootSystem plugin, PacificaCoreXpBridge coreXpBridge,
                                LootRankBonusService rankBonusService) {
        this.plugin = plugin;
        this.coreXpBridge = coreXpBridge;
        this.rankBonusService = rankBonusService;
    }

    @Override
    public boolean isAvailable() {
        return plugin.getConfig().getBoolean("xp-rewards.enabled", true) && coreXpBridge.isAvailable();
    }

    @Override
    public int calculateBaseXp(LootTable lootTable) {
        if (lootTable == null) return 0;
        int minimum = Math.max(0, lootTable.getMinimumXpReward());
        int maximum = Math.max(minimum, lootTable.getMaximumXpReward());
        return (int) ThreadLocalRandom.current().nextLong(minimum, (long) maximum + 1L);
    }

    @Override
    public int calculateBonusXp(Player player, int baseXp) {
        return calculateFinalXp(player, baseXp) - Math.max(0, baseXp);
    }

    @Override
    public int calculateFinalXp(Player player, int baseXp) {
        if (baseXp <= 0) return 0;
        double bonus = rankBonusService.getXpBonusPercent(player);
        double calculated = Math.floor(baseXp * (1.0 + bonus));
        return (int) Math.min(Integer.MAX_VALUE, calculated);
    }

    @Override
    public XpRewardResult awardXp(Player player, LinkedLoot linkedLoot, int baseXp) {
        Optional<String> applicableRank = rankBonusService.getApplicableRank(player);
        String rank = applicableRank.orElse("");
        double bonus = rankBonusService.getXpBonusPercent(player);
        if (player == null || linkedLoot == null || baseXp <= 0 || !isAvailable()) {
            return result(false, Math.max(0, baseXp), 0, 0, rank, bonus);
        }

        LinkedLoot transactionLoot = plugin.getLinkManager().findById(linkedLoot.getId()).orElse(null);
        LootTable table = transactionLoot == null ? null : plugin.getTableManager().all().stream()
                .filter(candidate -> candidate.getUuid().equals(transactionLoot.getLootTableId())).findFirst().orElse(null);
        if (transactionLoot == null || table == null) return result(false, baseXp, 0, 0, rank, bonus);

        int finalXp = calculateFinalXp(player, baseXp);
        synchronized (transactionLoot) {
            if (transactionLoot.isXpRewardClaimed()) return result(false, baseXp, 0, 0, rank, bonus);
            transactionLoot.setXpRewardClaimed(true);
            plugin.getLinkManager().save();
        }

        LootXpRewardEvent event = new LootXpRewardEvent(player, transactionLoot, table, baseXp,
                finalXp, rank, bonus);
        plugin.getApi().getEventService().call(event);
        if (event.isCancelled() || event.getFinalXp() <= 0 || !coreXpBridge.awardXp(player, event.getFinalXp())) {
            return result(false, baseXp, 0, 0, rank, bonus);
        }

        int awardedXp = event.getFinalXp();
        String message = plugin.getConfig().getString("xp-rewards.message", "&e&l+ &a&l%xp%");
        if (message != null && !message.isBlank()) {
            player.sendMessage(TextUtil.color(message.replace("%xp%", String.valueOf(awardedXp))));
        }
        if (plugin.getConfig().getBoolean("xp-rewards.sound.enabled", true)) {
            XSound.matchXSound(plugin.getConfig().getString("xp-rewards.sound.name", "ENTITY_EXPERIENCE_ORB_PICKUP"))
                    .ifPresent(sound -> sound.play(player));
        }
        return result(true, baseXp, awardedXp - baseXp, awardedXp, rank, bonus);
    }

    private XpRewardResult result(boolean successful, int baseXp, int bonusXp, int finalXp,
                                 String rank, double bonusPercent) {
        return new LootXpRewardResultImpl(successful, baseXp, bonusXp, finalXp, rank, bonusPercent);
    }
}