package com.rexcantor64.triton.storage;

import com.rexcantor64.triton.api.language.Language;
import com.rexcantor64.triton.language.item.Collection;
import com.rexcantor64.triton.language.item.LanguageItem;
import com.rexcantor64.triton.language.item.LanguageSign;
import com.rexcantor64.triton.language.item.SignLocation;
import com.rexcantor64.triton.player.LanguagePlayer;
import lombok.Getter;
import lombok.Setter;
import lombok.val;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

public abstract class Storage {

    @Getter
    @Setter
    protected ConcurrentHashMap<String, Collection> collections = new ConcurrentHashMap<>();

    public abstract Language getLanguageFromIp(String ip);

    public abstract Language getLanguage(LanguagePlayer lp);

    public abstract void setLanguage(@Nullable UUID uuid, @Nullable String ip, @NotNull Language newLanguage);

    public abstract void load();

    public abstract boolean uploadToStorage(ConcurrentHashMap<String, Collection> collections);

    public abstract boolean uploadPartiallyToStorage(ConcurrentHashMap<String, Collection> collections,
                                                     List<LanguageItem> changed, List<LanguageItem> deleted);

    public abstract ConcurrentHashMap<String, Collection> downloadFromStorage();

    // If key is null, it's a remove action; otherwise, it's an add action
    public List<LanguageItem> toggleLocationForSignGroup(SignLocation location, String key) {
        val changed = new ArrayList<LanguageItem>();
        for (val collection : collections.values()) {
            for (val item : collection.getItems()) {
                if (!(item instanceof LanguageSign)) continue;

                val sign = (LanguageSign) item;

                // Remove locations for a sign group
                if (key == null && sign.getLocations() != null) {
                    val newLocations = sign.getLocations().stream()
                            .filter(loc -> location.getServer() == null ?
                                    !loc.equals(location) :
                                    !loc.equalsWithServer(location))
                            .collect(Collectors.toList());

                    if (newLocations.size() != sign.getLocations().size())
                        changed.add(sign);

                    sign.setLocations(newLocations);
                }

                if (sign.getKey().equals(key)) {
                    if (sign.hasLocation(location, location.getServer() != null)) continue;
                    if (sign.getLocations() == null) sign.setLocations(new ArrayList<>());
                    sign.getLocations().add(location);
                    changed.add(sign);
                }
            }
        }
        return changed;
    }

}
