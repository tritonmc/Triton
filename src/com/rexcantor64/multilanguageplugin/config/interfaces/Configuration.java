package com.rexcantor64.multilanguageplugin.config.interfaces;

import java.util.List;

public abstract class Configuration {

    public abstract Configuration getConfigurationSection(String path);

    public abstract Configuration createSection(String path);

    public abstract boolean getBoolean(String path, boolean def);

    public abstract String getString(String path, String def);

    public abstract int getInt(String path, int def);

    public abstract List<String> getStringList(String path);

}
