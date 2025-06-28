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
            "4.23.0",
            "Ubn89img4yWTTyoyHpdL1tqCMzMmb/CZtK2GJC7cXvQ=",
            relocate("net{}kyori{}adventure", "adventure"),
            relocate("net{}kyori{}examination", "kyori{}examination")
    ),
    ADVENTURE_TEXT_SERIALIZER_GSON(
            "net{}kyori",
            "adventure-text-serializer-gson",
            "4.23.0",
            "PiaD2i0UAwMjzJZFu6TNhtfLq/zkK9Fi7C2lDF59Yig=",
            relocate("net{}kyori{}option", "kyori{}option"),
            relocateIf("net{}kyori{}adventure", "adventure", LoaderFlag.RELOCATE_ADVENTURE),
            relocateIf("net{}kyori{}examination", "kyori{}examination", LoaderFlag.RELOCATE_ADVENTURE),
            relocateIfNot("net{}kyori{}adventure{}text{}serializer{}gson", "adventure{}text{}serializer{}gson", LoaderFlag.RELOCATE_ADVENTURE),
            relocateIfNot("net{}kyori{}adventure{}text{}serializer{}json", "adventure{}text{}serializer{}json", LoaderFlag.RELOCATE_ADVENTURE)
    ),
    ADVENTURE_TEXT_SERIALIZER_LEGACY(
            "net{}kyori",
            "adventure-text-serializer-legacy",
            "4.23.0",
            "1SgOt1vypertB5eXGmzJAtxrjLtv6sJwEcyGJGEIIJ0=",
            relocateIf("net{}kyori{}adventure", "adventure", LoaderFlag.RELOCATE_ADVENTURE),
            relocateIf("net{}kyori{}examination", "kyori{}examination", LoaderFlag.RELOCATE_ADVENTURE),
            relocateIfNot("net{}kyori{}adventure{}text{}serializer{}legacy", "adventure{}text{}serializer{}legacy", LoaderFlag.RELOCATE_ADVENTURE)
    ),
    ADVENTURE_TEXT_SERIALIZER_PLAIN(
            "net{}kyori",
            "adventure-text-serializer-plain",
            "4.23.0",
            "wuHYBMob54Q09K1r29U8yXoROtkpXLTJNs87nZAsWR4=",
            relocateIf("net{}kyori{}adventure", "adventure", LoaderFlag.RELOCATE_ADVENTURE),
            relocateIf("net{}kyori{}examination", "kyori{}examination", LoaderFlag.RELOCATE_ADVENTURE),
            relocateIfNot("net{}kyori{}adventure{}text{}serializer{}plain", "adventure{}text{}serializer{}plain", LoaderFlag.RELOCATE_ADVENTURE)
    ),
    ADVENTURE_TEXT_SERIALIZER_BUNGEECORD(
            "net{}kyori",
            "adventure-text-serializer-bungeecord",
            "4.4.0",
            "4bw3bG3HohAAFgFXNc5MzFNNKya/WrgqrHUcUDIFbDk=",
            relocate("net{}kyori{}option", "kyori{}option"),
            relocateIf("net{}kyori{}adventure", "adventure", LoaderFlag.RELOCATE_ADVENTURE),
            relocateIf("net{}kyori{}examination", "kyori{}examination", LoaderFlag.RELOCATE_ADVENTURE),
            relocateIfNot("net{}kyori{}adventure{}text{}serializer{}bungeecord", "adventure{}text{}serializer{}bungeecord", LoaderFlag.RELOCATE_ADVENTURE),
            relocateIfNot("net{}kyori{}adventure{}text{}serializer{}gson", "adventure{}text{}serializer{}gson", LoaderFlag.RELOCATE_ADVENTURE),
            relocateIfNot("net{}kyori{}adventure{}text{}serializer{}json", "adventure{}text{}serializer{}json", LoaderFlag.RELOCATE_ADVENTURE),
            relocateIfNot("net{}kyori{}adventure{}text{}serializer{}legacy", "adventure{}text{}serializer{}legacy", LoaderFlag.RELOCATE_ADVENTURE)
    ),
    ADVENTURE_MINI_MESSAGE(
            "net{}kyori",
            "adventure-text-minimessage",
            "4.23.0",
            "Vu0nZroiOLQBuuqWDTquRftY/aFhTalo/iuR/bnDgTw=",
            relocate("net{}kyori{}option", "kyori{}option"),
            relocateIf("net{}kyori{}adventure", "adventure", LoaderFlag.RELOCATE_ADVENTURE),
            relocateIf("net{}kyori{}examination", "kyori{}examination", LoaderFlag.RELOCATE_ADVENTURE),
            relocateIfNot("net{}kyori{}adventure{}text{}minimessage", "adventure{}text{}minimessage", LoaderFlag.RELOCATE_ADVENTURE),
            relocateIfNot("net{}kyori{}adventure{}text{}serializer{}gson", "adventure{}text{}serializer{}gson", LoaderFlag.RELOCATE_ADVENTURE),
            relocateIfNot("net{}kyori{}adventure{}text{}serializer{}json", "adventure{}text{}serializer{}json", LoaderFlag.RELOCATE_ADVENTURE),
            relocateIfNot("net{}kyori{}adventure{}text{}serializer{}legacy", "adventure{}text{}serializer{}legacy", LoaderFlag.RELOCATE_ADVENTURE)
    ),

    // Dependencies of Adventure
    ADVENTURE_KEY(
            "net{}kyori",
            "adventure-key",
            "4.23.0",
            "gDd4WEpb+PpfF7q5s7YkUO4TEP2SjyAPFoOSmkMMJmQ=",
            relocate("net{}kyori{}adventure", "adventure"),
            relocateIf("net{}kyori{}examination", "kyori{}examination", LoaderFlag.RELOCATE_ADVENTURE)
    ),
    ADVENTURE_TEXT_SERIALIZER_JSON(
            "net{}kyori",
            "adventure-text-serializer-json",
            "4.23.0",
            "/CvHvwb8Cv5wbWJ493GDcqRoTSGhblLk1iJldxUomE8=",
            relocate("net{}kyori{}option", "kyori{}option"),
            relocateIf("net{}kyori{}adventure", "adventure", LoaderFlag.RELOCATE_ADVENTURE),
            relocateIf("net{}kyori{}examination", "kyori{}examination", LoaderFlag.RELOCATE_ADVENTURE),
            relocateIfNot("net{}kyori{}adventure{}text{}serializer{}json", "adventure{}text{}serializer{}json", LoaderFlag.RELOCATE_ADVENTURE)
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
            "2.8.0",
            "VCzaF+FQa2xME8T0VBjnA96EvBaIECFhypoGusB5k2M=",
            relocate("com{}github{}retrooper{}packetevents", "packetevents{}api"),
            relocate("io{}github{}retrooper{}packetevents", "packetevents{}impl")
            // TODO relocate adventure (?)
    ),
    PACKET_EVENTS_NETTY_COMMON(
            "com{}github{}retrooper",
            "packetevents-netty-common",
            "2.8.0",
            "59peqrKoKCYa3jEBY3i3ZGwq1IAjZ3hhP1b4Il4lNJg=",
            relocate("com{}github{}retrooper{}packetevents", "packetevents{}api"),
            relocate("io{}github{}retrooper{}packetevents", "packetevents{}impl")
    ),
    PACKET_EVENTS_VELOCITY(
            "com{}github{}retrooper",
            "packetevents-velocity",
            "2.8.0",
            "atT9KQYYT5ASIU0wMSJOLrhDt0ehmrTYK4YdP2uxJaw=",
            relocate("com{}github{}retrooper{}packetevents", "packetevents{}api"),
            relocate("io{}github{}retrooper{}packetevents", "packetevents{}impl")
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
        return new ConditionalRelocation(relocateFrom, relocateTo, flag, false);
    }

    private static OptionalRelocation relocateIfNot(String relocateFrom, String relocateTo, LoaderFlag flag) {
        return new ConditionalRelocation(relocateFrom, relocateTo, flag, true);
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
        private final boolean negate;

        @Override
        public Optional<Relocation> relocate(Set<LoaderFlag> loaderFlags) {
            if (loaderFlags.contains(flag) != negate) {
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
