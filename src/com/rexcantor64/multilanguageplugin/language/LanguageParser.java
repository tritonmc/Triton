package com.rexcantor64.multilanguageplugin.language;

import com.google.common.collect.Lists;
import com.rexcantor64.multilanguageplugin.SpigotMLP;
import com.rexcantor64.multilanguageplugin.components.api.ChatColor;
import com.rexcantor64.multilanguageplugin.components.api.chat.BaseComponent;
import com.rexcantor64.multilanguageplugin.components.api.chat.HoverEvent;
import com.rexcantor64.multilanguageplugin.components.api.chat.TextComponent;
import com.rexcantor64.multilanguageplugin.utils.ComponentUtils;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class LanguageParser {

    private final Pattern pattern = Pattern
            .compile("\\[" + SpigotMLP.get().getConf().getSyntax() + "\\](.+?)\\[/" + SpigotMLP.get().getConf().getSyntax() + "\\](?!\\[)");
    private final Pattern patternArgs = Pattern.compile(
            "(.+?)\\[" + SpigotMLP.get().getConf().getSyntaxArgs() + "\\](.+?)\\[/" + SpigotMLP.get().getConf().getSyntaxArgs() + "\\]");
    private final Pattern patternArgs2 = Pattern.compile(
            "\\[" + SpigotMLP.get().getConf().getSyntaxArgs() + "\\](.+?)\\[/" + SpigotMLP.get().getConf().getSyntaxArgs() + "\\]");
    private final Pattern patternArg = Pattern
            .compile("\\[" + SpigotMLP.get().getConf().getSyntaxArg() + "\\](.+?)\\[/" + SpigotMLP.get().getConf().getSyntaxArg() + "\\]");
    private final int patternSize = SpigotMLP.get().getConf().getSyntax().length() + 2;
    private final int patternArgSize = SpigotMLP.get().getConf().getSyntaxArg().length() + 2;

    public String replaceLanguages(String input, Player p) {
        Matcher matcher = pattern.matcher(input);
        while (matcher.find()) {
            String a = matcher.group(1);
            Matcher matcherArgs = patternArgs.matcher(a);
            List<String> args = Lists.newArrayList();
            if (matcherArgs.find()) {
                String argsString = matcherArgs.group(2);
                Matcher matcherArg = patternArg.matcher(argsString);
                while (matcherArg.find())
                    args.add(replaceLanguages(matcherArg.group(1), p));
            }
            input = input
                    .replace("[" + SpigotMLP.get().getConf().getSyntax() + "]" + a + "[/" + SpigotMLP.get().getConf().getSyntax() + "]",
                            SpigotMLP.get().getLanguageManager().getText(p,
                                    org.bukkit.ChatColor.stripColor(a.replaceAll("\\[" + SpigotMLP.get().getConf().getSyntaxArgs()
                                            + "\\](.+?)\\[/" + SpigotMLP.get().getConf().getSyntaxArgs() + "\\]", "")),
                                    args.toArray()));
        }
        return input;
    }

    public boolean hasLanguages(String input) {
        return findPlaceholdersIndex(input).size() != 0;
    }

    public String getLastColor(String input) {
        String extraModifiers = "";
        if (input.length() < 2)
            return ChatColor.WHITE + "";
        for (int i = input.length() - 2; i >= 0; i--)
            if (input.charAt(i) == ChatColor.COLOR_CHAR) {
                ChatColor color = ChatColor.getByChar(input.charAt(i + 1));
                switch (color) {
                    case AQUA:
                    case BLACK:
                    case BLUE:
                    case DARK_AQUA:
                    case DARK_BLUE:
                    case DARK_GRAY:
                    case DARK_GREEN:
                    case DARK_PURPLE:
                    case DARK_RED:
                    case GOLD:
                    case GRAY:
                    case GREEN:
                    case LIGHT_PURPLE:
                    case RED:
                    case WHITE:
                    case YELLOW:
                        return color + extraModifiers;
                    case BOLD:
                    case ITALIC:
                    case MAGIC:
                    case STRIKETHROUGH:
                    case UNDERLINE:
                        extraModifiers = color + extraModifiers;
                        break;
                    case RESET:
                        return extraModifiers;
                    default:
                        break;
                }
            }
        return ChatColor.WHITE.toString();
    }

    public String removeFirstColor(String str) {
        if (str == null) return null;
        if (str.length() <= 2) return str;
        return ChatColor.stripColor(str.substring(0, 2)) + str.substring(2);
    }

    private Integer[] findFirstPlaceholdersIndex(String input) {
        Matcher matcher = pattern.matcher(input);
        if (matcher.find())
            return new Integer[]{matcher.start(), matcher.end()};
        return null;
    }

    private Integer[] findArgsIndex(String input) {
        Matcher matcher = patternArgs2.matcher(input);
        if (matcher.find())
            return new Integer[]{matcher.start(), matcher.end()};
        return null;
    }

    private List<Integer[]> findArgIndex(String input) {
        Matcher matcher = patternArg.matcher(input);
        List<Integer[]> indexes = new ArrayList<>();
        while (matcher.find())
            indexes.add(new Integer[]{matcher.start(), matcher.end()});
        return indexes;
    }

    public BaseComponent[] parseSimpleBaseComponent(Player p, BaseComponent[] text) {
        for (BaseComponent a : text)
            if (a instanceof TextComponent)
                ((TextComponent) a).setText(replaceLanguages(((TextComponent) a).getText(), p));
        return text;
    }

    public BaseComponent[] parseTitle(Player p, BaseComponent[] text) {
        return TextComponent.fromLegacyText(replaceLanguages(TextComponent.toLegacyText(text), p));
    }

    private BaseComponent[] translateHoverComponents(Player p, BaseComponent... text) {
        List<BaseComponent> result = new ArrayList<>();
        for (BaseComponent comp : text) {
            if (comp.getHoverEvent() != null && comp.getHoverEvent().getAction() == HoverEvent.Action.SHOW_TEXT)
                comp.setHoverEvent(new HoverEvent(comp.getHoverEvent().getAction(), parseChat(p, comp.getHoverEvent().getValue())));
            result.add(comp);
            if (comp.getExtra() != null)
                for (BaseComponent extra : comp.getExtra())
                    translateHoverComponents(p, extra);
        }
        return result.toArray(new BaseComponent[result.size()]);
    }

    public BaseComponent[] parseChat(Player p, BaseComponent... text) {
        if (text == null) return null;
        List<LanguageMessage> messages = LanguageMessage.fromBaseComponentArray(text);
        int counter = 15;
        indexLoop:
        while (counter > 0) {
            counter--;
            Integer[] i = findFirstPlaceholdersIndex(BaseComponent.toPlainText(text));
            if (i == null) break;
            int index = 0;
            boolean foundStart = false;
            boolean foundEnd = false;
            BaseComponent beforeCache = new TextComponent("");
            BaseComponent compCache = new TextComponent("");
            BaseComponent afterCache = new TextComponent("");
            for (LanguageMessage message : messages) {
                if (foundEnd) {
                    afterCache.addExtra(ComponentUtils.copyFormatting(message.getComponent(), new TextComponent(message.getText())));
                    continue;
                }
                if (!foundStart) {
                    if (index + message.getText().length() <= i[0]) {
                        beforeCache.addExtra(ComponentUtils.copyFormatting(message.getComponent(), new TextComponent(message.getText())));
                        index += message.getText().length();
                        continue;
                    }
                    foundStart = true;
                    if (index + message.getText().length() >= i[1]) {
                        compCache.addExtra(ComponentUtils.copyFormatting(message.getComponent(), new TextComponent(message.getText().substring(i[0] - index, i[1] - index))));
                        beforeCache.addExtra(ComponentUtils.copyFormatting(message.getComponent(), new TextComponent(message.getText().substring(0, i[0] - index))));
                        afterCache.addExtra(ComponentUtils.copyFormatting(message.getComponent(), new TextComponent(message.getText().substring(i[1] - index))));
                        foundEnd = true;
                        continue;
                    }
                    compCache.addExtra(ComponentUtils.copyFormatting(message.getComponent(), new TextComponent(message.getText().substring(i[0] - index))));
                    beforeCache.addExtra(ComponentUtils.copyFormatting(message.getComponent(), new TextComponent(message.getText().substring(0, i[0] - index))));
                } else {
                    if (message.isTranslatableComponent()) continue indexLoop;
                    if (index + message.getText().length() < i[1]) {
                        compCache.addExtra(ComponentUtils.copyFormatting(message.getComponent(), new TextComponent(message.getText())));
                        if (index + message.getText().length() + 1 == i[1]) foundEnd = true;
                    } else {
                        compCache.addExtra(ComponentUtils.copyFormatting(message.getComponent(), new TextComponent(message.getText().substring(0, i[1] - index))));
                        afterCache.addExtra(ComponentUtils.copyFormatting(message.getComponent(), new TextComponent(message.getText().substring(i[1] - index))));
                        foundEnd = true;
                        continue;
                    }
                }
                index += message.getText().length();
            }
            BaseComponent result = new TextComponent("");
            result.addExtra(beforeCache);
            BaseComponent processed = processLanguageComponent(compCache, p);
            result.addExtra(processed);
            result.addExtra(afterCache);
            text = new BaseComponent[]{result};
            messages = LanguageMessage.fromBaseComponentArray(text);
        }

        text = translateHoverComponents(p, text);

        return text;
    }

    private BaseComponent processLanguageComponent(BaseComponent component, Player p) {
        String plainText = BaseComponent.toPlainText(component);
        Integer[] argsIndex = findArgsIndex(plainText);
        if (argsIndex == null) {
            BaseComponent comp = ComponentUtils.copyFormatting(component.getExtra().get(0), new TextComponent(""));
            comp.setExtra(Arrays.asList(TextComponent.fromLegacyText(replaceLanguages(plainText, p))));
            return comp;
        }
        String messageCode = plainText.substring(patternSize, argsIndex[0]);
        List<BaseComponent> arguments = new ArrayList<>();
        for (Integer[] i : findArgIndex(plainText)) {
            BaseComponent cache = new TextComponent("");
            i[0] = i[0] + patternArgSize;
            i[1] = i[1] - patternArgSize - 1;
            int index = 0;
            boolean foundStart = false;
            List<LanguageMessage> messages = LanguageMessage.fromBaseComponentArray(component);
            for (LanguageMessage message : messages) {
                if (!foundStart) {
                    if (index + message.getText().length() <= i[0]) {
                        index += message.getText().length();
                        continue;
                    }
                    foundStart = true;
                    if (index + message.getText().length() >= i[1]) {
                        cache.addExtra(ComponentUtils.copyFormatting(message.getComponent(), new TextComponent(TextComponent.fromLegacyText(ChatColor.translateAlternateColorCodes('&', message.getText().substring(i[0] - index, i[1] - index))))));
                        break;
                    }
                    cache.addExtra(ComponentUtils.copyFormatting(message.getComponent(), new TextComponent(TextComponent.fromLegacyText(ChatColor.translateAlternateColorCodes('&', message.getText().substring(i[0] - index))))));
                } else {
                    if (index + message.getText().length() < i[1]) {
                        cache.addExtra(ComponentUtils.copyFormatting(message.getComponent(), new TextComponent(TextComponent.fromLegacyText(ChatColor.translateAlternateColorCodes('&', message.getText())))));
                    } else {
                        cache.addExtra(ComponentUtils.copyFormatting(message.getComponent(), new TextComponent(TextComponent.fromLegacyText(ChatColor.translateAlternateColorCodes('&', message.getText().substring(0, i[1] - index))))));
                        break;
                    }
                }
                index += message.getText().length();
            }
            arguments.add(cache);
        }
        return replaceArguments(TextComponent.fromLegacyText(SpigotMLP.get().getLanguageManager().getText(p, messageCode)), arguments);
    }

    private BaseComponent replaceArguments(BaseComponent[] base, List<BaseComponent> args) {
        BaseComponent result = new TextComponent("");
        for (LanguageMessage message : LanguageMessage.fromBaseComponentArray(base)) {
            String msg = message.getText();
            TextComponent current = new TextComponent("");
            for (int i = 0; i < msg.length(); i++) {
                if (msg.charAt(i) == '%') {
                    i++;
                    if (Character.isDigit(msg.charAt(i)) && args.size() >= Character.getNumericValue(msg.charAt(i))) {
                        result.addExtra(ComponentUtils.copyFormatting(message.getComponent(), current));
                        current = new TextComponent("");
                        current.addExtra(args.get(Character.getNumericValue(msg.charAt(i) - 1)));
                        result.addExtra(ComponentUtils.copyFormatting(message.getComponent(), current));
                        current = new TextComponent("");
                        continue;
                    }
                    i--;
                }
                current.setText(current.getText() + msg.charAt(i));
            }
            result.addExtra(ComponentUtils.copyFormatting(message.getComponent(), current));
        }
        return result;
    }

}
