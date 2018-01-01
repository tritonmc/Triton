package com.rexcantor64.multilanguageplugin;

import com.rexcantor64.multilanguageplugin.config.LanguageConfig;
import com.rexcantor64.multilanguageplugin.config.MainConfig;
import com.rexcantor64.multilanguageplugin.config.interfaces.Configuration;
import com.rexcantor64.multilanguageplugin.guiapi.GuiManager;
import com.rexcantor64.multilanguageplugin.language.LanguageManager;
import com.rexcantor64.multilanguageplugin.language.LanguageParser;
import com.rexcantor64.multilanguageplugin.player.PlayerManager;
import com.rexcantor64.multilanguageplugin.web.GistManager;

import java.io.File;
import java.util.List;

public interface MultiLanguagePlugin {

    void reload();

    MainConfig getConf();

    LanguageConfig getLanguageConfig();

    LanguageManager getLanguageManager();

    LanguageParser getLanguageParser();

    PlayerManager getPlayerManager();

    GuiManager getGuiManager();

    GistManager getGistManager();

    String getMessage(String code, String def, Object... args);

    List<String> getMessageList(String code, String... def);

    File getLanguageFolder();

    void logInfo(String info, Object... arguments);

    void logWarning(String warning, Object... arguments);

    void logError(String error, Object... arguments);

    void logDebug(String info, Object... arguments);

    void logDebugWarning(String warning, Object... arguments);

    File getDataFolder();

    Configuration getConfiguration();

    void saveResource(String fileName, boolean override);

}
