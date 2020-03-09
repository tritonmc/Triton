package com.rexcantor64.triton.storage;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

public class IpCache {

    private ConcurrentHashMap<String, CacheEntry> cache = new ConcurrentHashMap<>();

    public void addToCache(String ip, String lang) {
        cache.put(ip, new CacheEntry(lang, System.currentTimeMillis() + TimeUnit.MINUTES.toMillis(5)));
    }

    public String getFromCache(String ip) {
        clearExpiredEntries();
        CacheEntry entry = cache.get(ip);
        return entry == null ? null : entry.lang;
    }

    private void clearExpiredEntries() {
        cache.keySet().removeIf(key -> cache.get(key).expiresAt < System.currentTimeMillis());
    }

    private static class CacheEntry {
        private String lang;
        private long expiresAt;

        public CacheEntry(String lang, long expiresAt) {
            this.lang = lang;
            this.expiresAt = expiresAt;
        }
    }

}
