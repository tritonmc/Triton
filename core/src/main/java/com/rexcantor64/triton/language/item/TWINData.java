package com.rexcantor64.triton.language.item;

import lombok.Data;

import java.util.UUID;

@Data
public class TWINData {
    private long dateCreated;
    private UUID twinId;
    private long dateUpdated;
    private boolean archived;
    private String[] tags;
}
