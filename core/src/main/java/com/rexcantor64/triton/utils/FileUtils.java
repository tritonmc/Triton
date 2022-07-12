package com.rexcantor64.triton.utils;

import com.google.common.io.ByteStreams;
import com.rexcantor64.triton.Triton;
import lombok.SneakyThrows;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;

public class FileUtils {

    public static File getResource(String fileName, String internalFileName) {
        Triton.get().getLogger().logDebug("Reading %1 file...", fileName);
        File folder = Triton.get().getDataFolder();
        if (!folder.exists()) {
            Triton.get().getLogger().logDebug("Plugin folder not found. Creating one...");
            if (!folder.mkdirs()) {
                Triton.get().getLogger()
                        .logError("Failed to create plugin folder! Please check if the server has the necessary " +
                                "permissions. The plugin will not work correctly.");
            } else {
                Triton.get().getLogger().logDebug("Plugin folder created.");
            }
        }

        File resourceFile = new File(folder, fileName);
        try {
            if (!resourceFile.exists()) {
                Triton.get().getLogger().logDebug("File %1 not found. Creating new one...", fileName);
                if (!resourceFile.createNewFile()) {
                    Triton.get().getLogger().logError("Failed to create the file %1!", fileName);
                }
                try (InputStream in = Triton.get().getLoader().getResourceAsStream(internalFileName);
                     OutputStream out = Files.newOutputStream(resourceFile.toPath())) {
                    ByteStreams.copy(in, out);
                }
                Triton.get().getLogger().logDebug("File %1 created successfully.", fileName);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return resourceFile;
    }

    @SneakyThrows
    public static Reader getReaderFromFile(File file) {
        return new BufferedReader(new InputStreamReader(Files.newInputStream(file.toPath()), StandardCharsets.UTF_8));
    }

    @SneakyThrows
    public static Writer getWriterFromFile(File file) {
        return new OutputStreamWriter(Files.newOutputStream(file.toPath()), StandardCharsets.UTF_8);
    }

}
