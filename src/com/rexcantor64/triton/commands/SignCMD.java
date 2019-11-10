package com.rexcantor64.triton.commands;

import com.google.common.collect.Lists;
import com.google.common.io.ByteArrayDataOutput;
import com.google.common.io.ByteStreams;
import com.rexcantor64.triton.Triton;
import com.rexcantor64.triton.language.item.LanguageItem;
import com.rexcantor64.triton.language.item.LanguageSign;
import com.rexcantor64.triton.utils.LocationUtils;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.Sign;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

public class SignCMD implements CommandExecutor, TabCompleter {

    @Override
    public boolean onCommand(CommandSender s, Command cmd, String label, String[] args) {
        if (!(s instanceof Player)) {
            s.sendMessage("Only Players.");
            return true;
        }

        Player p = (Player) s;

        if (!p.hasPermission("triton.sign")) {
            p.sendMessage(Triton.get()
                    .getMessage("error.no-permission", "&cNo permission. Permission required: &4%1", "triton.sign"));
            return true;
        }

        if (args.length < 2 || (!args[1].equalsIgnoreCase("set") && !args[1].equalsIgnoreCase("remove"))) {
            p.sendMessage(Triton.get()
                    .getMessage("help.sign", "&cUse &4/%1 sign <set|remove> <group key (set only)>&c to add a sign to" +
                            " a group.", label));
            return true;
        }

        Block block = p.getTargetBlock((Set<Material>) null, 10);
        if (!(block.getState() instanceof Sign)) {
            p.sendMessage(Triton.get().getMessage("error.not-sign", "&cYou're not looking at a sign."));
            return true;
        }

        if (args[1].equalsIgnoreCase("set")) {
            if (args.length < 3) {
                p.sendMessage(Triton.get()
                        .getMessage("help.sign", "&cUse &4/%1 sign <set|remove> <group key (set only)>&c to add a " +
                                "sign to a group.", label));
                return true;
            }
            LanguageSign sign = null;

            for (LanguageItem li : Triton.get().getLanguageManager().getAllItems(LanguageItem.LanguageItemType.SIGN)) {
                if (li.getKey().equals(args[2])) {
                    sign = (LanguageSign) li;
                    break;
                }
            }

            if (sign == null) {
                p.sendMessage(Triton.get()
                        .getMessage("error.sign-not-found", "&cSign group %1 not found! Note: It's case sensitive. " +
                                "Use TAB to all the available options.", args[2]));
            }

            if (Triton.get().getConf().isBungeecord()) {
                ByteArrayDataOutput out = ByteStreams.newDataOutput();
                // Action (1): sign management
                out.writeByte(1);
                out.writeUTF(block.getWorld().getName());
                out.writeInt(block.getX());
                out.writeInt(block.getY());
                out.writeInt(block.getZ());
                out.writeBoolean(true); // Set
                out.writeUTF(args[2]);
                p.sendPluginMessage(Triton.get().getLoader().asSpigot(), "triton:main", out.toByteArray());
            } else {
                LanguageSign.SignLocation loc = new LanguageSign.SignLocation(block.getWorld().getName(), block
                        .getX(), block.getY(), block.getZ());
                executeSignChange(true, args[2], loc);
            }
        } else {
            if (Triton.get().getConf().isBungeecord()) {
                ByteArrayDataOutput out = ByteStreams.newDataOutput();
                // Action (1): sign management
                out.writeByte(1);
                out.writeUTF(block.getWorld().getName());
                out.writeInt(block.getX());
                out.writeInt(block.getY());
                out.writeInt(block.getZ());
                out.writeBoolean(false); // Remove
                p.sendPluginMessage(Triton.get().getLoader().asSpigot(), "triton:main", out.toByteArray());
            } else {
                LanguageSign.SignLocation loc = new LanguageSign.SignLocation(block.getWorld().getName(), block
                        .getX(), block.getY(), block.getZ());
                executeSignChange(false, null, loc);
            }
            Triton.asSpigot().getProtocolLibListener()
                    .resetSign(p, new LanguageSign.SignLocation(block.getWorld().getName(), block.getX(), block
                            .getY(), block.getZ()));
        }


        return true;
    }

    private void executeSignChange(boolean add, String key, LanguageSign.SignLocation loc) {
        List<String> remove = new ArrayList<>();
        for (LanguageItem li : Triton.get().getLanguageManager().getAllItems(LanguageItem.LanguageItemType.SIGN))
            if (((LanguageSign) li).hasLocation(loc, false)) remove.add(li.getKey());
        JSONArray raw = Triton.get().getLanguageConfig().getRaw();
        for (int i = 0; i < raw.length(); i++) {
            JSONObject obj = raw.optJSONObject(i);
            if (obj == null || !obj.optString("type", "text").equals("sign")) continue;
            if (!remove.isEmpty() && remove.contains(obj.optString("key"))) {
                JSONArray locs = obj.optJSONArray("locations");
                if (locs == null) continue;
                for (int k = 0; k < locs.length(); k++) {
                    JSONObject l = locs.optJSONObject(k);
                    if (l != null && l.optString("world").equals(loc.getWorld()) && l.optInt("x") == loc.getX() && l
                            .optInt("y") == loc.getY() && l.optInt("z") == loc.getZ())
                        locs.remove(k--);
                }
                obj.put("locations", locs);
                raw.put(i, obj);
            }
            if (add && obj.optString("key").equals(key)) {
                JSONArray locs = obj.optJSONArray("locations");
                if (locs == null) locs = new JSONArray();
                locs.put(LocationUtils.locationToJSON(loc.getX(), loc.getY(), loc.getZ(), loc.getWorld()));
                obj.put("locations", locs);
                raw.put(i, obj);
            }
        }
        Triton.get().getLanguageConfig().saveFromRaw(raw);
        Triton.get().reload();
    }

    @Override
    public List<String> onTabComplete(CommandSender s, Command cmd, String label, String[] args) {
        List<String> tab = Lists.newArrayList();
        if (!s.hasPermission("triton.sign"))
            return tab;
        if (args.length == 2)
            for (String str : new String[]{"set", "remove"})
                if (str.startsWith(args[1]))
                    tab.add(str);
        if (args.length == 3)
            for (LanguageItem item : Triton.get().getLanguageManager().getAllItems(LanguageItem.LanguageItemType.SIGN))
                if (item.getKey().startsWith(args[2]))
                    tab.add(item.getKey());
        return tab;
    }

}
