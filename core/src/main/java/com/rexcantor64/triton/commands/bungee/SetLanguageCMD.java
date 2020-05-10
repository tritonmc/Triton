package com.rexcantor64.triton.commands.bungee;

import com.rexcantor64.triton.Triton;
import com.rexcantor64.triton.config.interfaces.Configuration;
import com.rexcantor64.triton.config.interfaces.ConfigurationProvider;
import com.rexcantor64.triton.config.interfaces.YamlConfiguration;
import com.rexcantor64.triton.language.Language;
import net.md_5.bungee.BungeeCord;
import net.md_5.bungee.api.CommandSender;
import net.md_5.bungee.api.connection.ProxiedPlayer;

import java.io.File;
import java.io.IOException;
import java.util.UUID;

public class SetLanguageCMD implements CommandExecutor {

    @Override
    @SuppressWarnings("deprecation")
    public void execute(CommandSender s, String[] args) {
        if (!s.hasPermission("multilanguageplugin.setlanguage") && !s.hasPermission("triton.setlanguage")) {
            s.sendMessage(Triton.get().getMessagesConfig().getMessage("error.no-permission", "triton.setlanguage"));
            return;
        }

        if (args.length == 1) {
            s.sendMessage(Triton.get().getMessagesConfig().getMessage("help.setlanguage", "triton"));
            return;
        }

        ProxiedPlayer target;
        UUID targetOffline = null;
        String langName;

        if (args.length >= 3) {
            langName = args[2];
            if (s.hasPermission("multilanguageplugin.setlanguage.others") || s.hasPermission("triton.setlanguage" +
                    ".others")) {
                target = BungeeCord.getInstance().getPlayer(args[1]);
                if (target == null) {
                    try {
                        targetOffline = UUID.fromString(args[1]);
                    } catch (IllegalArgumentException e) {
                        s.sendMessage(Triton.get().getMessagesConfig()
                                .getMessage("error.player-not-found-use-uuid", args[1]));
                        return;
                    }
                    target = BungeeCord.getInstance().getPlayer(targetOffline);
                }
            } else {
                s.sendMessage(Triton.get().getMessagesConfig()
                        .getMessage("error.no-permission", "triton.setlanguage.others"));
                return;
            }
        } else if (s instanceof ProxiedPlayer) {
            target = (ProxiedPlayer) s;
            langName = args[1];
        } else {
            s.sendMessage("Only Players.");
            return;
        }

        Language lang = Triton.get().getLanguageManager().getLanguageByName(langName, false);
        if (lang == null) {
            s.sendMessage(Triton.get().getMessagesConfig().getMessage("error.lang-not-found", args[1]));
            return;
        }

        if (target != null) {
            Triton.get().getPlayerManager().get(target.getUniqueId()).setLang(lang);
        } else {
            Configuration config = Triton.get().loadYAML("players", "players");
            config.set(targetOffline.toString(), lang.getName());
            try {
                ConfigurationProvider.getProvider(YamlConfiguration.class).save(config,
                        new File(Triton.get().getDataFolder(), "players.yml"));
            } catch (IOException e) {
                Triton.get().getLogger().logError("Failed to save players.yml: %1", e.getMessage());
            }
        }
        if (target == s)
            s.sendMessage(Triton.get().getMessagesConfig().getMessage("success.setlanguage",
                    lang.getDisplayName()));
        else
            s.sendMessage(Triton.get().getMessagesConfig()
                    .getMessage("success.setlanguage-others", target != null ? target.getName() : targetOffline
                                    .toString(),
                            lang.getDisplayName()));
    }
}
