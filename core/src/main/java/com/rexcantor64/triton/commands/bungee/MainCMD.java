package com.rexcantor64.triton.commands.bungee;

import com.rexcantor64.triton.Triton;
import com.rexcantor64.triton.utils.StringUtils;
import net.md_5.bungee.api.ChatColor;
import net.md_5.bungee.api.CommandSender;
import net.md_5.bungee.api.connection.ProxiedPlayer;
import net.md_5.bungee.api.plugin.Command;
import net.md_5.bungee.protocol.packet.Chat;

import java.util.HashMap;
import java.util.Map;

public class MainCMD extends Command {

    private HashMap<String, CommandExecutor> subCommands = new HashMap<>();

    public MainCMD() {
        super("triton", null, "mlp", "ml", "multilanguage", "language", "lang", "multilanguageplugin");
        subCommands.put("setlanguage", new SetLanguageCMD());
        subCommands.put("reload", new ReloadCMD());
        subCommands.put("database", new DatabaseCMD());
    }

    @Override
    @SuppressWarnings("deprecation")
    public void execute(CommandSender s, String[] args) {
        if (args.length != 0)
            for (Map.Entry<String, CommandExecutor> entry : subCommands.entrySet())
                if (entry.getKey().equalsIgnoreCase(args[0])) {
                    entry.getValue().execute(s, args);
                    return;
                }
        if (s instanceof ProxiedPlayer) {
            ((ProxiedPlayer) s).getServer().unsafe().sendPacket(new Chat("/triton " + StringUtils.join(" ", args)));
            return;
        }
        if (!s.hasPermission("multilanguageplugin.help") && !s.hasPermission("triton.help")) {
            s.sendMessage(Triton.get().getMessagesConfig()
                    .getMessage("error.no-permission", "triton.help"));
            return;
        }

        for (String str : Triton.get().getMessagesConfig().getMessageList("help.menu"))
            if (str.equalsIgnoreCase("%1"))
                for (String command : subCommands.keySet())
                    s.sendMessage(Triton.get().getMessagesConfig()
                            .getMessage("help.menu-item", "triton", command, Triton.get().getMessagesConfig()
                                    .getMessage("command." + command)));
            else
                s.sendMessage(ChatColor.translateAlternateColorCodes('&', str));
    }

}
