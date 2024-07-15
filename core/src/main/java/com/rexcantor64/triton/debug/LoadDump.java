package com.rexcantor64.triton.debug;

import com.google.gson.JsonParseException;
import com.rexcantor64.triton.Triton;
import lombok.Cleanup;
import lombok.val;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.gson.GsonComponentSerializer;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

public class LoadDump {

    private static final String DUMP_FOLDER_NAME = "dumps";

    /**
     * Get messages stored in a dump (by {@link DumpManager}, for example).
     * Empty lines are skipped.
     *
     * @param dumpName  The name of the file of the dump, inside the 'dumps' folder.
     * @param startLine The line number to start fetching (starting at zero, inclusive) messages.
     * @param endLine   The line number to stop fetching (starting at zero, exclusive).
     * @return The list of messages, converted to {@link Component}.
     * @throws IOException        if there is an I/O exception (e.g. file not found, no permission, etc).
     * @throws JsonParseException if some line of the file is not a valid JSON string.
     */
    public static List<Component> getMessagesFromDump(String dumpName, int startLine, int endLine) throws IOException, JsonParseException {
        Path tritonFolderPath = Triton.get().getDataFolder().toPath();
        Path dumpFolderPath = tritonFolderPath.resolve(DUMP_FOLDER_NAME);
        Path dumpPath = dumpFolderPath.resolve(dumpName);

        if (!dumpPath.toAbsolutePath().normalize().startsWith(dumpFolderPath.toAbsolutePath().normalize())) {
            // path traversal attack
            throw new IOException("Tried to access file outside dump folder");
        }

        File dumpFile = dumpPath.toFile();

        @Cleanup
        val reader = new BufferedReader(new FileReader(dumpFile));

        List<Component> components = new ArrayList<>();
        String line;
        int i = 0;
        while (i < endLine && (line = reader.readLine()) != null) {
            if (i >= startLine && !line.trim().isEmpty()) {
                components.add(GsonComponentSerializer.gson().deserialize(line));
            }
            ++i;
        }
        return components;
    }

}
