package com.rexcantor64.triton.commands;

import com.rexcantor64.triton.Triton;
import com.rexcantor64.triton.commands.handler.Command;
import com.rexcantor64.triton.commands.handler.CommandEvent;
import com.rexcantor64.triton.language.item.LanguageItem;
import com.rexcantor64.triton.language.item.LanguageSign;
import com.rexcantor64.triton.language.item.SignLocation;
import lombok.val;
import org.bukkit.Bukkit;
import org.bukkit.block.Sign;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class SignCommand implements Command {

    @Override
    public boolean handleCommand(CommandEvent event) {
        val sender = event.getSender();
        val uuid = sender.getUUID();

        if (uuid == null) {
            sender.sendMessage("Only players");
            return true;
        }

        if (event.getEnvironment() != CommandEvent.Environment.SPIGOT)
            return false;

        sender.assertPermission("triton.sign");

        val args = event.getArgs();

        if (args.length == 0 || !Arrays.asList("set", "remove").contains(args[0].toLowerCase())) {
            sender.sendMessageFormatted("help.sign", event.getLabel());
            return true;
        }

        val block = Objects.requireNonNull(Bukkit.getPlayer(sender.getUUID())).getTargetBlock(null, 10);
        if (!(block.getState() instanceof Sign)) {
            sender.sendMessageFormatted("error.not-sign");
            return true;
        }

        if (args[0].equalsIgnoreCase("set")) {
            if (args.length < 2) {
                sender.sendMessageFormatted("help.sign", event.getLabel());
                return true;
            }

            val key = Arrays.stream(args).skip(1).collect(Collectors.joining(" "));
            LanguageItem sign = null;

            // Find a sign group with a matching key
            collectionLoop:
            for (val collection : Triton.get().getStorage().getCollections().values())
                for (val item : collection.getItems())
                    if (item instanceof LanguageSign && item.getKey().equals(key)) {
                        sign = item;
                        break collectionLoop;
                    }

            if (sign == null) {
                sender.sendMessageFormatted("error.sign-not-found", key);
                return true;
            }

            if (Triton.get().getConf().isBungeecord()) {
                Triton.asSpigot().getBridgeManager()
                        .updateSign(block.getWorld().getName(), block.getX(), block.getY(), block
                                .getZ(), key, Bukkit.getPlayer(sender.getUUID()));
            } else {
                SignLocation loc = new SignLocation(block.getWorld().getName(), block
                        .getX(), block.getY(), block.getZ());
                executeSignChange(loc, key);
                Triton.get().refreshPlayers();
            }
            sender.sendMessageFormatted("success.sign-set", key);
        } else if (args[0].equalsIgnoreCase("remove")) {
            if (Triton.get().getConf().isBungeecord()) {
                Triton.asSpigot().getBridgeManager()
                        .updateSign(block.getWorld().getName(), block.getX(), block.getY(), block.getZ(), null, Bukkit
                                .getPlayer(sender.getUUID()));
            } else {
                SignLocation loc = new SignLocation(block.getWorld().getName(), block
                        .getX(), block.getY(), block.getZ());
                executeSignChange(loc, null);
            }
            for (val p2 : Objects.requireNonNull(Bukkit.getPlayer(sender.getUUID())).getWorld().getPlayers())
                Triton.asSpigot().getProtocolLibListener()
                        .resetSign(p2, new SignLocation(block.getWorld().getName(), block.getX(), block
                                .getY(), block.getZ()));
            sender.sendMessageFormatted("success.sign-remove");
        } else {
            sender.sendMessageFormatted("help.sign", event.getLabel());
        }

        return true;
    }

    private void executeSignChange(SignLocation location, String key) {
        val storage = Triton.get().getStorage();
        val changed = storage.toggleLocationForSignGroup(location, key);
        storage.uploadPartiallyToStorage(storage.getCollections(), changed, null);

        Triton.get().getLanguageManager().setup();
    }

    @Override
    public List<String> handleTabCompletion(CommandEvent event) {
        event.getSender().assertPermission("triton.sign");

        val args = event.getArgs();

        if (args.length == 1)
            return Stream.of("set", "remove").filter((v) -> v.startsWith(args[0].toLowerCase()))
                    .collect(Collectors.toList());

        if (args.length == 2 && args[0].equalsIgnoreCase("set"))
            return Triton.get().getLanguageManager().getSignKeys().stream()
                    .filter(key -> key.toLowerCase().startsWith(args[1].toLowerCase()))
                    .collect(Collectors.toList());

        return Collections.emptyList();
    }
}
