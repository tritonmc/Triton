package com.rexcantor64.triton.utils;

import com.rexcantor64.triton.api.config.FeatureSyntax;

public class DefaultFeatureSyntax implements FeatureSyntax {
    @Override
    public String getLang() {
        return "lang";
    }

    @Override
    public String getArgs() {
        return "args";
    }

    @Override
    public String getArg() {
        return "arg";
    }
}
