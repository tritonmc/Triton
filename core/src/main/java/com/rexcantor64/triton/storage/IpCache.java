package com.rexcantor64.triton.storage;

import com.rexcantor64.triton.Triton;
import lombok.RequiredArgsConstructor;
import lombok.val;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

public class IpCache {

    private final ConcurrentHashMap<String, CacheEntry> cache = new ConcurrentHashMap<>();

    public void addToCache(String ip, String lang) {
        cache.put(ip, new CacheEntry(lang, System.currentTimeMillis() + TimeUnit.MINUTES.toMillis(5)));
    }

    public String getFromCache(String ip) {
        Triton.get().runAsync(this::clearExpiredEntries);
        val entry = cache.get(ip);
        return entry == null || entry.expiresAt < System.currentTimeMillis() ? null : entry.lang;
    }

    private void clearExpiredEntries() {
        cache.keySet().removeIf(key -> cache.get(key).expiresAt < System.currentTimeMillis());
    }

    @RequiredArgsConstructor
    private static class CacheEntry {
        private final String lang;
        private final long expiresAt;
    }

}
