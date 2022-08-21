package com.rexcantor64.triton.utils;

import org.jetbrains.annotations.Nullable;

import java.net.InetSocketAddress;
import java.net.SocketAddress;

public class SocketUtils {

    public static @Nullable String getIpAddress(SocketAddress address) {
        if (address instanceof InetSocketAddress) {
            return ((InetSocketAddress) address).getAddress().getHostAddress();
        }

        return null;
    }

}
