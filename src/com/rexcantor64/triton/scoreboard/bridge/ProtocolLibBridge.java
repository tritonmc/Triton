package com.rexcantor64.triton.scoreboard.bridge;

import com.comphenix.protocol.PacketType;
import com.comphenix.protocol.ProtocolLibrary;
import com.comphenix.protocol.events.PacketContainer;
import com.comphenix.protocol.reflect.StructureModifier;
import com.comphenix.protocol.wrappers.EnumWrappers;
import com.comphenix.protocol.wrappers.WrappedChatComponent;
import com.rexcantor64.triton.Triton;
import com.rexcantor64.triton.packetinterceptor.ProtocolLibListener;
import com.rexcantor64.triton.player.SpigotLanguagePlayer;
import com.rexcantor64.triton.scoreboard.ScoreboardBridge;
import com.rexcantor64.triton.utils.ScoreboardUtils;
import org.bukkit.Bukkit;

import java.util.Collection;
import java.util.Collections;

public class ProtocolLibBridge implements ScoreboardBridge {

    private static final String OBJECTIVE_NAME = "TritonObj";
    private static final String TEAM_PREFIX = "TritonTeam_";
    private final int mcVersion;
    private final SpigotLanguagePlayer owner;

    public ProtocolLibBridge(SpigotLanguagePlayer owner) {
        this.owner = owner;
        String a = Bukkit.getServer().getClass().getPackage().getName();
        mcVersion = Integer.parseInt(a.substring(a.lastIndexOf('.') + 1).split("_")[1]);
    }

    @Override
    public boolean useComponents() {
        return mcVersion >= 13;
    }

    @Override
    public void updateEntryScore(String entry, Integer score) {
        PacketContainer container = ProtocolLibrary.getProtocolManager().createPacket(PacketType.Play.Server.SCOREBOARD_SCORE, true);
        StructureModifier<String> strings = container.getStrings();
        strings.writeSafely(0, entry);
        strings.writeSafely(1, OBJECTIVE_NAME);
        if (score != null)
            container.getIntegers().writeSafely(0, score);
        container.getScoreboardActions().writeSafely(0, score == null ? EnumWrappers.ScoreboardAction.REMOVE : EnumWrappers.ScoreboardAction.CHANGE);
        try {
            ProtocolLibrary.getProtocolManager().sendServerPacket(owner.toBukkit(), container, false);
        } catch (Exception e) {
            Triton.get().logError("Failed to send setEntryScore packet: %1", e.getMessage());
        }
    }

    @Override
    public void updateTeamPrefixSuffix(String prefix, String suffix, int index) {
        PacketContainer container = ProtocolLibrary.getProtocolManager().createPacket(PacketType.Play.Server.SCOREBOARD_TEAM, true);
        StructureModifier<String> strings = container.getStrings();
        strings.writeSafely(0, TEAM_PREFIX + index);
        StructureModifier<Integer> integers = container.getIntegers();
        if (useComponents()) {
            StructureModifier<WrappedChatComponent> components = container.getChatComponents();
            components.writeSafely(0, WrappedChatComponent.fromJson("{\"text\":\"\"}"));
            components.writeSafely(1, WrappedChatComponent.fromJson(prefix));
            components.writeSafely(2, WrappedChatComponent.fromJson(suffix));
            strings.writeSafely(1, "always");
            strings.writeSafely(2, "always");
            container.getEnumModifier(TeamColor.class, 6).writeSafely(0, TeamColor.RESET);
            integers.writeSafely(0, 2);
            integers.writeSafely(1, 0);
        } else {
            strings.writeSafely(1, "");
            strings.writeSafely(2, prefix);
            strings.writeSafely(3, suffix);
            strings.writeSafely(4, "always");
            strings.writeSafely(5, "always");
            integers.writeSafely(0, 21);
            integers.writeSafely(1, 2);
            integers.writeSafely(2, 0);
        }
        try {
            ProtocolLibrary.getProtocolManager().sendServerPacket(owner.toBukkit(), container, false);
        } catch (Exception e) {
            Triton.get().logError("Failed to send updateTeamPrefixSuffix packet: %1", e.getMessage());
        }
    }

    @Override
    public void addEntryToTeam(String entry, int index) {
        PacketContainer container = ProtocolLibrary.getProtocolManager().createPacket(PacketType.Play.Server.SCOREBOARD_TEAM, true);
        container.getIntegers().writeSafely(1, 3);
        container.getStrings().writeSafely(0, TEAM_PREFIX + index);
        container.getSpecificModifier(Collection.class).writeSafely(0, Collections.singleton(entry));
        try {
            ProtocolLibrary.getProtocolManager().sendServerPacket(owner.toBukkit(), container, false);
        } catch (Exception e) {
            Triton.get().logError("Failed to send addEntryToTeam packet: %1", e.getMessage());
        }
    }

    @Override
    public void initializeScoreboard(String title) {
        PacketContainer container = ProtocolLibrary.getProtocolManager().createPacket(PacketType.Play.Server.SCOREBOARD_OBJECTIVE, true);
        container.getStrings().writeSafely(0, OBJECTIVE_NAME);
        if (useComponents())
            container.getChatComponents().writeSafely(0, WrappedChatComponent.fromJson(title));
        else
            container.getStrings().writeSafely(1, title);
        container.getEnumModifier(ProtocolLibListener.EnumScoreboardHealthDisplay.class, 2).writeSafely(0, ProtocolLibListener.EnumScoreboardHealthDisplay.INTEGER);
        container.getIntegers().writeSafely(0, 0);
        try {
            ProtocolLibrary.getProtocolManager().sendServerPacket(owner.toBukkit(), container, false);
        } catch (Exception e) {
            Triton.get().logError("Failed to send addObjective packet: %1", e.getMessage());
        }
        container = ProtocolLibrary.getProtocolManager().createPacket(PacketType.Play.Server.SCOREBOARD_DISPLAY_OBJECTIVE, true);
        container.getStrings().writeSafely(0, OBJECTIVE_NAME);
        container.getIntegers().writeSafely(0, 1);
        try {
            ProtocolLibrary.getProtocolManager().sendServerPacket(owner.toBukkit(), container, false);
        } catch (Exception e) {
            Triton.get().logError("Failed to send displayObjective packet: %1", e.getMessage());
        }
        for (int i = 0; i < 15; i++) {
            container = ProtocolLibrary.getProtocolManager().createPacket(PacketType.Play.Server.SCOREBOARD_TEAM, true);
            StructureModifier<String> strings = container.getStrings();
            strings.writeSafely(0, TEAM_PREFIX + i);
            StructureModifier<Integer> integers = container.getIntegers();
            if (useComponents()) {
                StructureModifier<WrappedChatComponent> components = container.getChatComponents();
                components.writeSafely(0, WrappedChatComponent.fromJson("{\"text\":\"\"}"));
                components.writeSafely(1, WrappedChatComponent.fromJson("{\"text\":\"\"}"));
                components.writeSafely(2, WrappedChatComponent.fromJson("{\"text\":\"\"}"));
                strings.writeSafely(1, "always");
                strings.writeSafely(2, "always");
                container.getEnumModifier(TeamColor.class, 6).writeSafely(0, TeamColor.RESET);
                integers.writeSafely(0, 0);
                integers.writeSafely(1, 0);
            } else {
                strings.writeSafely(1, "");
                strings.writeSafely(2, "");
                strings.writeSafely(3, "");
                strings.writeSafely(4, "always");
                strings.writeSafely(5, "always");
                integers.writeSafely(0, 21);
                integers.writeSafely(1, 0);
                integers.writeSafely(2, 0);
            }
            container.getSpecificModifier(Collection.class).writeSafely(0, useComponents() ? Collections.singleton(ScoreboardUtils.getEntrySuffix(i)) : Collections.emptyList());
            try {
                ProtocolLibrary.getProtocolManager().sendServerPacket(owner.toBukkit(), container, false);
            } catch (Exception e) {
                Triton.get().logError("Failed to send createTeam packet: %1", e.getMessage());
            }
        }
    }

    @Override
    public void updateObjectiveTitle(String title) {
        PacketContainer container = ProtocolLibrary.getProtocolManager().createPacket(PacketType.Play.Server.SCOREBOARD_OBJECTIVE, true);
        container.getStrings().writeSafely(0, OBJECTIVE_NAME);
        if (useComponents())
            container.getChatComponents().writeSafely(0, WrappedChatComponent.fromJson(title));
        else
            container.getStrings().writeSafely(1, title);
        container.getEnumModifier(ProtocolLibListener.EnumScoreboardHealthDisplay.class, 2).writeSafely(0, ProtocolLibListener.EnumScoreboardHealthDisplay.INTEGER);
        container.getIntegers().writeSafely(0, 2);
        try {
            ProtocolLibrary.getProtocolManager().sendServerPacket(owner.toBukkit(), container, false);
        } catch (Exception e) {
            Triton.get().logError("Failed to send updateObjectiveTitle packet: %1", e.getMessage());
        }
    }

    public enum TeamColor {
        BLACK,
        DARK_BLUE,
        DARK_GREEN,
        DARK_AQUA,
        DARK_RED,
        DARK_PURPLE,
        GOLD,
        GRAY,
        DARK_GRAY,
        BLUE,
        GREEN,
        AQUA,
        RED,
        LIGHT_PURPLE,
        YELLOW,
        WHITE,
        OBFUSCATED,
        BOLD,
        STRIKETHROUGH,
        UNDERLINE,
        ITALIC,
        RESET;

    }
}
