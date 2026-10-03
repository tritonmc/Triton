package com.rexcantor64.triton.spigot.wrappers;

import lombok.AllArgsConstructor;

@AllArgsConstructor
public final class WrappedPositionedAdvancement {
    public WrappedAdvancementHolder advancement;
    public float x;
    public float y;

    public WrappedPositionedAdvancement() {
        this(null, 0.0f, 0.0f);
    }
}
