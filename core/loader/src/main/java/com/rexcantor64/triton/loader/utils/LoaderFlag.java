package com.rexcantor64.triton.loader.utils;

public enum LoaderFlag {
    /** Vendor the Adventure core and examination API */
    VENDOR_ADVENTURE,
    /** Vendor Minimessage, Kyori option and all Adventure serializers EXCEPT the BungeeCord one */
    VENDOR_ADVENTURE_SERIALIZERS,
    /** Vendor the Bungee Serializer */
    VENDOR_ADVENTURE_BUNGEE_SERIALIZER,
    /** Vendor packetevents */
    VENDOR_PACKET_EVENTS,
}
