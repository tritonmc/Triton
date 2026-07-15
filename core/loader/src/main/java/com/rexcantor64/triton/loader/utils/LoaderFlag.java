package com.rexcantor64.triton.loader.utils;

public enum LoaderFlag {
    /** Vendor the Adventure core */
    VENDOR_ADVENTURE,
    /** Vendor the Adventure NBT API */
    VENDOR_ADVENTURE_NBT,
    /** Vendor Minimessage, Kyori option and all Adventure serializers EXCEPT the BungeeCord one */
    VENDOR_ADVENTURE_SERIALIZERS,
    /** Vendor the Bungee Serializer */
    VENDOR_ADVENTURE_BUNGEE_SERIALIZER,
    /** Vendor packetevents */
    VENDOR_PACKET_EVENTS,
}
