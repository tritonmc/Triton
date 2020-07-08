package com.rexcantor64.triton.language;

import com.rexcantor64.triton.SpigotMLP;
import com.rexcantor64.triton.Triton;
import com.rexcantor64.triton.config.MainConfig.FeatureSyntax;
import com.rexcantor64.triton.language.parser.AdvancedComponent;
import com.rexcantor64.triton.player.LanguagePlayer;
import com.rexcantor64.triton.utils.ComponentUtils;
import lombok.val;
import lombok.var;
import net.md_5.bungee.api.ChatColor;
import net.md_5.bungee.api.chat.BaseComponent;
import net.md_5.bungee.api.chat.ClickEvent;
import net.md_5.bungee.api.chat.TextComponent;
import net.md_5.bungee.api.chat.TranslatableComponent;
import net.md_5.bungee.chat.ComponentSerializer;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class LanguageParser {

    private static Integer[] getPatternIndex(String input, String pattern) {
        int start = -1;
        int contentLength = 0;
        int openedAmount = 0;

        for (int i = 0; i < input.length(); i++) {
            char currentChar = input.charAt(i);
            if (currentChar == '[' && input.length() > i + pattern.length() + 1 && input.substring(i + 1,
                    i + 2 + pattern.length()).equals(pattern + "]")) {
                if (start == -1) start = i;
                openedAmount++;
                i += 1 + pattern.length();
            } else if (currentChar == '[' && input.length() > i + pattern.length() + 2 && input.substring(i + 1,
                    i + 3 + pattern.length()).equals("/" + pattern + "]")) {
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

    public static List<Integer[]> getPatternIndexArray(String input, String pattern) {
        List<Integer[]> result = new ArrayList<>();
        int start = -1;
        int contentLength = 0;
        int openedAmount = 0;

        for (int i = 0; i < input.length(); i++) {
            char currentChar = input.charAt(i);
            if (currentChar == '[' && input.length() > i + pattern.length() + 1 && input.substring(i + 1,
                    i + 2 + pattern.length()).equals(pattern + "]")) {
                if (start == -1) start = i;
                openedAmount++;
                i += 1 + pattern.length();
            } else if (currentChar == '[' && input.length() > i + pattern.length() + 2 && input.substring(i + 1,
                    i + 3 + pattern.length()).equals("/" + pattern + "]")) {
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

    public boolean hasTranslatableComponent(BaseComponent... comps) {
        for (BaseComponent c : comps) {
            if (c instanceof TranslatableComponent)
                return true;
            if (c.getExtra() != null && hasTranslatableComponent(c.getExtra().toArray(new BaseComponent[0])))
                return true;
        }
        return false;
    }

    public String replaceLanguages(String input, LanguagePlayer p, FeatureSyntax syntax) {
        if (input == null) return null;
        return replaceLanguages(input, p.getLang().getName(), syntax);
    }

    public String replaceLanguages(String input, String p, FeatureSyntax syntax) {
        Integer[] i;
        int safeCounter = 0;
        while ((i = getPatternIndex(input, syntax.getLang())) != null) {
            safeCounter++;
            if (safeCounter > 10) {
                Triton.get()
                        .getLogger()
                        .logError("The maximum attempts to translate a message have been exceeded. To prevent the " +
                                "server from crashing, the message might not have been translated. If using " +
                                "BungeeCord, restarting your proxy might fix the problem.");
                break;
            }
            StringBuilder builder = new StringBuilder();
            builder.append(input, 0, i[0]);
            String placeholder = input.substring(i[2], i[3]);
            if (!Triton.get().getConf().getDisabledLine().isEmpty() && ChatColor.stripColor(placeholder)
                    .equals(Triton.get().getConf().getDisabledLine()))
                return null;
            Integer[] argsIndex = getPatternIndex(placeholder, syntax.getArgs());
            if (argsIndex == null) {
                builder.append(SpigotMLP.get().getLanguageManager().getText(p, ChatColor.stripColor(placeholder)));
                builder.append(input.substring(i[1]));
                input = builder.toString();
                continue;
            }
            String code = ChatColor.stripColor(placeholder.substring(0, argsIndex[0]));
            String args = placeholder.substring(argsIndex[2], argsIndex[3]);
            List<Integer[]> argIndexList = getPatternIndexArray(args, syntax.getArg());
            Object[] argList = new Object[argIndexList.size()];
            for (int k = 0; k < argIndexList.size(); k++) {
                Integer[] argIndex = argIndexList.get(k);
                argList[k] = replaceLanguages(args.substring(argIndex[2], argIndex[3]), p, syntax);
                if (argList[k] == null)
                    return null;
            }
            builder.append(SpigotMLP.get().getLanguageManager().getText(p, code, argList));
            builder.append(input.substring(i[1]));
            input = builder.toString();
        }
        return input;
    }

    private void removeMLPLinks(BaseComponent[] baseComponents) {
        for (BaseComponent component : baseComponents) {
            if (component.getClickEvent() != null && component.getClickEvent()
                    .getAction() == ClickEvent.Action.OPEN_URL && !ComponentUtils
                    .isLink(component.getClickEvent().getValue()))
                component.setClickEvent(null);
            if (component.getExtra() != null)
                removeMLPLinks(component.getExtra().toArray(new BaseComponent[0]));
        }
    }

    public BaseComponent[] parseComponent(LanguagePlayer p, FeatureSyntax syntax, BaseComponent... text) {
        return parseComponent(p.getLang().getName(), syntax, text);
    }

    public BaseComponent[] parseComponent(String language, FeatureSyntax syntax, BaseComponent... text) {
        text = ComponentSerializer.parse(ComponentSerializer.toString(text));
        removeMLPLinks(text);
        val advancedComponent = parseAdvancedComponent(language, syntax, AdvancedComponent.fromBaseComponent(text));

        if (advancedComponent == null) return null;
        return advancedComponent.toBaseComponent();
    }

    private AdvancedComponent parseAdvancedComponent(String language, FeatureSyntax syntax,
                                                     AdvancedComponent advancedComponent) {
        var input = advancedComponent.getTextClean();
        input = Triton.get().getLanguageManager().matchPattern(input, language);
        Integer[] i;
        var safeCounter = 0;
        while ((i = getPatternIndex(input, syntax.getLang())) != null) {
            safeCounter++;
            if (safeCounter > 10) {
                Triton.get()
                        .getLogger()
                        .logError("The maximum attempts to translate a message have been exceeded. To prevent the " +
                                "server from crashing, the message might not have been translated. If using " +
                                "BungeeCord, restarting your proxy might fix the problem.");
                break;
            }
            val builder = new StringBuilder();
            builder.append(input, 0, i[0]);
            val placeholder = input.substring(i[2], i[3]);
            val argsIndex = getPatternIndex(placeholder, syntax.getArgs());
            if (argsIndex == null) {
                if (!Triton.get().getConf().getDisabledLine().isEmpty() && ChatColor.stripColor(placeholder)
                        .equals(Triton.get().getConf().getDisabledLine()))
                    return null;
                val result = AdvancedComponent.fromBaseComponent(TextComponent
                        .fromLegacyText(Triton.get().getLanguageManager()
                                .getText(language, ChatColor.stripColor(placeholder))));
                advancedComponent.getComponents().putAll(result.getComponents());
                while (result.getText().startsWith(ChatColor.RESET.toString()))
                    result.setText(result.getText().substring(2));
                builder.append(result.getText());
                builder.append(input.substring(i[1]));
                input = builder.toString();
                continue;
            }
            val code = ChatColor.stripColor(placeholder.substring(0, argsIndex[0]));
            if (!Triton.get().getConf().getDisabledLine().isEmpty() && code
                    .equals(Triton.get().getConf().getDisabledLine()))
                return null;
            val args = placeholder.substring(argsIndex[2], argsIndex[3]);
            val argIndexList = getPatternIndexArray(args, syntax.getArg());
            val argList = new Object[argIndexList.size()];
            for (int k = 0; k < argIndexList.size(); k++) {
                Integer[] argIndex = argIndexList.get(k);
                argList[k] = replaceLanguages(args.substring(argIndex[2], argIndex[3]), language, syntax);
            }
            val result = AdvancedComponent.fromBaseComponent(TextComponent
                    .fromLegacyText(SpigotMLP.get().getLanguageManager().getText(language, code, argList)));
            while (result.getText().startsWith(ChatColor.RESET.toString()))
                result.setText(result.getText().substring(2));
            advancedComponent.getComponents().putAll(result.getComponents());
            builder.append(result.getText());
            builder.append(input.substring(i[1]));
            input = builder.toString();
        }
        advancedComponent.setText(input);
        for (val entry : advancedComponent.getComponents().entrySet())
            advancedComponent.setComponent(entry.getKey(), replaceLanguages(entry.getValue(), language, syntax));

        for (val entry : advancedComponent.getAllTranslatableArguments().entrySet())
            advancedComponent.getAllTranslatableArguments().put(entry.getKey(), entry.getValue().stream()
                    .map(comp -> parseAdvancedComponent(language, syntax, comp)).collect(Collectors.toList()));
        return advancedComponent;
    }
}
