package com.rexcantor64.triton.utils;

import com.google.common.io.ByteStreams;
import com.rexcantor64.triton.Triton;
import lombok.SneakyThrows;

import java.io.*;
import java.nio.charset.StandardCharsets;

public class FileUtils {

    // TODO
    public static String contentsToString(File file) {
        StringBuilder contentBuilder = new StringBuilder();
        try (BufferedReader br = new BufferedReader(new InputStreamReader(
                new FileInputStream(file), StandardCharsets.UTF_8))) {

            String sCurrentLine;
            while ((sCurrentLine = br.readLine()) != null) {
                contentBuilder.append(sCurrentLine).append("\n");
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return contentBuilder.toString();
    }

    public static File getResource(String fileName, String internalFileName) {
        Triton.get().getLogger().logInfo(2, "Reading %1 file...", fileName);
        File folder = Triton.get().getDataFolder();
        if (!folder.exists()) {
            Triton.get().getLogger().logInfo(2, "Plugin folder not found. Creating one...");
            if (!folder.mkdirs())
                Triton.get().getLogger()
                        .logError("Failed to create plugin folder! Please check if the server has the necessary " +
                                "permissions. The plugin will not work correctly.");
            else
                Triton.get().getLogger().logInfo(2, "Plugin folder created.");
        }

        File resourceFile = new File(folder, fileName);
        try {
            if (!resourceFile.exists()) {
                Triton.get().getLogger().logInfo(2, "File %1 not found. Creating new one...", fileName);
                if (!resourceFile.createNewFile())
                    Triton.get().getLogger().logError("Failed to create the file %1!", fileName);
                try (InputStream in = Triton.get().getLoader().getResourceAsStream(internalFileName);
                     OutputStream out = new FileOutputStream(resourceFile)) {
                    ByteStreams.copy(in, out);
                }
                Triton.get().getLogger().logInfo(2, "File %1 created successfully.", fileName);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return resourceFile;
    }

    @SneakyThrows
    public static Reader getReaderFromFile(File file) {
        return new BufferedReader(new InputStreamReader(new FileInputStream(file), StandardCharsets.UTF_8));
    }

    @SneakyThrows
    public static Writer getWriterFromFile(File file) {
        return new OutputStreamWriter(new FileOutputStream(file), StandardCharsets.UTF_8);
    }

}
