package com.rexcantor64.triton.dependencies;

import me.lucko.jarrelocator.JarRelocator;
import net.byteflux.libby.relocation.Relocation;
import net.byteflux.libby.relocation.RelocationHelper;

import java.io.IOException;
import java.nio.file.Path;
import java.util.Collection;
import java.util.LinkedList;
import java.util.List;

import static java.util.Objects.requireNonNull;

public class NativeRelocationHelper implements RelocationHelper {
    @Override
    public void relocate(Path in, Path out, Collection<Relocation> relocations) {
        requireNonNull(in, "in");
        requireNonNull(out, "out");
        requireNonNull(relocations, "relocations");

        try {
            List<me.lucko.jarrelocator.Relocation> rules = new LinkedList<>();
            for (Relocation relocation : relocations) {
                rules.add(new me.lucko.jarrelocator.Relocation(
                        relocation.getPattern(),
                        relocation.getRelocatedPattern(),
                        relocation.getIncludes(),
                        relocation.getExcludes()
                ));
            }

            new JarRelocator(in.toFile(), out.toFile(), rules).run();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
