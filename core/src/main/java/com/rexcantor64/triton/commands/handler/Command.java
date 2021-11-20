package com.rexcantor64.triton.commands.handler;

import java.util.List;

public interface Command {

    boolean handleCommand(CommandEvent event);

    List<String> handleTabCompletion(CommandEvent event);


}
