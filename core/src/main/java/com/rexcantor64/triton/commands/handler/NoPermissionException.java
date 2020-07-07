package com.rexcantor64.triton.commands.handler;

import lombok.AllArgsConstructor;
import lombok.Getter;

@AllArgsConstructor
public class NoPermissionException extends RuntimeException {
    @Getter
    private final String permission;
}
