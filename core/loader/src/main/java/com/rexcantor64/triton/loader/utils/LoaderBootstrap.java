package com.rexcantor64.triton.loader.utils;

/**
 * Minimal bootstrap plugin, called by the loader plugin.
 */
public interface LoaderBootstrap {

    default void onEnable() {}

    default void onDisable() {}

}
