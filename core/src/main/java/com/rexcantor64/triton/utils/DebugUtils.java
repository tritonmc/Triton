package com.rexcantor64.triton.utils;

import com.google.gson.ExclusionStrategy;
import com.google.gson.FieldAttributes;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import com.rexcantor64.triton.Triton;
import lombok.Cleanup;
import lombok.val;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

public class DebugUtils {

    public static String generateDebugInfo() {
        Gson gson = new GsonBuilder().setExclusionStrategies(new ExclusionStrategy() {
            @Override
            public boolean shouldSkipField(FieldAttributes fieldAttributes) {
                return fieldAttributes.getAnnotation(GsonExclude.class) != null;
            }

            @Override
            public boolean shouldSkipClass(Class<?> clazz) {
                return clazz.getAnnotation(GsonExclude.class) != null;
            }
        }).setPrettyPrinting().create();
        val instance = Triton.get();

        JsonObject root = new JsonObject();
        root.addProperty("tritonVersion", instance.getVersion());
        root.addProperty("platform", Triton.platform().toString());
        root.add("loaderFlags", gson.toJsonTree(instance.getLoader().getLoaderFlags()));
        root.add("platformData", instance.getPlatformDebugInfo());
        root.addProperty("textTranslationCount", instance.getTranslationManager().getTextTranslationCount());
        root.addProperty("signTranslationCount", instance.getTranslationManager().getSignTranslationCount());
        root.add("config", gson.toJsonTree(instance.getConfig()));

        return gson.toJson(root);
    }

    /**
     *
     * @return The path of the saved file, relative to the plugin data dir
     */
    public static @Nullable String saveDebugInfo(@NotNull String contents) {
        String date = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH_mm_ss"));

        String fileName = String.format("triton-%s-%s.json", Triton.platform().toString().toLowerCase(Locale.ROOT), date);

        Path tritonFolderPath = Triton.get().getDataFolder().toPath();
        Path infoFolderPath = tritonFolderPath.resolve("debug-info");
        Path infoPath = infoFolderPath.resolve(fileName);

        File infoFolderFile = infoFolderPath.toFile();
        if (!infoFolderFile.isDirectory() && !infoFolderFile.mkdirs()) {
            Triton.get().getLogger().logError("Failed to create \"%1\" folder!", infoFolderPath.toAbsolutePath().toString());
            return null;
        }

        File infoFile = infoPath.toFile();

        try {
            @Cleanup
            val writer = new BufferedWriter(new FileWriter(infoFile, true));

            writer.write(contents);
            writer.write("\n");

            return tritonFolderPath.relativize(infoPath).toString();
        } catch (IOException exception) {
            Triton.get().getLogger().logError(exception, "Failed writing to debug info %1!", infoPath.toString());
            return null;
        }
    }

    @Retention(RetentionPolicy.RUNTIME)
    @Target(ElementType.FIELD)
    public @interface GsonExclude {
    }
}
