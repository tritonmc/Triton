package com.rexcantor64.triton.commands.bungee;

import com.rexcantor64.triton.MultiLanguagePlugin;
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
            ((ProxiedPlayer) s).getServer().unsafe().sendPacket(new Chat("/triton " + join(" ", args)));
            return;
        }
        if (!s.hasPermission("multilanguageplugin.help") && !s.hasPermission("triton.help")) {
            s.sendMessage(MultiLanguagePlugin.get().getMessage("error.no-permission", "&cNo permission. Permission required: &4%1", "triton.help"));
            return;
        }

        for (String str : MultiLanguagePlugin.get().getMessageList("help.menu", "&a---------MultiLanguagePlugin---------", "&6Available commands:", "%1", "&a---------MultiLanguagePlugin---------"))
            if (str.equalsIgnoreCase("%1"))
                for (String command : subCommands.keySet())
                    s.sendMessage(MultiLanguagePlugin.get().getMessage("help.menu-item", "&6/%1 %2", "triton", command));
            else
                s.sendMessage(ChatColor.translateAlternateColorCodes('&', str));
    }

    private String join(String delimiter, String... strings) {
        StringBuilder builder = new StringBuilder();
        for (int i = 0; i < strings.length; i++) {
            if (i != 0) builder.append(delimiter);
            builder.append(strings[i]);
        }
        return builder.toString();
    }
}
