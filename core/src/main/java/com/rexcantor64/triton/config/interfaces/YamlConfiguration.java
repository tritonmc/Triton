package com.rexcantor64.triton.config.interfaces;

import org.yaml.snakeyaml.DumperOptions;
import org.yaml.snakeyaml.LoaderOptions;
import org.yaml.snakeyaml.Yaml;
import org.yaml.snakeyaml.constructor.Constructor;
import org.yaml.snakeyaml.representer.BaseRepresenter;
import org.yaml.snakeyaml.representer.Represent;
import org.yaml.snakeyaml.representer.Representer;

import java.io.*;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.util.LinkedHashMap;
import java.util.Map;

public class YamlConfiguration extends ConfigurationProvider {

    private final ThreadLocal<Yaml> yaml = ThreadLocal.withInitial(() -> {
        DumperOptions options = new DumperOptions();
        options.setDefaultFlowStyle(DumperOptions.FlowStyle.BLOCK);

        try {
            Representer representer = new Representer(options) {
                {
                    representers.put(Configuration.class, data -> represent(((Configuration) data).self));
                }
            };

            return new Yaml(new Constructor(new LoaderOptions()), representer, options);
        } catch (NoSuchMethodError e) {
            // Compatibility with SnakeYAML v1 (for old Spigot versions)

            try {
                // noinspection JavaReflectionMemberAccess
                Representer representer = Representer.class.getConstructor().newInstance();
                Field representersField = BaseRepresenter.class.getDeclaredField("representers");
                representersField.setAccessible(true);
                @SuppressWarnings("unchecked") Map<Class<?>, Represent> representers =
                        (Map<Class<?>, Represent>) representersField.get(representer);
                representers.put(Configuration.class, data -> representer.represent(((Configuration) data).self));

                // noinspection JavaReflectionMemberAccess
                Constructor constructor = Constructor.class.getConstructor().newInstance();
                return new Yaml(constructor, representer, options);
            } catch (NoSuchMethodException | InvocationTargetException | InstantiationException |
                     IllegalAccessException | NoSuchFieldException ex) {
                throw new RuntimeException(ex);
            }
        }
    });

    @Override
    public void save(Configuration config, File file) throws IOException {
        try (FileWriter writer = new FileWriter(file)) {
            save(config, writer);
        }
    }

    @Override
    public void save(Configuration config, Writer writer) {
        yaml.get().dump(config.self, writer);
    }

    @Override
    public Configuration load(File file) throws IOException {
        return load(file, null);
    }

    @Override
    public Configuration load(File file, Configuration defaults) throws IOException {
        try (FileReader reader = new FileReader(file)) {
            return load(reader, defaults);
        }
    }

    @Override
    public Configuration load(Reader reader) {
        return load(reader, null);
    }

    @Override
    @SuppressWarnings("unchecked")
    public Configuration load(Reader reader, Configuration defaults) {
        Map<String, Object> map = yaml.get().loadAs(reader, LinkedHashMap.class);
        if (map == null) {
            map = new LinkedHashMap<>();
        }
        return new Configuration(map, defaults);
    }

    @Override
    public Configuration load(InputStream is) {
        return load(is, null);
    }

    @Override
    @SuppressWarnings("unchecked")
    public Configuration load(InputStream is, Configuration defaults) {
        Map<String, Object> map = yaml.get().loadAs(is, LinkedHashMap.class);
        if (map == null) {
            map = new LinkedHashMap<>();
        }
        return new Configuration(map, defaults);
    }

    @Override
    public Configuration load(String string) {
        return load(string, null);
    }

    @Override
    @SuppressWarnings("unchecked")
    public Configuration load(String string, Configuration defaults) {
        Map<String, Object> map = yaml.get().loadAs(string, LinkedHashMap.class);
        if (map == null) {
            map = new LinkedHashMap<>();
        }
        return new Configuration(map, defaults);
    }
}