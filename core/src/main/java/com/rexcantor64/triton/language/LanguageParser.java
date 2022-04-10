package com.rexcantor64.triton.language;

import com.google.gson.JsonParseException;
import com.rexcantor64.triton.SpigotMLP;
import com.rexcantor64.triton.Triton;
import com.rexcantor64.triton.api.config.FeatureSyntax;
import com.rexcantor64.triton.language.parser.AdvancedComponent;
import com.rexcantor64.triton.player.LanguagePlayer;
import com.rexcantor64.triton.player.SpigotLanguagePlayer;
import com.rexcantor64.triton.utils.ComponentUtils;
import com.rexcantor64.triton.wrappers.legacy.HoverComponentWrapper;
import lombok.val;
import lombok.var;
import me.clip.placeholderapi.PlaceholderAPI;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.serializer.gson.GsonComponentSerializer;
import net.md_5.bungee.api.ChatColor;
import net.md_5.bungee.api.chat.BaseComponent;
import net.md_5.bungee.api.chat.ClickEvent;
import net.md_5.bungee.api.chat.HoverEvent;
import net.md_5.bungee.api.chat.TextComponent;
import net.md_5.bungee.chat.ComponentSerializer;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class LanguageParser implements com.rexcantor64.triton.api.language.LanguageParser {

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
                    result.add(new Integer[]{start, i + 3 + pattern.length(), start + pattern.length() + 2, i});
                    start = -1;
                }
            }
        }
        return result;
    }

    public String parseString(String language, FeatureSyntax syntax, String input) {
        return replaceLanguages(input, language, syntax);
    }

    public String replaceLanguages(String input, LanguagePlayer p, FeatureSyntax syntax) {
        if (input == null) return null;
        return replaceLanguages(input, p.getLang().getName(), syntax);
    }

    public String replaceLanguages(String input, String language, FeatureSyntax syntax) {
        if (input == null) return null;
        Integer[] i;
        int safeCounter = 0;
        while ((i = getPatternIndex(input, syntax.getLang())) != null) {
            safeCounter++;
            if (safeCounter > Triton.get().getConfig().getMaxPlaceholdersInMessage()) {
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
                builder.append(SpigotMLP.get().getLanguageManager()
                        .getText(language, ChatColor.stripColor(placeholder)));
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
                argList[k] = replaceLanguages(args.substring(argIndex[2], argIndex[3]), language, syntax);
                if (argList[k] == null)
                    return null;
            }
            builder.append(SpigotMLP.get().getLanguageManager().getText(language, code, argList));
            builder.append(input.substring(i[1]));
            input = builder.toString();
        }
        return input;
    }

    private List<BaseComponent> removeTritonLinks(BaseComponent... baseComponents) {
        val result = new ArrayList<BaseComponent>();
        for (val component : baseComponents) {
            if (component == null) continue;
            if (component.getClickEvent() != null && component.getClickEvent()
                    .getAction() == ClickEvent.Action.OPEN_URL && !ComponentUtils
                    .isLink(component.getClickEvent().getValue()))
                component.setClickEvent(null);
            if (component.getExtra() != null)
                component.setExtra(removeTritonLinks(component.getExtra().toArray(new BaseComponent[0])));

            val lastComp = result.size() > 0 ? result.get(result.size() - 1) : null;
            if (lastComp instanceof TextComponent &&
                    component instanceof TextComponent &&
                    !ComponentUtils.hasExtra(lastComp) &&
                    !ComponentUtils.hasExtra(component) &&
                    ComponentUtils.haveSameFormatting(lastComp, component)
            ) {
                val lastTextComp = (TextComponent) lastComp;
                val textComp = (TextComponent) component;
                lastTextComp.setText(lastTextComp.getText() + textComp.getText());
                continue;
            }
            result.add(component);
        }
        return result;
    }

    public BaseComponent[] parseComponent(LanguagePlayer p, FeatureSyntax syntax, BaseComponent... text) {
        return parseComponent(p.getLang().getName(), syntax, p, text);
    }

    public BaseComponent[] parseComponent(String language, FeatureSyntax syntax, BaseComponent... text) {
        return parseComponent(language, syntax, null, text);
    }

    public BaseComponent[] parseComponent(String language, FeatureSyntax syntax, LanguagePlayer contextPlayer, BaseComponent... text) {
        text = ComponentSerializer.parse(ComponentSerializer.toString(text));
        text = removeTritonLinks(text).toArray(new BaseComponent[0]);
        val advancedComponent = parseAdvancedComponent(language, syntax, AdvancedComponent.fromBaseComponent(text), contextPlayer);

        if (advancedComponent == null) return null;
        return advancedComponent.toBaseComponent();
    }

    private AdvancedComponent parseAdvancedComponent(String language, FeatureSyntax syntax,
                                                     AdvancedComponent advancedComponent,
                                                     LanguagePlayer contextPlayer) {
        String input = advancedComponent.getTextClean();
        input = Triton.get().getLanguageManager().matchPattern(input, language);
        Integer[] i;
        int safeCounter = 0;
        while ((i = getPatternIndex(input, syntax.getLang())) != null) {
            safeCounter++;
            if (safeCounter > Triton.get().getConfig().getMaxPlaceholdersInMessage()) {
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
                val code = AdvancedComponent.stripFormatting(placeholder);
                if (!Triton.get().getConf().getDisabledLine().isEmpty() &&
                        code.equals(Triton.get().getConf().getDisabledLine()))
                    return null;
                val result = parseTritonTranslation(Triton.get().getLanguageManager().getText(language, code), contextPlayer);
                advancedComponent.inheritSpecialComponents(result);
                builder.append(result.getTextClean());
                builder.append(input.substring(i[1]));
                input = builder.toString();
                continue;
            }
            val code = AdvancedComponent.stripFormatting(placeholder.substring(0, argsIndex[0]));
            if (!Triton.get().getConf().getDisabledLine().isEmpty() && code
                    .equals(Triton.get().getConf().getDisabledLine()))
                return null;
            val args = placeholder.substring(argsIndex[2], argsIndex[3]);
            val argIndexList = getPatternIndexArray(args, syntax.getArg());
            val argList = new Object[argIndexList.size()];
            for (int k = 0; k < argIndexList.size(); k++) {
                Integer[] argIndex = argIndexList.get(k);
                AdvancedComponent argAdvancedComponent = AdvancedComponent.fromString(args.substring(argIndex[2], argIndex[3]));
                argAdvancedComponent = parseAdvancedComponent(language, syntax, argAdvancedComponent, contextPlayer);
                if (argAdvancedComponent == null) {
                    argList[k] = "";
                    continue;
                }
                argList[k] = argAdvancedComponent.getText();
                advancedComponent.inheritSpecialComponents(argAdvancedComponent);
            }
            val result = parseTritonTranslation(SpigotMLP.get().getLanguageManager().getText(language, code, argList), contextPlayer);
            advancedComponent.inheritSpecialComponents(result);
            builder.append(result.getTextClean());
            builder.append(input.substring(i[1]));
            input = builder.toString();
        }
        advancedComponent.setText(input);
        for (val entry : advancedComponent.getComponents().entrySet())
            advancedComponent.setComponent(entry.getKey(), replaceLanguages(entry.getValue(), language, syntax));

        try {
            for (val entry : advancedComponent.getHovers().entrySet())
                entry.setValue(com.rexcantor64.triton.wrappers.HoverComponentWrapper
                        .handleHoverEvent(entry.getValue(), language, syntax));
        } catch (NoSuchMethodError e) {
            for (val entry : advancedComponent.getHovers().entrySet()) {
                val comps = HoverComponentWrapper.getValue(entry.getValue());
                val string = TextComponent.toLegacyText(comps);
                val replaced = replaceLanguages(Triton.get().getLanguageManager()
                        .matchPattern(string, language), language, syntax);
                if (replaced == null) {
                    if (entry.getValue().getAction() != HoverEvent.Action.SHOW_ITEM)
                        entry.setValue(null);
                    continue;
                }
                entry.setValue(HoverComponentWrapper
                        .setValue(entry.getValue(), TextComponent.fromLegacyText(replaced)));
            }
        }

        for (val entry : advancedComponent.getAllTranslatableArguments().entrySet())
            advancedComponent.getAllTranslatableArguments().put(entry.getKey(), entry.getValue().stream()
                    .map(comp -> parseAdvancedComponent(language, syntax, comp, contextPlayer)).collect(Collectors.toList()));
        return advancedComponent;
    }

    private AdvancedComponent parseTritonTranslation(String translatedResult, LanguagePlayer contextPlayer) {
        if (contextPlayer instanceof SpigotLanguagePlayer && Triton.isSpigot() && Triton.asSpigot().isPapiEnabled()) {
            SpigotLanguagePlayer slp = (SpigotLanguagePlayer) contextPlayer;
            val bukkitPlayer = slp.toBukkit();
            if (bukkitPlayer.isPresent()) {
                translatedResult = PlaceholderAPI.setPlaceholders(bukkitPlayer.get(), translatedResult);
            }
        }

        BaseComponent[] componentResult;
        if (translatedResult.startsWith("[triton_json]")) {
            val jsonInput = translatedResult.substring(13);
            try {
                componentResult = ComponentSerializer.parse(jsonInput);
            } catch (JsonParseException e) {
                Triton.get().getLogger()
                        .logError("Failed to parse JSON translation: %1", jsonInput);
                componentResult = TextComponent.fromLegacyText(jsonInput);
                e.printStackTrace();
            }
        } else if (translatedResult.startsWith("[minimsg]")) {
            val mmInput = translatedResult.substring(9);
            try {
                val textComponent = MiniMessage.miniMessage().deserialize(mmInput);
                val jsonSerialized = GsonComponentSerializer.gson().serialize(textComponent);
                componentResult = ComponentSerializer.parse(jsonSerialized);
            } catch (JsonParseException | NullPointerException e) {
                Triton.get().getLogger()
                        .logError("Failed to parse Mini Message translation: %1", mmInput);
                componentResult = TextComponent.fromLegacyText(mmInput);
                e.printStackTrace();
            } catch (NoClassDefFoundError e) {
                Triton.get().getLogger().logError("Failed to parse Mini Message translation because Mini Message is only available on PaperMC (or forks).");
                componentResult = TextComponent.fromLegacyText(mmInput);
            }
        } else {
            componentResult = TextComponent.fromLegacyText(translatedResult);
        }
        return AdvancedComponent.fromBaseComponent(componentResult);
    }
}
