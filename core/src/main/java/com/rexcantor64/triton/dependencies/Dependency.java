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
            "4.17.0",
            "FcjC6xpp2LG8kU9VQ1PajufPB0wFyAdNqYmK7lxw0Ng=",
            relocate("net{}kyori{}adventure", "adventure"),
            relocate("net{}kyori{}examination", "kyori{}examination")
    ),
    ADVENTURE_TEXT_SERIALIZER_GSON(
            "net{}kyori",
            "adventure-text-serializer-gson",
            "4.17.0",
            "YifWv9LWrm0SHT3NpZDTP/g3POpliUhd8/e3Wwnn0hA=",
            relocate("net{}kyori{}option", "kyori{}option"),
            relocateIf("net{}kyori{}adventure", "adventure", LoaderFlag.RELOCATE_ADVENTURE),
            relocateIf("net{}kyori{}examination", "kyori{}examination", LoaderFlag.RELOCATE_ADVENTURE),
            relocateIfNot("net{}kyori{}adventure{}text{}serializer{}gson", "adventure{}text{}serializer{}gson", LoaderFlag.RELOCATE_ADVENTURE),
            relocateIfNot("net{}kyori{}adventure{}text{}serializer{}json", "adventure{}text{}serializer{}json", LoaderFlag.RELOCATE_ADVENTURE)
    ),
    ADVENTURE_TEXT_SERIALIZER_LEGACY(
            "net{}kyori",
            "adventure-text-serializer-legacy",
            "4.17.0",
            "nUlVVC5A61tB2+0XKygmrPHL40BeAC8zYOsupXjVNro=",
            relocateIf("net{}kyori{}adventure", "adventure", LoaderFlag.RELOCATE_ADVENTURE),
            relocateIf("net{}kyori{}examination", "kyori{}examination", LoaderFlag.RELOCATE_ADVENTURE),
            relocateIfNot("net{}kyori{}adventure{}text{}serializer{}legacy", "adventure{}text{}serializer{}legacy", LoaderFlag.RELOCATE_ADVENTURE)
    ),
    ADVENTURE_TEXT_SERIALIZER_PLAIN(
            "net{}kyori",
            "adventure-text-serializer-plain",
            "4.17.0",
            "Im1KPSxCt3J98RoCppjGxkR1OEcF3RK/Asf2l0R5nC4=",
            relocateIf("net{}kyori{}adventure", "adventure", LoaderFlag.RELOCATE_ADVENTURE),
            relocateIf("net{}kyori{}examination", "kyori{}examination", LoaderFlag.RELOCATE_ADVENTURE),
            relocateIfNot("net{}kyori{}adventure{}text{}serializer{}plain", "adventure{}text{}serializer{}plain", LoaderFlag.RELOCATE_ADVENTURE)
    ),
    ADVENTURE_TEXT_SERIALIZER_BUNGEECORD(
            "net{}kyori",
            "adventure-text-serializer-bungeecord",
            "4.3.4",
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
            "4.17.0",
            "g3Kp6bwMCuIdYObObihKgcbJbR7DmPgIglAVxvYCUvU=",
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
            "4.17.0",
            "jlz1cGEtbM7flDrBcWpEneLdWpAge33m9zwCNpNbdm4=",
            relocate("net{}kyori{}adventure", "adventure"),
            relocateIf("net{}kyori{}examination", "kyori{}examination", LoaderFlag.RELOCATE_ADVENTURE)
    ),
    ADVENTURE_TEXT_SERIALIZER_JSON(
            "net{}kyori",
            "adventure-text-serializer-json",
            "4.17.0",
            "bJHT4jsGTXarfBVOvpQPVSIyrheLldIbUN/c/sKSXZY=",
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
            "1.0.0",
            "K95aei1z+hFmoPFmOiLDr30naP/E/qMxd0w8D2IiXRE=",
            relocate("net{}kyori{}option", "kyori{}option")
    ),

    // PacketEvents
    PACKET_EVENTS_API(
            "com{}github{}retrooper",
            "packetevents-api",
            "2.6.0",
            "CdIZKtdDgdpw0uQR/tJOsAfZF66/oRRT0JMqSXCLAWs=",
            relocate("com{}github{}retrooper{}packetevents", "packetevents{}api"),
            relocate("io{}github{}retrooper{}packetevents", "packetevents{}impl")
            // TODO relocate adventure (?)
    ),
    PACKET_EVENTS_NETTY_COMMON(
            "com{}github{}retrooper",
            "packetevents-netty-common",
            "2.6.0",
            "IzZVaD5pI267Q2baymTiFtB9E8119YkhkPZeLXCJLTc=",
            relocate("com{}github{}retrooper{}packetevents", "packetevents{}api"),
            relocate("io{}github{}retrooper{}packetevents", "packetevents{}impl")
    ),
    PACKET_EVENTS_VELOCITY(
            "com{}github{}retrooper",
            "packetevents-velocity",
            "2.6.0",
            "93wEevRi6xSrz+mGVMaHO7sLfsH4AAISsmxNigSU4RA=",
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
