package com.rexcantor64.triton.utils;

import io.netty.channel.unix.DomainSocketAddress;

import java.net.InetSocketAddress;
import java.net.SocketAddress;

public class SocketUtils {

    public static String getIpAddress(SocketAddress address) {
        if (address instanceof InetSocketAddress)
            return ((InetSocketAddress) address).getAddress().getHostAddress();

        if (address instanceof DomainSocketAddress)
            return ((DomainSocketAddress) address).path();

        return null;
    }

}
