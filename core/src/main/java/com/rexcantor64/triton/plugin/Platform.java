package com.rexcantor64.triton.plugin;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
@Getter
public enum Platform {
    SPIGOT(false), BUNGEE(true), VELOCITY(true);

    private final boolean proxy;
}
