package com.rexcantor64.triton.language.localized;

import com.rexcantor64.triton.Triton;
import com.rexcantor64.triton.api.language.Language;
import com.rexcantor64.triton.api.language.Localized;
import lombok.Data;
import lombok.RequiredArgsConstructor;

@Data
@RequiredArgsConstructor
public class StringLocale implements Localized {

    private final String languageId;
    private Language cachedLanguage;

    @Override
    public Language getLanguage() {
        if (cachedLanguage == null) {
            cachedLanguage = Triton.get().getLanguageManager().getLanguageByName(languageId, true);
        }
        return cachedLanguage;
    }
}
