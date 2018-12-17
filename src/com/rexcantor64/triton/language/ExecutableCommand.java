package com.rexcantor64.triton.language;

import com.rexcantor64.triton.Triton;
import com.rexcantor64.triton.utils.StringUtils;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class ExecutableCommand {

    private String cmd;
    private Type type;
    private boolean universal = true;
    private List<String> servers = new ArrayList<>();

    private ExecutableCommand(String cmd, Type type, List<String> servers) {
        this.cmd = cmd;
        this.type = type;
        this.universal = false;
        this.servers = servers;
    }

    private ExecutableCommand(String cmd, Type type) {
        this.cmd = cmd;
        this.type = type;
    }

    public static ExecutableCommand parse(String input) {
        String[] inputSplit = input.split(":");
        if (inputSplit.length < 2) {
            Triton.get().logWarning("Language command '%1' doesn't have a type. Using type 'PLAYER' by default.", input);
            return new ExecutableCommand(input, Type.PLAYER);
        }
        Type type = null;
        for (Type t : Type.values()) {
            if (inputSplit[0].equals(t.name())) {
                type = t;
                break;
            }
        }
        if (type == null) {
            Triton.get().logWarning("Language command '%1' has invalid type '%2'. Using the default type 'PLAYER'.", input, inputSplit[0]);
            type = Type.PLAYER;
        }
        if (inputSplit.length < 3 || !Triton.isBungee())
            return new ExecutableCommand(StringUtils.join(":", Arrays.copyOfRange(inputSplit, 1, inputSplit.length)), type);
        if (inputSplit[1].isEmpty())
            return new ExecutableCommand(StringUtils.join(":", Arrays.copyOfRange(inputSplit, 2, inputSplit.length)), type);

        return new ExecutableCommand(StringUtils.join(":", Arrays.copyOfRange(inputSplit, 2, inputSplit.length)), type, Arrays.asList(inputSplit[1].split(",")));
    }

    public String getCmd() {
        return cmd;
    }

    public Type getType() {
        return type;
    }

    public boolean isUniversal() {
        return universal;
    }

    public List<String> getServers() {
        return servers;
    }

    public enum Type {
        PLAYER, SERVER, BUNGEE, BUNGEE_PLAYER
    }

}
