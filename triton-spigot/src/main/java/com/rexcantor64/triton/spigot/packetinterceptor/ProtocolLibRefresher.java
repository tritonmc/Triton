package com.rexcantor64.triton.spigot.packetinterceptor;

import com.rexcantor64.triton.language.item.SignLocation;
import com.rexcantor64.triton.spigot.player.SpigotLanguagePlayer;
import net.kyori.adventure.text.Component;
import org.bukkit.entity.Player;

import java.util.UUID;

public interface ProtocolLibRefresher {

    void refreshSigns(SpigotLanguagePlayer player);

    void refreshEntities(SpigotLanguagePlayer player);

    void refreshTabHeaderFooter(SpigotLanguagePlayer player, Component header, Component footer);

    void refreshBossbar(SpigotLanguagePlayer player, UUID uuid, String json);

    void refreshScoreboard(SpigotLanguagePlayer player);

    void refreshAdvancements(SpigotLanguagePlayer languagePlayer);

    void resetSign(Player p, SignLocation location);
}
