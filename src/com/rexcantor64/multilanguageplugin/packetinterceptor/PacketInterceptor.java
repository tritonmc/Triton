package com.rexcantor64.multilanguageplugin.packetinterceptor;

import com.rexcantor64.multilanguageplugin.player.SpigotLanguagePlayer;

import java.util.UUID;

public interface PacketInterceptor {

    void refreshSigns(SpigotLanguagePlayer player);

    void refreshEntities(SpigotLanguagePlayer player);

    void refreshTabHeaderFooter(SpigotLanguagePlayer player, String header, String footer);

    void refreshBossbar(SpigotLanguagePlayer player, UUID uuid, String text);

}
