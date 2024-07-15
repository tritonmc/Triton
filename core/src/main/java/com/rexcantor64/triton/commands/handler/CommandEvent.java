package com.rexcantor64.triton.commands.handler;

import com.rexcantor64.triton.Triton;
import com.rexcantor64.triton.plugin.Platform;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Data;
import lombok.RequiredArgsConstructor;

import java.util.Arrays;
import java.util.StringJoiner;

@Data
@Builder(toBuilder = true)
@RequiredArgsConstructor(access = AccessLevel.PUBLIC)
public class CommandEvent {
    private final Sender sender;
    private final String subCommand;
    private final String[] args;
    private final String label;
    /**
     * Whether this event was forwarded from a proxy
     */
    private final boolean forwarded;

    /**
     * Join the sub command with the arguments,
     * in order to recreate the original sub command typed by the player.
     * <p>
     * If the player typed "/triton setlanguage en_GB Player1",
     * this will return "setlanguage en_GB Player1"
     *
     * @return The sub command and arguments joined by a space.
     */
    public String getFullSubCommand() {
        StringJoiner joiner = new StringJoiner(" ");
        joiner.setEmptyValue("");
        if (this.subCommand != null) {
            joiner.add(this.subCommand);
        }
        if (this.args != null) {
            Arrays.stream(this.args).forEach(joiner::add);
        }
        return joiner.toString();
    }

    /**
     * Join the arguments by a space.
     *
     * @return The arguments joined by a space.
     */
    public String argumentsToString() {
        return String.join(" ", this.args);
    }

    public Platform getPlatform() {
        return Triton.platform();
    }

}
