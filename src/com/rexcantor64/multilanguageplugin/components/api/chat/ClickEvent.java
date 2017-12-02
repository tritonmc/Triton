package com.rexcantor64.multilanguageplugin.components.api.chat;

import java.beans.ConstructorProperties;

public final class ClickEvent {
    private final Action action;
    private final String value;

    public String toString() {
        return "ClickEvent(action=" + getAction() + ", value=" + getValue() + ")";
    }

    @ConstructorProperties({"action", "value"})
    public ClickEvent(Action action, String value) {
        this.action = action;
        this.value = value;
    }

    public Action getAction() {
        return this.action;
    }

    public String getValue() {
        return this.value;
    }

    public enum Action {
        OPEN_URL, OPEN_FILE, RUN_COMMAND, SUGGEST_COMMAND, CHANGE_PAGE;
    }
}