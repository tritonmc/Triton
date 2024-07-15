package com.rexcantor64.triton.commands.handler.exceptions;

/**
 * Thrown when the command can only be executed by a player in-game,
 * but it is executed by a non-player executor (console, command blocks, etc).
 *
 * @since 4.0.0
 */
public class PlayerOnlyCommandException extends Exception {
}
