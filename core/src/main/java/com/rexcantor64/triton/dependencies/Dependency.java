package com.rexcantor64.triton.dependencies;

import lombok.Getter;
import lombok.val;
import net.byteflux.libby.Library;
import net.byteflux.libby.relocation.Relocation;

public enum Dependency {

    ADVENTURE(
            "net{}kyori",
            "adventure-api",
            "4.12.0",
            "vkfxrI917x81BfIwPLk0fPVNpb+iEGVQzJ1vm4weijQ="
    ),
    ADVENTURE_TEXT_SERIALIZER_GSON(
            "net{}kyori",
            "adventure-text-serializer-gson",
            "4.12.0",
            "Ioindur0+rZ+O3cJ+UKjNekSRaltHh9BwaKc5hlcEo0=",
            relocate("net{}kyori{}adventure{}text{}serializer{}gson", "adventure{}serializer{}gson")
    ),
    ADVENTURE_TEXT_SERIALIZER_LEGACY(
            "net{}kyori",
            "adventure-text-serializer-legacy",
            "4.12.0",
            "GI688kJH9jLfbwUbfmIgyya0lbJE1nOVaj1JqLQ5YAI=",
            relocate("net{}kyori{}adventure{}text{}serializer{}legacy", "adventure{}serializer{}legacy")
    ),
    ADVENTURE_TEXT_SERIALIZER_PLAIN(
            "net{}kyori",
            "adventure-text-serializer-plain",
            "4.12.0",
            "geOXCwvtvPho9dPV+urFQ6GXO4DgKxvKSEfdwYaa23I=",
            relocate("net{}kyori{}adventure{}text{}serializer{}plain", "adventure{}serializer{}plain")
    ),
    ADVENTURE_TEXT_SERIALIZER_BUNGEECORD(
            "net{}kyori",
            "adventure-text-serializer-bungeecord",
            "4.2.0",
            "v7CwvYDAPVgkhmi6QR3IP02mILywNnPHNK8lDx13kKI=",
            relocate("net{}kyori{}adventure{}text{}serializer{}bungeecord", "adventure{}serializer{}bungeecord"),
            relocate("net{}kyori{}adventure{}text{}serializer{}gson", "adventure{}serializer{}gson"),
            relocate("net{}kyori{}adventure{}text{}serializer{}legacy", "adventure{}serializer{}legacy")
    ),

    // Dependencies of Adventure
    ADVENTURE_KEY(
            "net{}kyori",
            "adventure-key",
            "4.12.0",
            "vsVR644rA0OShZxU2fgBPO3pdipwl0ctk7yYO3Xl/z8="
    ),
    KYORI_EXAMINATION(
            "net{}kyori",
            "examination-api",
            "1.3.0",
            "ySN//ssFQo9u/4YhYkascM4LR7BMCOp8o1Ag/eV/hJI="
    );

    @Getter
    private final Library library;

    Dependency(String groupId, String artifactId, String version, String sha256Checksum, Relocation... relocations) {
        val libraryBuilder = Library.builder()
                .groupId(groupId)
                .artifactId(artifactId)
                .version(version)
                .checksum(sha256Checksum);
        for (Relocation relocation : relocations) {
            libraryBuilder.relocate(relocation);
        }
        this.library = libraryBuilder.build();
    }

    private static Relocation relocate(String relocateFrom, String relocateTo) {
        return new Relocation(relocateFrom, "com{}rexcantor64{}triton{}lib{}" + relocateTo);
    }

}
