package com.rexcantor64.triton.commands;

import com.google.common.collect.Lists;
import com.rexcantor64.triton.Triton;
import com.rexcantor64.triton.language.item.LanguageItem;
import com.rexcantor64.triton.language.item.LanguageSign;
import com.rexcantor64.triton.language.item.SignLocation;
import lombok.val;
import org.bukkit.block.Sign;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.List;
import java.util.stream.Collectors;

public class SignCMD implements CommandExecutor, TabCompleter {

    @Override
    public boolean onCommand(CommandSender s, Command cmd, String label, String[] args) {
        if (!(s instanceof Player)) {
            s.sendMessage("Only Players.");
            return true;
        }

        val p = (Player) s;

        if (!p.hasPermission("triton.sign")) {
            p.sendMessage(Triton.get().getMessagesConfig().getMessage("error.no-permission", "triton.sign"));
            return true;
        }

        if (args.length < 2 || (!args[1].equalsIgnoreCase("set") && !args[1].equalsIgnoreCase("remove"))) {
            p.sendMessage(Triton.get().getMessagesConfig().getMessage("help.sign", label));
            return true;
        }

        val block = p.getTargetBlock(null, 10);
        if (!(block.getState() instanceof Sign)) {
            p.sendMessage(Triton.get().getMessagesConfig().getMessage("error.not-sign"));
            return true;
        }

        if (args[1].equalsIgnoreCase("set")) {
            if (args.length < 3) {
                p.sendMessage(Triton.get().getMessagesConfig().getMessage("help.sign", label));
                return true;
            }

            LanguageItem sign = null;

            collectionLoop:
            for (val collection : Triton.get().getStorage().getCollections().values())
                for (val item : collection.getItems())
                    if (item instanceof LanguageSign && item.getKey().equals(args[2])) {
                        sign = item;
                        break collectionLoop;
                    }


            if (sign == null) {
                p.sendMessage(Triton.get().getMessagesConfig().getMessage("error.sign-not-found", args[2]));
                return true;
            }

            if (Triton.get().getConf().isBungeecord()) {
                Triton.asSpigot().getBridgeManager()
                        .updateSign(block.getWorld().getName(), block.getX(), block.getY(), block.getZ(), args[2], p);
            } else {
                SignLocation loc = new SignLocation(block.getWorld().getName(), block
                        .getX(), block.getY(), block.getZ());
                executeSignChange(loc, args[2]);
            }
            p.sendMessage(Triton.get().getMessagesConfig().getMessage("success.sign-set", args[2]));
        } else if (args[1].equalsIgnoreCase("remove")) {
            if (Triton.get().getConf().isBungeecord()) {
                Triton.asSpigot().getBridgeManager()
                        .updateSign(block.getWorld().getName(), block.getX(), block.getY(), block.getZ(), null, p);
            } else {
                SignLocation loc = new SignLocation(block.getWorld().getName(), block
                        .getX(), block.getY(), block.getZ());
                executeSignChange(loc, null);
            }
            Triton.asSpigot().getProtocolLibListener()
                    .resetSign(p, new SignLocation(block.getWorld().getName(), block.getX(), block
                            .getY(), block.getZ()));
            p.sendMessage(Triton.get().getMessagesConfig().getMessage("success.sign-remove"));
        } else {
            p.sendMessage(Triton.get().getMessagesConfig().getMessage("help.sign", label));
        }

        return true;
    }

    private void executeSignChange(SignLocation location, String key) {
        val storage = Triton.get().getStorage();
        storage.toggleLocationForSignGroup(location, key);
        storage.uploadToStorage(storage.getCollections());

        Triton.get().getLanguageManager().setup();
    }

    @Override
    public List<String> onTabComplete(CommandSender s, Command cmd, String label, String[] args) {
        List<String> tab = Lists.newArrayList();
        if (!s.hasPermission("triton.sign"))
            return tab;

        if (args.length == 2)
            for (String str : new String[]{"set", "remove"})
                if (str.startsWith(args[1].toLowerCase()))
                    tab.add(str);

        if (args.length == 3 && args[1].equals("set"))
            return Triton.get().getLanguageManager().getSignKeys().stream()
                    .filter(key -> key.toLowerCase().startsWith(args[1].toLowerCase()))
                    .collect(Collectors.toList());
        return tab;
    }

}
