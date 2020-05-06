package com.rexcantor64.triton.commands.bungee;

import com.rexcantor64.triton.Triton;
import net.md_5.bungee.api.CommandSender;
import net.md_5.bungee.api.connection.ProxiedPlayer;
import net.md_5.bungee.protocol.packet.Chat;

public class ReloadCMD implements CommandExecutor {

    @Override
    @SuppressWarnings("deprecation")
    public void execute(CommandSender s, String[] args) {
        if (!s.hasPermission("multilanguageplugin.reload") && !s.hasPermission("triton.reload")) {
            s.sendMessage(Triton.get()
                    .getMessage("error.no-permission", "&cNo permission. Permission required: &4%1", "triton.reload"));
            return;
        }

        if (args.length > 1 && s instanceof ProxiedPlayer) {
            String mode = args[1];
            if (mode.equals("server") || mode.equals("s") || mode.equals("all") || mode.equals("a")) {
                ((ProxiedPlayer) s).getServer().unsafe().sendPacket(new Chat("/triton reload"));
                if (mode.equals("server") || mode.equals("s"))
                    return;
            } else if (!mode.equals("bungee") && !mode.equals("b")) {
                s.sendMessage(Triton.get()
                        .getMessage("error.bungee-reload-invalid-mode", "&cMode &4%1&c does not exist. Available " +
                                "modes are 'bungee' (or 'b'), 'server' (or 's') or 'all' (or 'a').", mode));
                return;
            }
        }
        Triton.get().reload();
        s.sendMessage(Triton.get().getMessage("success.bungee-reload", "&aBungeeCord config successfully reloaded."));
    }
}
