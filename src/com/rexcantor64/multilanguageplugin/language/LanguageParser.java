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

    private final String pattern = SpigotMLP.get().getConf().getSyntax();
    private final String patternArgs = SpigotMLP.get().getConf().getSyntaxArgs();
    private final String patternArg = SpigotMLP.get().getConf().getSyntaxArg();
    private final int patternSize = SpigotMLP.get().getConf().getSyntax().length() + 2;
    private final int patternArgSize = SpigotMLP.get().getConf().getSyntaxArg().length() + 2;

    public String replaceLanguages(String input, Player p) {
        Integer[] i;
        while ((i = getPatternIndex(input, pattern)) != null) {
            StringBuilder builder = new StringBuilder();
            builder.append(input.substring(0, i[0]));
            String placeholder = input.substring(i[2], i[3]);
            Integer[] argsIndex = getPatternIndex(placeholder, patternArgs);
            if (argsIndex == null) {
                builder.append(SpigotMLP.get().getLanguageManager().getText(p, placeholder));
                builder.append(input.substring(i[1]));
                input = builder.toString();
                continue;
            }
            String code = placeholder.substring(0, argsIndex[0]);
            String args = placeholder.substring(argsIndex[2], argsIndex[3]);
            List<Integer[]> argIndexList = getPatternIndexArray(args, patternArg);
            Object[] argList = new Object[argIndexList.size()];
            for (int k = 0; k < argIndexList.size(); k++) {
                Integer[] argIndex = argIndexList.get(k);
                argList[k] = replaceLanguages(args.substring(argIndex[2], argIndex[3]), p);
            }
            builder.append(SpigotMLP.get().getLanguageManager().getText(p, code, argList));
            builder.append(input.substring(i[1]));
            input = builder.toString();
        }
        return input;
    }

    public boolean hasLanguages(String input) {
        return getPatternIndex(input, pattern) != null;
    }

    private static Integer[] getPatternIndex(String input, String pattern) {
        int start = -1;
        int contentLength = 0;
        int openedAmount = 0;

        for (int i = 0; i < input.length(); i++) {
            char currentChar = input.charAt(i);
            if (currentChar == '[' && input.length() > i + pattern.length() + 1 && input.substring(i + 1, i + 2 + pattern.length()).equals(pattern + "]")) {
                if (start == -1) start = i;
                openedAmount++;
                i += 1 + pattern.length();
            } else if (currentChar == '[' && input.length() > i + pattern.length() + 2 && input.substring(i + 1, i + 3 + pattern.length()).equals("/" + pattern + "]")) {
                openedAmount--;
                if (openedAmount == 0) {
                    if (contentLength == 0) {
                        start = -1;
                        continue;
                    }
                    return new Integer[]{start, i + 3 + pattern.length(), start + pattern.length() + 2, i};
                }
            } else if (start != -1)
                contentLength++;
        }
        return null;
    }

    private static List<Integer[]> getPatternIndexArray(String input, String pattern) {
        List<Integer[]> result = new ArrayList<>();
        int start = -1;
        int contentLength = 0;
        int openedAmount = 0;

        for (int i = 0; i < input.length(); i++) {
            char currentChar = input.charAt(i);
            if (currentChar == '[' && input.length() > i + pattern.length() + 1 && input.substring(i + 1, i + 2 + pattern.length()).equals(pattern + "]")) {
                if (start == -1) start = i;
                openedAmount++;
                i += 1 + pattern.length();
            } else if (currentChar == '[' && input.length() > i + pattern.length() + 2 && input.substring(i + 1, i + 3 + pattern.length()).equals("/" + pattern + "]")) {
                openedAmount--;
                if (openedAmount == 0) {
                    if (contentLength == 0) {
                        start = -1;
                        continue;
                    }
                    result.add(new Integer[]{start, i + 3 + pattern.length(), start + pattern.length() + 2, i});
                    start = -1;
                    contentLength = 0;
                }
            } else if (start != -1)
                contentLength++;
        }
        return result;
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

    public BaseComponent getLastColorComponent(String input) {
        BaseComponent comp = new TextComponent("");
        if (input.length() < 2)
            return comp;
        for (int i = input.length() - 2; i >= 0; i--)
            if (input.charAt(i) == com.rexcantor64.multilanguageplugin.components.api.ChatColor.COLOR_CHAR) {
                com.rexcantor64.multilanguageplugin.components.api.ChatColor color = com.rexcantor64.multilanguageplugin.components.api.ChatColor.getByChar(input.charAt(i + 1));
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
                        comp.setColor(color);
                        return comp;
                    case BOLD:
                        comp.setBold(true);
                        break;
                    case ITALIC:
                        comp.setItalic(true);
                        break;
                    case MAGIC:
                        comp.setObfuscated(true);
                        break;
                    case STRIKETHROUGH:
                        comp.setStrikethrough(true);
                        break;
                    case UNDERLINE:
                        comp.setUnderlined(true);
                        break;
                    case RESET:
                        return comp;
                    default:
                        break;

                }
            }
        return comp;
    }

    public String removeFirstColor(String str) {
        if (str == null) return null;
        if (str.length() <= 2) return str;
        return ChatColor.stripColor(str.substring(0, 2)) + str.substring(2);
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
            Integer[] i = getPatternIndex(BaseComponent.toPlainText(text), pattern);
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
            if (processed == null) return null;
            result.addExtra(processed);

            BaseComponent afterCacheWrapper = getLastColorComponent(BaseComponent.toLegacyText(processed));
            afterCacheWrapper.addExtra(afterCache);
            result.addExtra(afterCacheWrapper);
            text = new BaseComponent[]{result};
            messages = LanguageMessage.fromBaseComponentArray(text);
        }

        text = translateHoverComponents(p, text);

        return text;
    }

    private BaseComponent processLanguageComponent(BaseComponent component, Player p) {
        String plainText = BaseComponent.toPlainText(component);
        Integer[] argsIndex = getPatternIndex(plainText, patternArgs);
        if (argsIndex == null) {
            if (!SpigotMLP.get().getConf().getDisabledLine().isEmpty() && plainText.substring(patternSize, plainText.length() - patternSize - 1).equals(SpigotMLP.get().getConf().getDisabledLine()))
                return null;
            BaseComponent comp = ComponentUtils.copyFormatting(component.getExtra().get(0), new TextComponent(""));
            comp.setExtra(Arrays.asList(TextComponent.fromLegacyText(replaceLanguages(plainText, p))));
            return comp;
        }
        String messageCode = plainText.substring(patternSize, argsIndex[0]);
        if (!SpigotMLP.get().getConf().getDisabledLine().isEmpty() && messageCode.equals(SpigotMLP.get().getConf().getDisabledLine()))
            return null;
        List<BaseComponent> arguments = new ArrayList<>();
        for (Integer[] i : getPatternIndexArray(plainText, patternArg)) {
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
