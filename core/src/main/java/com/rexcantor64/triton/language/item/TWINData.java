package com.rexcantor64.triton.language.item;

import lombok.Data;

import java.util.UUID;

@Data
public class TWINData {
    private UUID id;
    private long dateCreated;
    private long dateUpdated;
    private boolean archived;
    private String[] tags;

    /**
     * Returns true if something changed, otherwise returns false
     */
    public boolean ensureValid() {
        if (id != null && dateCreated > 0 && dateUpdated > 0 && tags != null) return false;
        if (id == null) id = UUID.randomUUID();
        if (dateCreated <= 0) dateCreated = System.currentTimeMillis();
        if (dateUpdated <= 0) dateUpdated = System.currentTimeMillis();
        if (tags == null) tags = new String[0];
        return true;
    }
}
