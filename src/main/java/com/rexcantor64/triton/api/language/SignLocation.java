package com.rexcantor64.triton.api.language;

/**
 * Represents the location of a sign
 *
 * @since 1.0.0
 */
public interface SignLocation {

    /**
     * Bungee only.
     *
     * @return Get the server where this sign is.
     * @since 1.0.0
     */
    String getServer();

    /**
     * @return Get the world where the sign is.
     * @since 1.0.0
     */
    String getWorld();

    /**
     * @return Get the X position where the sign is.
     * @since 1.0.0
     */
    int getX();

    /**
     * @return Get the Y position where the sign is.
     * @since 1.0.0
     */
    int getY();

    /**
     * @return Get the Z position where the sign is.
     * @since 1.0.0
     */
    int getZ();

}
