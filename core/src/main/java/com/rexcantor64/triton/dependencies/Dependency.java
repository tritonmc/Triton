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
            "4.26.1",
            "VR5Ta56oaPMOcseQCjCbNRJO59SIn6OzrtCRApl1GiY=",
            relocate("net{}kyori{}adventure", "adventure"),
            relocate("net{}kyori{}examination", "kyori{}examination")
    ),
    ADVENTURE_TEXT_SERIALIZER_GSON(
            "net{}kyori",
            "adventure-text-serializer-gson",
            "4.26.1",
            "5KkI3txKy0MFCD2RbTYt3Csh7PRS1XegvmZSgL3a6fw=",
            relocateIf("net{}kyori{}adventure", "adventure", LoaderFlag.VENDOR_ADVENTURE),
            relocateIf("net{}kyori{}examination", "kyori{}examination", LoaderFlag.VENDOR_ADVENTURE),
            relocate("net{}kyori{}option", "kyori{}option"),
            relocate("net{}kyori{}adventure{}text{}serializer{}gson", "adventure{}text{}serializer{}gson"),
            relocate("net{}kyori{}adventure{}text{}serializer{}json", "adventure{}text{}serializer{}json")
    ),
    ADVENTURE_TEXT_SERIALIZER_LEGACY(
            "net{}kyori",
            "adventure-text-serializer-legacy",
            "4.26.1",
            "chEHvCE1ckVN8b++Q426Yw4wRXVQx1C3oVSzXcMmSqg=",
            relocateIf("net{}kyori{}adventure", "adventure", LoaderFlag.VENDOR_ADVENTURE),
            relocateIf("net{}kyori{}examination", "kyori{}examination", LoaderFlag.VENDOR_ADVENTURE),
            relocate("net{}kyori{}adventure{}text{}serializer{}legacy", "adventure{}text{}serializer{}legacy")
    ),
    ADVENTURE_TEXT_SERIALIZER_PLAIN(
            "net{}kyori",
            "adventure-text-serializer-plain",
            "4.26.1",
            "Obm/5XkPZFYF/1RkOCwOBiR29I+b+yVIgB8nXxL69AU=",
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
            "4.26.1",
            "HUNFHpr0cyUtyK8+gIQjjVzmitQ68OO3OD6z1LY//58=",
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
            "4.26.1",
            "7sFy1j23e0Drer7rJfZe7eqJvTAmTQV7aLEv7Lcxvl4=",
            relocate("net{}kyori{}adventure", "adventure"),
            relocate("net{}kyori{}examination", "kyori{}examination")
    ),
    ADVENTURE_NBT(
            "net{}kyori",
            "adventure-nbt",
            "4.26.1",
            "8m72v/X83YF5ArTf01NWHZjLExtOy2c0H/AO8u0TNvg=",
            new SimpleRelocation(relocateInner("net{}kyori{}adventure{}nbt", "adventure{}nbt", null, Collections.singleton("net{}kyori{}adventure{}nbt{}api{}*"))),
            relocateIf("net{}kyori{}adventure", "adventure", LoaderFlag.VENDOR_ADVENTURE),
            relocateIf("net{}kyori{}examination", "kyori{}examination", LoaderFlag.VENDOR_ADVENTURE)
    ),
    ADVENTURE_TEXT_SERIALIZER_JSON(
            "net{}kyori",
            "adventure-text-serializer-json",
            "4.26.1",
            "VcZLQzPV0paKASW48p2ufPuhU5LV+Z94266nEcsNTcI=",
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
            "2.13.0",
            "x/64iHLZBlA31FGQ3i6W9p6+A58umyix50ZpWVEH7o8=",
            relocate("com{}github{}retrooper{}packetevents", "packetevents{}api"),
            relocate("io{}github{}retrooper{}packetevents", "packetevents{}impl"),
            relocateIf("net{}kyori{}adventure", "adventure", LoaderFlag.VENDOR_ADVENTURE),
            relocateIf("net{}kyori{}examination", "kyori{}examination", LoaderFlag.VENDOR_ADVENTURE),
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
            "2.13.0",
            "ibRP/qBRoSQq7k4HA89XIsQ29zVK2dUhSMjW7w0K6EM=",
            relocate("com{}github{}retrooper{}packetevents", "packetevents{}api"),
            relocate("io{}github{}retrooper{}packetevents", "packetevents{}impl"),
            relocateIf("net{}kyori{}adventure", "adventure", LoaderFlag.VENDOR_ADVENTURE),
            relocateIf("net{}kyori{}examination", "kyori{}examination", LoaderFlag.VENDOR_ADVENTURE),
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
            "2.13.0",
            "LZP6ryynJN9s07c8vjxcS5Nh7lJ/FvR4xwdU6swMlpw=",
            relocate("com{}github{}retrooper{}packetevents", "packetevents{}api"),
            relocate("io{}github{}retrooper{}packetevents", "packetevents{}impl"),
            relocateIf("net{}kyori{}adventure", "adventure", LoaderFlag.VENDOR_ADVENTURE),
            relocateIf("net{}kyori{}examination", "kyori{}examination", LoaderFlag.VENDOR_ADVENTURE),
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
            "2.13.0",
            "8GSB2Cnkrts29i5wJ0NHyWLWIEPoadialV3Jk1nUDYA=",
            relocate("com{}github{}retrooper{}packetevents", "packetevents{}api"),
            relocate("io{}github{}retrooper{}packetevents", "packetevents{}impl"),
            relocateIf("net{}kyori{}adventure", "adventure", LoaderFlag.VENDOR_ADVENTURE),
            relocateIf("net{}kyori{}examination", "kyori{}examination", LoaderFlag.VENDOR_ADVENTURE),
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
            "2.13.0",
            "XdFvpzbQRZiYe/7Q9U+yjg3mCjUeudH5U56Fs3KmgDE=",
            relocate("com{}github{}retrooper{}packetevents", "packetevents{}api"),
            relocate("io{}github{}retrooper{}packetevents", "packetevents{}impl"),
            relocateIf("net{}kyori{}adventure", "adventure", LoaderFlag.VENDOR_ADVENTURE),
            relocateIf("net{}kyori{}examination", "kyori{}examination", LoaderFlag.VENDOR_ADVENTURE),
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
