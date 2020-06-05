package com.rexcantor64.triton.language.item;

import lombok.Data;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Data
public abstract class LanguageItem {

    private String key;
    private TWINData twinData = null;

    public abstract LanguageItemType getType();

    @RequiredArgsConstructor
    @Getter
    public enum LanguageItemType {
        TEXT("text"), SIGN("sign");

        private final String name;

    }

}
