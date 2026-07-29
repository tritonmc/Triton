package com.rexcantor64.triton.api.impl.adventure;

import com.rexcantor64.triton.api.Triton;
import com.rexcantor64.triton.api.config.TritonConfig;
import com.rexcantor64.triton.api.language.LanguageManager;
import com.rexcantor64.triton.api.language.LanguageParser;
import com.rexcantor64.triton.api.language.MessageParser;
import com.rexcantor64.triton.api.language.TranslationManager;
import com.rexcantor64.triton.api.players.LanguagePlayer;
import com.rexcantor64.triton.api.players.PlayerManager;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class TritonImpl implements Triton {

    private final com.rexcantor64.triton.Triton<?, ?> triton;

    @Override
    public TritonConfig getConfig() {
        return triton.getConfig();
    }

    @Override
    public LanguageManager getLanguageManager() {
        return triton.getLanguageManager();
    }

    @Override
    public LanguageParser getLanguageParser() {
        return triton.getLanguageParser();
    }

    @Override
    public TranslationManager getTranslationManager() {
        return new TranslationManagerImpl(triton.getTranslationManager());
    }

    @Override
    public MessageParser getMessageParser() {
        return new MessageParserImpl(triton.getMessageParser());
    }

    @Override
    public PlayerManager getPlayerManager() {
        return triton.getPlayerManager();
    }

    @Override
    public void openLanguagesSelectionGUI(LanguagePlayer player) {
        triton.openLanguagesSelectionGUI(player);
    }

    @Override
    public void reload() {
        triton.reload();
    }
}
