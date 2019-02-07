package com.rexcantor64.triton.language.parser;

import java.util.HashMap;
import java.util.List;
import java.util.UUID;

public class AdvancedComponent {

    private String text;
    private HashMap<String, String> components = new HashMap<>();
    private HashMap<String, List<AdvancedComponent>> translatableArguments = new HashMap<>();

    public String getText() {
        return text;
    }

    public void setText(String text) {
        this.text = text;
    }

    public void setComponent(UUID uuid, String text) {
        components.put(uuid.toString(), text);
    }

    public void setComponent(String uuid, String text) {
        components.put(uuid, text);
    }

    public String getComponent(String uuid) {
        return components.get(uuid);
    }

    public void setTranslatableArguments(UUID uuid, List<AdvancedComponent> list) {
        translatableArguments.put(uuid.toString(), list);
    }

    public List<AdvancedComponent> getTranslatableArguments(String uuid) {
        return translatableArguments.get(uuid);
    }

    public HashMap<String, String> getComponents() {
        return components;
    }

}