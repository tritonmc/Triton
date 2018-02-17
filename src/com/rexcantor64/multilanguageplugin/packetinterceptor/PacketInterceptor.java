package com.rexcantor64.multilanguageplugin.packetinterceptor;

import com.rexcantor64.multilanguageplugin.player.LanguagePlayer;

import java.util.UUID;

public interface PacketInterceptor {

    void refreshSigns(LanguagePlayer player);

    void refreshEntities(LanguagePlayer player);

    void refreshTabHeaderFooter(LanguagePlayer player, String header, String footer);

    void refreshBossbar(LanguagePlayer player, UUID uuid, String text);

}
