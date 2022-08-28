package com.rexcantor64.triton.commands.handler.exceptions;

import com.rexcantor64.triton.commands.handler.CommandEvent;

/**
 * Thrown when the current platform is a proxy and can't handle the current command.
 * The {@link CommandEvent} will then be forwarded to a server.
 */
public class UnsupportedPlatformException extends Exception {
}
