package com.rexcantor64.triton.language.item;

import lombok.Data;

import java.util.ArrayList;
import java.util.List;

@Data
public class Collection {
    private CollectionMetadata metadata = new CollectionMetadata();
    private List<LanguageItem> items = new ArrayList<>();

    @Data
    public static class CollectionMetadata {
        private boolean blacklist = true;
        private List<String> servers = new ArrayList<>();
    }
}


