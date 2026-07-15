package com.rexcantor64.triton.dependencies;

import com.rexcantor64.triton.loader.utils.LoaderFlag;
import lombok.Getter;
import lombok.val;
import net.byteflux.libby.Library;
import net.byteflux.libby.relocation.Relocation;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Optional;
import java.util.Set;

public enum Dependency {

    ADVENTURE(
            "net{}kyori",
            "adventure-api",
            "5.2.0",
            "flL+cZC+Poezs/cXEs+hIxX80nEJ8wKntEDBLwH66Cc=",
            relocate("net{}kyori{}adventure", "adventure")
    ),
    ADVENTURE_TEXT_SERIALIZER_GSON(
            "net{}kyori",
            "adventure-text-serializer-gson",
            "5.2.0",
            "WyrRyKZef6CBEnm1GkXV/FdLamNUCXtUXk/bW2iq7XM=",
            relocateIf("net{}kyori{}adventure", "adventure", LoaderFlag.VENDOR_ADVENTURE),
            relocate("net{}kyori{}option", "kyori{}option"),
            relocate("net{}kyori{}adventure{}text{}serializer{}gson", "adventure{}text{}serializer{}gson"),
            relocate("net{}kyori{}adventure{}text{}serializer{}json", "adventure{}text{}serializer{}json")
    ),
    ADVENTURE_TEXT_SERIALIZER_LEGACY(
            "net{}kyori",
            "adventure-text-serializer-legacy",
            "5.2.0",
            "EjGg3kF95mxZ4vKjiS7MprjUhiHfbx2aL4GwH1fjKLc=",
            relocateIf("net{}kyori{}adventure", "adventure", LoaderFlag.VENDOR_ADVENTURE),
            relocate("net{}kyori{}adventure{}text{}serializer{}legacy", "adventure{}text{}serializer{}legacy")
    ),
    ADVENTURE_TEXT_SERIALIZER_PLAIN(
            "net{}kyori",
            "adventure-text-serializer-plain",
            "5.2.0",
            "9kJMwDimMbecxLdLazU9XQB8mbOPm+SC4MJEigDuzSE=",
            relocateIf("net{}kyori{}adventure", "adventure", LoaderFlag.VENDOR_ADVENTURE),
            relocate("net{}kyori{}adventure{}text{}serializer{}plain", "adventure{}text{}serializer{}plain")
    ),
    ADVENTURE_TEXT_SERIALIZER_BUNGEECORD(
            "net{}kyori",
            "adventure-text-serializer-bungeecord",
            "4.4.1",
            "4bw3bG3HohAAFgFXNc5MzFNNKya/WrgqrHUcUDIFbDk=",
            relocateIf("net{}kyori{}adventure", "adventure", LoaderFlag.VENDOR_ADVENTURE),
            relocate("net{}kyori{}adventure{}text{}serializer{}bungeecord", "adventure{}text{}serializer{}bungeecord"),
            relocateIf("net{}kyori{}adventure{}text{}serializer{}gson", "adventure{}text{}serializer{}gson", LoaderFlag.VENDOR_ADVENTURE_SERIALIZERS),
            relocateIf("net{}kyori{}adventure{}text{}serializer{}json", "adventure{}text{}serializer{}json", LoaderFlag.VENDOR_ADVENTURE_SERIALIZERS)
    ),
    ADVENTURE_MINI_MESSAGE(
            "net{}kyori",
            "adventure-text-minimessage",
            "5.2.0",
            "4YcauhUR2+Sc/yEcEPhWgohgFBYeXWKTGKppWDwxosA=",
            relocateIf("net{}kyori{}adventure", "adventure", LoaderFlag.VENDOR_ADVENTURE),
            relocate("net{}kyori{}option", "kyori{}option"),
            relocate("net{}kyori{}adventure{}text{}minimessage", "adventure{}text{}minimessage"),
            relocate("net{}kyori{}adventure{}text{}serializer{}gson", "adventure{}text{}serializer{}gson"),
            relocate("net{}kyori{}adventure{}text{}serializer{}json", "adventure{}text{}serializer{}json"),
            relocate("net{}kyori{}adventure{}text{}serializer{}legacy", "adventure{}text{}serializer{}legacy")
    ),

    // Dependencies of Adventure
    ADVENTURE_KEY(
            "net{}kyori",
            "adventure-key",
            "5.2.0",
            "AYTRcyAOLu+PvHkfYi0dWP1Fn4kwxha1pP556D7abFU=",
            relocate("net{}kyori{}adventure", "adventure")
    ),
    ADVENTURE_NBT(
            "net{}kyori",
            "adventure-nbt",
            "5.2.0",
            "g06U1siDrF26QwVGMrw4Pu5uOb1CuhuHtrb0e8+EVUs=",
            new SimpleRelocation(relocateInner("net{}kyori{}adventure{}nbt", "adventure{}nbt", null, Collections.singleton("net{}kyori{}adventure{}nbt{}api{}*"))),
            relocateIf("net{}kyori{}adventure", "adventure", LoaderFlag.VENDOR_ADVENTURE)
    ),
    ADVENTURE_TEXT_SERIALIZER_JSON(
            "net{}kyori",
            "adventure-text-serializer-json",
            "5.2.0",
            "rbVBAAYXW5qmNDv63QvRHEppVe4qvBpYq+Kj+my9mrU=",
            relocateIf("net{}kyori{}adventure", "adventure", LoaderFlag.VENDOR_ADVENTURE),
            relocate("net{}kyori{}option", "kyori{}option"),
            relocate("net{}kyori{}adventure{}text{}serializer{}json", "adventure{}text{}serializer{}json")
    ),
    KYORI_OPTION(
            "net{}kyori",
            "option",
            "1.1.0",
            "l7abSxff4CIXyRMa00JWTLya69BMdetoljm194/UsRw=",
            relocate("net{}kyori{}option", "kyori{}option")
    ),

    // PacketEvents
    PACKET_EVENTS_API(
            "com{}github{}retrooper",
            "packetevents-api",
            "2.13.0-triton",
            "0Tbdp4ISSzN+H4KfTkMxLApexKZy/TpcX1P0xzLQTIw=",
            relocate("com{}github{}retrooper{}packetevents", "packetevents{}api"),
            relocate("io{}github{}retrooper{}packetevents", "packetevents{}impl"),
            relocateIf("net{}kyori{}adventure", "adventure", LoaderFlag.VENDOR_ADVENTURE),
            new ConditionalRelocation(relocateInner("net{}kyori{}adventure{}nbt", "adventure{}nbt", null, Collections.singleton("net{}kyori{}adventure{}nbt{}api{}*")), LoaderFlag.VENDOR_ADVENTURE_NBT),
            relocateIf("net{}kyori{}option", "kyori{}option", LoaderFlag.VENDOR_ADVENTURE_SERIALIZERS),
            relocateIf("net{}kyori{}adventure{}text{}minimessage", "adventure{}text{}minimessage", LoaderFlag.VENDOR_ADVENTURE_SERIALIZERS),
            relocateIf("net{}kyori{}adventure{}text{}serializer{}gson", "adventure{}text{}serializer{}gson", LoaderFlag.VENDOR_ADVENTURE_SERIALIZERS),
            relocateIf("net{}kyori{}adventure{}text{}serializer{}json", "adventure{}text{}serializer{}json", LoaderFlag.VENDOR_ADVENTURE_SERIALIZERS),
            relocateIf("net{}kyori{}adventure{}text{}serializer{}legacy", "adventure{}text{}serializer{}legacy", LoaderFlag.VENDOR_ADVENTURE_SERIALIZERS)
    ),
    PACKET_EVENTS_NETTY_COMMON(
            "com{}github{}retrooper",
            "packetevents-netty-common",
            "2.13.0-triton",
            "rLMuNV7gMBFi25dXfyUYB7R7c+vGx1F5IBSB/+EbDQs=",
            relocate("com{}github{}retrooper{}packetevents", "packetevents{}api"),
            relocate("io{}github{}retrooper{}packetevents", "packetevents{}impl"),
            relocateIf("net{}kyori{}adventure", "adventure", LoaderFlag.VENDOR_ADVENTURE),
            new ConditionalRelocation(relocateInner("net{}kyori{}adventure{}nbt", "adventure{}nbt", null, Collections.singleton("net{}kyori{}adventure{}nbt{}api{}*")), LoaderFlag.VENDOR_ADVENTURE_NBT),
            relocateIf("net{}kyori{}option", "kyori{}option", LoaderFlag.VENDOR_ADVENTURE_SERIALIZERS),
            relocateIf("net{}kyori{}adventure{}text{}minimessage", "adventure{}text{}minimessage", LoaderFlag.VENDOR_ADVENTURE_SERIALIZERS),
            relocateIf("net{}kyori{}adventure{}text{}serializer{}gson", "adventure{}text{}serializer{}gson", LoaderFlag.VENDOR_ADVENTURE_SERIALIZERS),
            relocateIf("net{}kyori{}adventure{}text{}serializer{}json", "adventure{}text{}serializer{}json", LoaderFlag.VENDOR_ADVENTURE_SERIALIZERS),
            relocateIf("net{}kyori{}adventure{}text{}serializer{}legacy", "adventure{}text{}serializer{}legacy", LoaderFlag.VENDOR_ADVENTURE_SERIALIZERS)
    ),
    PACKET_EVENTS_SPIGOT(
            "com{}github{}retrooper",
            "packetevents-spigot",
            "2.13.0-triton",
            "EtDA4jV/GwK0i5hbCiV6PB1X0rA1rLNoieBJN9WpVbw=",
            relocate("com{}github{}retrooper{}packetevents", "packetevents{}api"),
            relocate("io{}github{}retrooper{}packetevents", "packetevents{}impl"),
            relocateIf("net{}kyori{}adventure", "adventure", LoaderFlag.VENDOR_ADVENTURE),
            new ConditionalRelocation(relocateInner("net{}kyori{}adventure{}nbt", "adventure{}nbt", null, Collections.singleton("net{}kyori{}adventure{}nbt{}api{}*")), LoaderFlag.VENDOR_ADVENTURE_NBT),
            relocateIf("net{}kyori{}option", "kyori{}option", LoaderFlag.VENDOR_ADVENTURE_SERIALIZERS),
            relocateIf("net{}kyori{}adventure{}text{}minimessage", "adventure{}text{}minimessage", LoaderFlag.VENDOR_ADVENTURE_SERIALIZERS),
            relocateIf("net{}kyori{}adventure{}text{}serializer{}gson", "adventure{}text{}serializer{}gson", LoaderFlag.VENDOR_ADVENTURE_SERIALIZERS),
            relocateIf("net{}kyori{}adventure{}text{}serializer{}json", "adventure{}text{}serializer{}json", LoaderFlag.VENDOR_ADVENTURE_SERIALIZERS),
            relocateIf("net{}kyori{}adventure{}text{}serializer{}legacy", "adventure{}text{}serializer{}legacy", LoaderFlag.VENDOR_ADVENTURE_SERIALIZERS)
    ),
    PACKET_EVENTS_BUNGEE(
            "com{}github{}retrooper",
            "packetevents-bungeecord",
            "2.13.0-triton",
            "i6gF2UoxTyGthIT+TfeatnBgf3jUPIBcoujEi5RGaL4=",
            relocate("com{}github{}retrooper{}packetevents", "packetevents{}api"),
            relocate("io{}github{}retrooper{}packetevents", "packetevents{}impl"),
            relocateIf("net{}kyori{}adventure", "adventure", LoaderFlag.VENDOR_ADVENTURE),
            new ConditionalRelocation(relocateInner("net{}kyori{}adventure{}nbt", "adventure{}nbt", null, Collections.singleton("net{}kyori{}adventure{}nbt{}api{}*")), LoaderFlag.VENDOR_ADVENTURE_NBT),
            relocateIf("net{}kyori{}option", "kyori{}option", LoaderFlag.VENDOR_ADVENTURE_SERIALIZERS),
            relocateIf("net{}kyori{}adventure{}text{}minimessage", "adventure{}text{}minimessage", LoaderFlag.VENDOR_ADVENTURE_SERIALIZERS),
            relocateIf("net{}kyori{}adventure{}text{}serializer{}gson", "adventure{}text{}serializer{}gson", LoaderFlag.VENDOR_ADVENTURE_SERIALIZERS),
            relocateIf("net{}kyori{}adventure{}text{}serializer{}json", "adventure{}text{}serializer{}json", LoaderFlag.VENDOR_ADVENTURE_SERIALIZERS),
            relocateIf("net{}kyori{}adventure{}text{}serializer{}legacy", "adventure{}text{}serializer{}legacy", LoaderFlag.VENDOR_ADVENTURE_SERIALIZERS)
    ),
    PACKET_EVENTS_VELOCITY(
            "com{}github{}retrooper",
            "packetevents-velocity",
            "2.13.0-triton",
            "4h8RvCA3fDDQQ89zBqEQ5a2Lfn7XVi7IIMEpUhqhrC4=",
            relocate("com{}github{}retrooper{}packetevents", "packetevents{}api"),
            relocate("io{}github{}retrooper{}packetevents", "packetevents{}impl"),
            relocateIf("net{}kyori{}adventure", "adventure", LoaderFlag.VENDOR_ADVENTURE),
            new ConditionalRelocation(relocateInner("net{}kyori{}adventure{}nbt", "adventure{}nbt", null, Collections.singleton("net{}kyori{}adventure{}nbt{}api{}*")), LoaderFlag.VENDOR_ADVENTURE_NBT),
            relocateIf("net{}kyori{}option", "kyori{}option", LoaderFlag.VENDOR_ADVENTURE_SERIALIZERS),
            relocateIf("net{}kyori{}adventure{}text{}minimessage", "adventure{}text{}minimessage", LoaderFlag.VENDOR_ADVENTURE_SERIALIZERS),
            relocateIf("net{}kyori{}adventure{}text{}serializer{}gson", "adventure{}text{}serializer{}gson", LoaderFlag.VENDOR_ADVENTURE_SERIALIZERS),
            relocateIf("net{}kyori{}adventure{}text{}serializer{}json", "adventure{}text{}serializer{}json", LoaderFlag.VENDOR_ADVENTURE_SERIALIZERS),
            relocateIf("net{}kyori{}adventure{}text{}serializer{}legacy", "adventure{}text{}serializer{}legacy", LoaderFlag.VENDOR_ADVENTURE_SERIALIZERS)
    );

    @Getter
    private final String groupId;
    private final String artifactId;
    @Getter
    private final String version;
    private final String sha256Checksum;
    private final OptionalRelocation[] relocations;

    Dependency(String groupId, String artifactId, String version, String sha256Checksum, OptionalRelocation... relocations) {
        this.groupId = groupId;
        this.artifactId = artifactId;
        this.version = version;
        this.sha256Checksum = sha256Checksum;
        this.relocations = relocations;
    }

    public Library getLibrary(Set<LoaderFlag> loaderFlags) {
        val builder = Library.builder()
                .groupId(this.groupId)
                .artifactId(this.artifactId)
                .version(this.version)
                .checksum(this.sha256Checksum);

        Arrays.stream(relocations)
                .map(relocation -> relocation.relocate(loaderFlags))
                .filter(Optional::isPresent)
                .map(Optional::get)
                .forEach(builder::relocate);

        return builder.build();
    }

    private static OptionalRelocation relocate(String relocateFrom, String relocateTo) {
        return new SimpleRelocation(relocateInner(relocateFrom, relocateTo));
    }

    private static OptionalRelocation relocateIf(String relocateFrom, String relocateTo, LoaderFlag flag) {
        return new ConditionalRelocation(relocateInner(relocateFrom, relocateTo), flag);
    }

    private static Relocation relocateInner(String relocateFrom, String relocateTo) {
        return new Relocation(relocateFrom, "com{}rexcantor64{}triton{}lib{}" + relocateTo);
    }

    private static @NotNull Relocation relocateInner(@NotNull String relocateFrom, @NotNull String relocateTo, @Nullable Collection<String> includes, @Nullable Collection<String> excludes) {
        return new Relocation(relocateFrom, "com{}rexcantor64{}triton{}lib{}" + relocateTo, includes, excludes);
    }

    private interface OptionalRelocation {
        Optional<Relocation> relocate(Set<LoaderFlag> loaderFlags);
    }

    private record ConditionalRelocation(Relocation relocation, LoaderFlag flag) implements OptionalRelocation {
        @Override
        public Optional<Relocation> relocate(Set<LoaderFlag> loaderFlags) {
            if (loaderFlags.contains(flag)) {
                return Optional.of(relocation);
            }
            return Optional.empty();
        }
    }

    private record SimpleRelocation(Relocation relocation) implements OptionalRelocation {
        @Override
        public Optional<Relocation> relocate(Set<LoaderFlag> loaderFlags) {
            return Optional.of(relocation);
        }
    }

}
