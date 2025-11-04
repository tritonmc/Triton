package com.rexcantor64.triton.dependencies;

import com.rexcantor64.triton.loader.utils.LoaderFlag;
import lombok.Data;
import lombok.Getter;
import lombok.val;
import net.byteflux.libby.Library;
import net.byteflux.libby.relocation.Relocation;

import java.util.Arrays;
import java.util.Optional;
import java.util.Set;

public enum Dependency {

    ADVENTURE(
            "net{}kyori",
            "adventure-api",
            "4.25.0",
            "jigb8TV/+Rr5eDpsJCdrSD2BVKezyrWwTpCea/XnLJ0=",
            relocate("net{}kyori{}adventure", "adventure"),
            relocate("net{}kyori{}examination", "kyori{}examination")
    ),
    ADVENTURE_TEXT_SERIALIZER_GSON(
            "net{}kyori",
            "adventure-text-serializer-gson",
            "4.25.0",
            "kx9nlZ1G4P5Vtkq5Fk4xtXi3kjV4f2nUcrYoZfl7ckU=",
            relocateIf("net{}kyori{}adventure", "adventure", LoaderFlag.VENDOR_ADVENTURE),
            relocateIf("net{}kyori{}examination", "kyori{}examination", LoaderFlag.VENDOR_ADVENTURE),
            relocate("net{}kyori{}option", "kyori{}option"),
            relocate("net{}kyori{}adventure{}text{}serializer{}gson", "adventure{}text{}serializer{}gson"),
            relocate("net{}kyori{}adventure{}text{}serializer{}json", "adventure{}text{}serializer{}json")
    ),
    ADVENTURE_TEXT_SERIALIZER_LEGACY(
            "net{}kyori",
            "adventure-text-serializer-legacy",
            "4.25.0",
            "BvFSXr8X+080w08bUmKt5nJwXBFj9qSLaKTgMU36ahM=",
            relocateIf("net{}kyori{}adventure", "adventure", LoaderFlag.VENDOR_ADVENTURE),
            relocateIf("net{}kyori{}examination", "kyori{}examination", LoaderFlag.VENDOR_ADVENTURE),
            relocate("net{}kyori{}adventure{}text{}serializer{}legacy", "adventure{}text{}serializer{}legacy")
    ),
    ADVENTURE_TEXT_SERIALIZER_PLAIN(
            "net{}kyori",
            "adventure-text-serializer-plain",
            "4.25.0",
            "lFN1egpsDdnktfzSVOgqfqhn9MvkA3cWf0QMQexgUu4=",
            relocateIf("net{}kyori{}adventure", "adventure", LoaderFlag.VENDOR_ADVENTURE),
            relocateIf("net{}kyori{}examination", "kyori{}examination", LoaderFlag.VENDOR_ADVENTURE),
            relocate("net{}kyori{}adventure{}text{}serializer{}plain", "adventure{}text{}serializer{}plain")
    ),
    ADVENTURE_TEXT_SERIALIZER_BUNGEECORD(
            "net{}kyori",
            "adventure-text-serializer-bungeecord",
            "4.4.1",
            "4bw3bG3HohAAFgFXNc5MzFNNKya/WrgqrHUcUDIFbDk=",
            relocateIf("net{}kyori{}adventure", "adventure", LoaderFlag.VENDOR_ADVENTURE),
            relocateIf("net{}kyori{}examination", "kyori{}examination", LoaderFlag.VENDOR_ADVENTURE),
            relocate("net{}kyori{}adventure{}text{}serializer{}bungeecord", "adventure{}text{}serializer{}bungeecord"),
            relocateIf("net{}kyori{}adventure{}text{}serializer{}gson", "adventure{}text{}serializer{}gson", LoaderFlag.VENDOR_ADVENTURE_SERIALIZERS),
            relocateIf("net{}kyori{}adventure{}text{}serializer{}json", "adventure{}text{}serializer{}json", LoaderFlag.VENDOR_ADVENTURE_SERIALIZERS)
    ),
    ADVENTURE_MINI_MESSAGE(
            "net{}kyori",
            "adventure-text-minimessage",
            "4.25.0",
            "GY8nxvkeXjJxZAZQhp/v3WgW17MQtIz6UptfWOSpKaQ=",
            relocateIf("net{}kyori{}adventure", "adventure", LoaderFlag.VENDOR_ADVENTURE),
            relocateIf("net{}kyori{}examination", "kyori{}examination", LoaderFlag.VENDOR_ADVENTURE),
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
            "4.25.0",
            "zsjwq/ZC3w5ie/Wp//bxfLtiDqb+BfIWK5bK0VZNiFc=",
            relocate("net{}kyori{}adventure", "adventure"),
            relocate("net{}kyori{}examination", "kyori{}examination")
    ),
    ADVENTURE_TEXT_SERIALIZER_JSON(
            "net{}kyori",
            "adventure-text-serializer-json",
            "4.25.0",
            "SCtKohjXyHn89X5dD5zATbS01LVnnrmbcn9hBO1/uYI=",
            relocateIf("net{}kyori{}adventure", "adventure", LoaderFlag.VENDOR_ADVENTURE),
            relocateIf("net{}kyori{}examination", "kyori{}examination", LoaderFlag.VENDOR_ADVENTURE),
            relocate("net{}kyori{}option", "kyori{}option"),
            relocate("net{}kyori{}adventure{}text{}serializer{}json", "adventure{}text{}serializer{}json")
    ),
    KYORI_EXAMINATION_API(
            "net{}kyori",
            "examination-api",
            "1.3.0",
            "ySN//ssFQo9u/4YhYkascM4LR7BMCOp8o1Ag/eV/hJI=",
            relocate("net{}kyori{}examination", "kyori{}examination")
    ),
    KYORI_EXAMINATION_STRING(
            "net{}kyori",
            "examination-string",
            "1.3.0",
            "fQH8JaS7OvDhZiaFRV9FQfv0YmIW6lhG5FXBSR4Va4w=",
            relocate("net{}kyori{}examination", "kyori{}examination")
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
            "2.10.0",
            "pdaW/xrw13MT9H0xnJez9+WNmafRApdijp34njZ05n4=",
            relocate("com{}github{}retrooper{}packetevents", "packetevents{}api"),
            relocate("io{}github{}retrooper{}packetevents", "packetevents{}impl"),
            relocateIf("net{}kyori{}adventure", "adventure", LoaderFlag.VENDOR_ADVENTURE),
            relocateIf("net{}kyori{}examination", "kyori{}examination", LoaderFlag.VENDOR_ADVENTURE),
            relocateIf("net{}kyori{}option", "kyori{}option", LoaderFlag.VENDOR_ADVENTURE_SERIALIZERS),
            relocateIf("net{}kyori{}adventure{}text{}minimessage", "adventure{}text{}minimessage", LoaderFlag.VENDOR_ADVENTURE_SERIALIZERS),
            relocateIf("net{}kyori{}adventure{}text{}serializer{}gson", "adventure{}text{}serializer{}gson", LoaderFlag.VENDOR_ADVENTURE_SERIALIZERS),
            relocateIf("net{}kyori{}adventure{}text{}serializer{}json", "adventure{}text{}serializer{}json", LoaderFlag.VENDOR_ADVENTURE_SERIALIZERS),
            relocateIf("net{}kyori{}adventure{}text{}serializer{}legacy", "adventure{}text{}serializer{}legacy", LoaderFlag.VENDOR_ADVENTURE_SERIALIZERS)
    ),
    PACKET_EVENTS_NETTY_COMMON(
            "com{}github{}retrooper",
            "packetevents-netty-common",
            "2.10.0",
            "3WafAPe9IXICdUgbffRMpAq4zj3LniwNkO/XfnZAX+g=",
            relocate("com{}github{}retrooper{}packetevents", "packetevents{}api"),
            relocate("io{}github{}retrooper{}packetevents", "packetevents{}impl"),
            relocateIf("net{}kyori{}adventure", "adventure", LoaderFlag.VENDOR_ADVENTURE),
            relocateIf("net{}kyori{}examination", "kyori{}examination", LoaderFlag.VENDOR_ADVENTURE),
            relocateIf("net{}kyori{}option", "kyori{}option", LoaderFlag.VENDOR_ADVENTURE_SERIALIZERS),
            relocateIf("net{}kyori{}adventure{}text{}minimessage", "adventure{}text{}minimessage", LoaderFlag.VENDOR_ADVENTURE_SERIALIZERS),
            relocateIf("net{}kyori{}adventure{}text{}serializer{}gson", "adventure{}text{}serializer{}gson", LoaderFlag.VENDOR_ADVENTURE_SERIALIZERS),
            relocateIf("net{}kyori{}adventure{}text{}serializer{}json", "adventure{}text{}serializer{}json", LoaderFlag.VENDOR_ADVENTURE_SERIALIZERS),
            relocateIf("net{}kyori{}adventure{}text{}serializer{}legacy", "adventure{}text{}serializer{}legacy", LoaderFlag.VENDOR_ADVENTURE_SERIALIZERS)
    ),
    PACKET_EVENTS_SPIGOT(
            "com{}github{}retrooper",
            "packetevents-spigot",
            "2.10.0",
            "DFb6juVlnwbbNIvGYUXDIRfjXFWQ+isuU/iVuwn56Yw=",
            relocate("com{}github{}retrooper{}packetevents", "packetevents{}api"),
            relocate("io{}github{}retrooper{}packetevents", "packetevents{}impl"),
            relocateIf("net{}kyori{}adventure", "adventure", LoaderFlag.VENDOR_ADVENTURE),
            relocateIf("net{}kyori{}examination", "kyori{}examination", LoaderFlag.VENDOR_ADVENTURE),
            relocateIf("net{}kyori{}option", "kyori{}option", LoaderFlag.VENDOR_ADVENTURE_SERIALIZERS),
            relocateIf("net{}kyori{}adventure{}text{}minimessage", "adventure{}text{}minimessage", LoaderFlag.VENDOR_ADVENTURE_SERIALIZERS),
            relocateIf("net{}kyori{}adventure{}text{}serializer{}gson", "adventure{}text{}serializer{}gson", LoaderFlag.VENDOR_ADVENTURE_SERIALIZERS),
            relocateIf("net{}kyori{}adventure{}text{}serializer{}json", "adventure{}text{}serializer{}json", LoaderFlag.VENDOR_ADVENTURE_SERIALIZERS),
            relocateIf("net{}kyori{}adventure{}text{}serializer{}legacy", "adventure{}text{}serializer{}legacy", LoaderFlag.VENDOR_ADVENTURE_SERIALIZERS)
    ),
    PACKET_EVENTS_VELOCITY(
            "com{}github{}retrooper",
            "packetevents-velocity",
            "2.10.0",
            "EUbMnX9enkzYbdduoc+KS/3FjbCke0i4ZvMAQHjLFXY=",
            relocate("com{}github{}retrooper{}packetevents", "packetevents{}api"),
            relocate("io{}github{}retrooper{}packetevents", "packetevents{}impl"),
            relocateIf("net{}kyori{}adventure", "adventure", LoaderFlag.VENDOR_ADVENTURE),
            relocateIf("net{}kyori{}examination", "kyori{}examination", LoaderFlag.VENDOR_ADVENTURE),
            relocateIf("net{}kyori{}option", "kyori{}option", LoaderFlag.VENDOR_ADVENTURE_SERIALIZERS),
            relocateIf("net{}kyori{}adventure{}text{}minimessage", "adventure{}text{}minimessage", LoaderFlag.VENDOR_ADVENTURE_SERIALIZERS),
            relocateIf("net{}kyori{}adventure{}text{}serializer{}gson", "adventure{}text{}serializer{}gson", LoaderFlag.VENDOR_ADVENTURE_SERIALIZERS),
            relocateIf("net{}kyori{}adventure{}text{}serializer{}json", "adventure{}text{}serializer{}json", LoaderFlag.VENDOR_ADVENTURE_SERIALIZERS),
            relocateIf("net{}kyori{}adventure{}text{}serializer{}legacy", "adventure{}text{}serializer{}legacy", LoaderFlag.VENDOR_ADVENTURE_SERIALIZERS)
    );

    @Getter
    private final String groupId;
    private final String artifactId;
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
        return new ConditionalRelocation(relocateFrom, relocateTo, flag);
    }

    private static Relocation relocateInner(String relocateFrom, String relocateTo) {
        return new Relocation(relocateFrom, "com{}rexcantor64{}triton{}lib{}" + relocateTo);
    }

    private interface OptionalRelocation {
        Optional<Relocation> relocate(Set<LoaderFlag> loaderFlags);
    }

    @Data
    private static class ConditionalRelocation implements OptionalRelocation {
        private final String relocateFrom;
        private final String relocateTo;
        private final LoaderFlag flag;

        @Override
        public Optional<Relocation> relocate(Set<LoaderFlag> loaderFlags) {
            if (loaderFlags.contains(flag)) {
                return Optional.of(Dependency.relocateInner(relocateFrom, relocateTo));
            }
            return Optional.empty();
        }
    }

    @Data
    private static class SimpleRelocation implements OptionalRelocation {
        private final Relocation relocation;

        @Override
        public Optional<Relocation> relocate(Set<LoaderFlag> loaderFlags) {
            return Optional.of(relocation);
        }
    }

}
