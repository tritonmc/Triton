package com.rexcantor64.triton.commands.handler.exceptions;

import lombok.AllArgsConstructor;
import lombok.Getter;

@AllArgsConstructor
public class NoPermissionException extends Exception {
    @Getter
    private final String permission;
}
