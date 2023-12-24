package com.rexcantor64.triton.dependencies;

import lombok.Getter;
import lombok.val;
import net.byteflux.libby.Library;
import net.byteflux.libby.relocation.Relocation;

public enum Dependency {

    ADVENTURE(
            "net{}kyori",
            "adventure-api",
            "4.15.0",
            "TDRoKSrXCMxo/4+pd4/tKvfbcagSYfeQRmzQM/8BgZQ="
    ),
    ADVENTURE_TEXT_SERIALIZER_GSON(
            "net{}kyori",
            "adventure-text-serializer-gson",
            "4.15.0",
            "0t/P0pGWrqe0zNNySTUrWmEFbM7ijGXv9KmJ9zfuIDo=",
            relocate("net{}kyori{}adventure{}text{}serializer{}gson", "adventure{}serializer{}gson"),
            relocate("net{}kyori{}adventure{}text{}serializer{}json", "adventure{}serializer{}json"),
            relocate("net{}kyori{}option", "kyori{}option")
    ),
    ADVENTURE_TEXT_SERIALIZER_LEGACY(
            "net{}kyori",
            "adventure-text-serializer-legacy",
            "4.15.0",
            "05buwHoYTm1E8/wJBJDZiCzMlinvDhmR51kIhceIDgs=",
            relocate("net{}kyori{}adventure{}text{}serializer{}legacy", "adventure{}serializer{}legacy")
    ),
    ADVENTURE_TEXT_SERIALIZER_PLAIN(
            "net{}kyori",
            "adventure-text-serializer-plain",
            "4.15.0",
            "T9uUMFA3ehElOSOfEOcy0tukCyoh8lKDlYooIypz+Ok=",
            relocate("net{}kyori{}adventure{}text{}serializer{}plain", "adventure{}serializer{}plain")
    ),
    ADVENTURE_TEXT_SERIALIZER_BUNGEECORD(
            "net{}kyori",
            "adventure-text-serializer-bungeecord",
            "4.3.2",
            "4bw3bG3HohAAFgFXNc5MzFNNKya/WrgqrHUcUDIFbDk=",
            relocate("net{}kyori{}adventure{}text{}serializer{}bungeecord", "adventure{}serializer{}bungeecord"),
            relocate("net{}kyori{}adventure{}text{}serializer{}gson", "adventure{}serializer{}gson"),
            relocate("net{}kyori{}adventure{}text{}serializer{}json", "adventure{}serializer{}json"),
            relocate("net{}kyori{}option", "kyori{}option"),
            relocate("net{}kyori{}adventure{}text{}serializer{}legacy", "adventure{}serializer{}legacy")
    ),

    // Dependencies of Adventure
    ADVENTURE_KEY(
            "net{}kyori",
            "adventure-key",
            "4.15.0",
            "fG80fIMSZFVT941uItSDVVbUNtUBioL6sHxK3uDW/tc="
    ),
    ADVENTURE_TEXT_SERIALIZER_JSON(
            "net{}kyori",
            "adventure-text-serializer-json",
            "4.15.0",
            "IjUGO0PYrqRiXPrCgKHlJ1NmDq0b4rk4Gz544ouMI6Y=",
            relocate("net{}kyori{}adventure{}text{}serializer{}json", "adventure{}serializer{}json"),
            relocate("net{}kyori{}option", "kyori{}option")
    ),
    KYORI_EXAMINATION(
            "net{}kyori",
            "examination-api",
            "1.3.0",
            "ySN//ssFQo9u/4YhYkascM4LR7BMCOp8o1Ag/eV/hJI="
    ),
    KYORI_OPTION(
            "net{}kyori",
            "option",
            "1.0.0",
            "K95aei1z+hFmoPFmOiLDr30naP/E/qMxd0w8D2IiXRE=",
            relocate("net{}kyori{}option", "kyori{}option")
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
