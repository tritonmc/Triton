package com.rexcantor64.triton.utils;

import org.jetbrains.annotations.Nullable;

import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.SocketAddress;

public class SocketUtils {

    public static @Nullable String getIpAddress(@Nullable SocketAddress address) {
        if (address instanceof InetSocketAddress) {
            InetAddress addr = ((InetSocketAddress) address).getAddress();
            if (addr == null) {
                return null;
            }
            return addr.getHostAddress();
        }

        return null;
    }

}
