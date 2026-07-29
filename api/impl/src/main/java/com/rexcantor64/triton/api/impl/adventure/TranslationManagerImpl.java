package com.rexcantor64.triton.api.impl.adventure;

import com.rexcantor64.triton.api.impl.ComponentUtils;
import com.rexcantor64.triton.api.language.Localized;
import com.rexcantor64.triton.api.language.SignLocation;
import com.rexcantor64.triton.api.language.TranslationManager;
import lombok.RequiredArgsConstructor;
import net.kyori.adventure.text.Component;
import org.jetbrains.annotations.NotNullByDefault;

import java.util.Optional;
import java.util.function.Supplier;

@RequiredArgsConstructor
@NotNullByDefault
public class TranslationManagerImpl implements TranslationManager {

    private final com.rexcantor64.triton.language.TranslationManager manager;


    @Override
    public Component getTextComponentOr404(Localized locale, String key, Component... arguments) {
        return ComponentUtils.deserializeFromJsonTree(manager.getTextComponentOr404Json(locale, key, ComponentUtils.serializeToJsonTree(arguments)));
    }

    @Override
    public Optional<Component> getTextComponent(Localized locale, String key, Component... arguments) {
        return manager.getTextComponentJson(locale, key, ComponentUtils.serializeToJsonTree(arguments))
                .map(ComponentUtils::deserializeFromJsonTree);
    }

    @Override
    public Optional<String> getTextString(Localized locale, String key) {
        return manager.getTextString(locale, key);
    }

    @Override
    public Optional<Component[]> getSignComponents(Localized locale, SignLocation location) {
        return manager.getSignComponentsJson(locale, location).map(ComponentUtils::deserializeFromJsonTree);
    }

    @Override
    public Optional<Component[]> getSignComponents(Localized locale, SignLocation location, Component[] defaultLines) {
        return manager.getSignComponentsJson(locale, location, ComponentUtils.serializeToJsonTree(defaultLines))
                .map(ComponentUtils::deserializeFromJsonTree);
    }

    @Override
    public Optional<Component[]> getSignComponents(Localized locale, SignLocation location, Supplier<Component[]> defaultLinesSupplier) {
        return manager.getSignComponentsJson(locale, location, () -> ComponentUtils.serializeToJsonTree(defaultLinesSupplier.get()))
                .map(ComponentUtils::deserializeFromJsonTree);
    }
}
