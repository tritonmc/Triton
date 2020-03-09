package com.rexcantor64.triton.language;

import com.rexcantor64.triton.SpigotMLP;
import com.rexcantor64.triton.Triton;
import com.rexcantor64.triton.config.MainConfig.FeatureSyntax;
import com.rexcantor64.triton.language.parser.AdvancedComponent;
import com.rexcantor64.triton.player.LanguagePlayer;
import com.rexcantor64.triton.scoreboard.ScoreboardComponent;
import com.rexcantor64.triton.utils.ComponentUtils;
import net.md_5.bungee.api.ChatColor;
import net.md_5.bungee.api.chat.BaseComponent;
import net.md_5.bungee.api.chat.ClickEvent;
import net.md_5.bungee.api.chat.TextComponent;
import net.md_5.bungee.api.chat.TranslatableComponent;
import net.md_5.bungee.chat.ComponentSerializer;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

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

    private static List<Integer[]> getPatternIndexArray(String input, String pattern) {
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
        text = ComponentSerializer.parse(ComponentSerializer.toString(text));
        removeMLPLinks(text);
        AdvancedComponent advancedComponent = AdvancedComponent.fromBaseComponent(text);
        String input = advancedComponent.getText();
        while (input.startsWith(ChatColor.RESET.toString()))
            input = input.substring(2);
        input = Triton.get().getLanguageManager().matchPattern(input, p);
        Integer[] i;
        int safeCounter = 0;
        while ((i = getPatternIndex(input, syntax.getLang())) != null) {
            safeCounter++;
            if (safeCounter > 10) {
                Triton.get()
                        .logError("The maximum attempts to translate a message have been exceeded. To prevent the " +
                                "server from crashing, the message might not have been translated. If using " +
                                "BungeeCord, restarting your proxy might fix the problem.");
                break;
            }
            StringBuilder builder = new StringBuilder();
            builder.append(input, 0, i[0]);
            String placeholder = input.substring(i[2], i[3]);
            Integer[] argsIndex = getPatternIndex(placeholder, syntax.getArgs());
            if (argsIndex == null) {
                if (!Triton.get().getConf().getDisabledLine().isEmpty() && ChatColor.stripColor(placeholder)
                        .equals(Triton.get().getConf().getDisabledLine()))
                    return null;
                AdvancedComponent result =
                        AdvancedComponent
                                .fromBaseComponent(TextComponent.fromLegacyText(Triton.get().getLanguageManager()
                                        .getText(p, ChatColor.stripColor(placeholder))));
                advancedComponent.getComponents().putAll(result.getComponents());
                while (result.getText().startsWith(ChatColor.RESET.toString()))
                    result.setText(result.getText().substring(2));
                builder.append(result.getText());
                builder.append(input.substring(i[1]));
                input = builder.toString();
                continue;
            }
            String code = ChatColor.stripColor(placeholder.substring(0, argsIndex[0]));
            if (!Triton.get().getConf().getDisabledLine().isEmpty() && code
                    .equals(Triton.get().getConf().getDisabledLine()))
                return null;
            String args = placeholder.substring(argsIndex[2], argsIndex[3]);
            List<Integer[]> argIndexList = getPatternIndexArray(args, syntax.getArg());
            Object[] argList = new Object[argIndexList.size()];
            for (int k = 0; k < argIndexList.size(); k++) {
                Integer[] argIndex = argIndexList.get(k);
                argList[k] = replaceLanguages(args.substring(argIndex[2], argIndex[3]), p, syntax);
            }
            AdvancedComponent result =
                    AdvancedComponent.fromBaseComponent(TextComponent
                            .fromLegacyText(SpigotMLP.get().getLanguageManager().getText(p, code, argList)));
            while (result.getText().startsWith(ChatColor.RESET.toString()))
                result.setText(result.getText().substring(2));
            advancedComponent.getComponents().putAll(result.getComponents());
            builder.append(result.getText());
            builder.append(input.substring(i[1]));
            input = builder.toString();
        }
        advancedComponent.setText(input);
        for (Map.Entry<String, String> entry : advancedComponent.getComponents().entrySet())
            advancedComponent.setComponent(entry.getKey(), replaceLanguages(entry.getValue(), p, syntax));
        return advancedComponent.toBaseComponent();
    }

    /* Scoreboard stuff */

    public List<ScoreboardComponent> toScoreboardComponents(String text) {
        List<ScoreboardComponent> components = new ArrayList<>();
        StringBuilder builder = new StringBuilder();
        ScoreboardComponent current = new ScoreboardComponent();
        for (int i = 0; i < text.length(); i++) {
            char c = text.charAt(i);
            if (c == '§' && i + 1 != text.length()) {
                i++;
                ChatColor cc = ChatColor.getByChar(text.charAt(i));
                if (cc == null) continue;
                switch (cc) {
                    case BOLD:
                        current.setBold(true);
                        break;
                    case ITALIC:
                        current.setItalic(true);
                        break;
                    case MAGIC:
                        current.setMagic(true);
                        break;
                    case STRIKETHROUGH:
                        current.setStrikethrough(true);
                        break;
                    case UNDERLINE:
                        current.setUnderline(true);
                        break;
                    default:
                        current.setText(builder.toString());
                        builder = new StringBuilder();
                        components.add(current);
                        current = new ScoreboardComponent();
                        current.setColor(cc);
                }
            } else
                builder.append(c);
        }
        if (builder.length() > 0) {
            current.setText(builder.toString());
            components.add(current);
        }
        return components;
    }

    public List<ScoreboardComponent> removeDummyColors(List<ScoreboardComponent> scoreboardComponents) {
        List<ScoreboardComponent> result = new ArrayList<>();
        for (ScoreboardComponent comp : scoreboardComponents) {
            if (comp.getText().length() == 0) continue;
            if (result.size() > 0 && comp.equalsFormatting(result.get(result.size() - 1))) {
                result.get(result.size() - 1).appendText(comp.getText());
                continue;
            }
            result.add(comp);
        }
        return result;
    }

    public String scoreboardComponentToString(List<ScoreboardComponent> scoreboardComponents) {
        StringBuilder builder = new StringBuilder();
        for (ScoreboardComponent comp : scoreboardComponents)
            builder.append(comp.getFormatting()).append(comp.getText());
        return builder.toString();
    }

    public String[] toPacketFormatting(List<ScoreboardComponent> components) {
        String toString = scoreboardComponentToString(components);
        if (toString.length() <= 36) return new String[]{"", toString, ""};
        StringBuilder prefix = new StringBuilder();
        StringBuilder entry = new StringBuilder();
        StringBuilder suffix = new StringBuilder();
        int status = 0;
        compLoop:
        for (ScoreboardComponent comp : components) {
            String formatting = comp.getFormatting();
            String text = comp.getText();
            boolean first = true;
            for (int i = 0; i < text.length(); i++) {
                char c = text.charAt(i);
                if (status == 0) {
                    if (first) {
                        first = false;
                        if (prefix.length() == 0 && formatting.equals("§f"))
                            formatting = "";
                        if (prefix.length() + formatting.length() > 16) {
                            int size = 16 - prefix.length();
                            prefix.append(formatting, 0, size);
                            entry.append(formatting.substring(size));
                            status = 1;
                            i--;
                            continue;
                        }
                        prefix.append(formatting);
                    }
                    if (prefix.length() >= 16)
                        status = 1;
                    else
                        prefix.append(c);
                }
                formatting = comp.getFormatting();
                if (status == 1) {
                    if (first) {
                        first = false;
                        if (entry.length() + formatting.length() > 36) {
                            entry.append("1234");
                            status = 2;
                            i--;
                            continue;
                        }
                        entry.append(formatting);
                    }
                    if (entry.length() >= 36) {
                        entry.append("1234");
                        status = 2;
                    } else
                        entry.append(c);
                }
                if (status == 2) {
                    if (first || suffix.length() == 0) {
                        first = false;
                        if (suffix.length() + formatting.length() > 16)
                            break compLoop;
                        suffix.append(formatting);
                    }
                    if (suffix.length() >= 16)
                        break compLoop;
                    suffix.append(c);
                }
            }
        }
        return new String[]{prefix.toString(), entry.toString(), suffix.toString()};
    }
}
