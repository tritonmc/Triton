package com.rexcantor64.triton.utils;

import com.rexcantor64.triton.Triton;
import com.rexcantor64.triton.language.parser.AdvancedComponent;
import net.md_5.bungee.api.ChatColor;
import net.md_5.bungee.api.chat.*;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.regex.Pattern;

public class ComponentUtils {

    private static final Pattern url = Pattern.compile("^(?:(https?)://)?([-\\w_\\.]{2,}\\.[a-z]{2,4})(/\\S*)?$");

    public static BaseComponent[] fromLegacyText(AdvancedComponent advancedComponent) {
        String message = advancedComponent.getText();
        BaseComponent master = new TextComponent("");
        StringBuilder builder = new StringBuilder();
        InteractiveEventStatus clickEventStatus = InteractiveEventStatus.NONE;
        BaseComponent currentClickComponent = new TextComponent("");
        InteractiveEventStatus hoverEventStatus = InteractiveEventStatus.NONE;
        BaseComponent currentHoverComponent = new TextComponent("");
        BaseComponent writeTo = master;
        TextComponent component = new TextComponent("");
        for (int i = 0; i < message.length(); i++) {
            char c = message.charAt(i);
            if (clickEventStatus == InteractiveEventStatus.READING_VALUE) {
                if (c == '\uD805') {
                    String value = advancedComponent.getComponent(builder.toString());
                    if (value == null)
                        return null;
                    currentClickComponent.setClickEvent(new ClickEvent(writeTo.getClickEvent().getAction(), value));
                    builder = new StringBuilder();
                    clickEventStatus = InteractiveEventStatus.READING_TEXT;
                    continue;
                }
                builder.append(c);
            } else if (hoverEventStatus == InteractiveEventStatus.READING_VALUE) {
                if (c == '\uD806') {
                    String value = advancedComponent.getComponent(builder.toString());
                    if (value == null)
                        return null;
                    currentHoverComponent.setHoverEvent(new HoverEvent(writeTo.getHoverEvent().getAction(), TextComponent.fromLegacyText(value)));
                    builder = new StringBuilder();
                    hoverEventStatus = InteractiveEventStatus.READING_TEXT;
                    continue;
                }
                builder.append(c);
            } else if (c == '§') {
                i++;
                if (i >= message.length()) {
                    builder.append(c);
                    continue;
                }
                char code = message.charAt(i);
                if ("0123456789AaBbCcDdEeFfKkLlMmNnOoRr".indexOf(code) == -1) {
                    builder.append(c);
                    i--;
                    continue;
                }
                code = Character.toLowerCase(code);
                ChatColor format = ChatColor.getByChar(code);
                if (builder.length() != 0) {
                    component.setText(builder.toString());
                    builder = new StringBuilder();
                    ChatColor previousColor = component.getColor();
                    writeTo.addExtra(component);
                    component = new TextComponent("");
                    component.setColor(previousColor);
                }
                switch (format) {
                    case BOLD:
                        component.setBold(true);
                        break;
                    case ITALIC:
                        component.setItalic(true);
                        break;
                    case UNDERLINE:
                        component.setUnderlined(true);
                        break;
                    case STRIKETHROUGH:
                        component.setStrikethrough(true);
                        break;
                    case MAGIC:
                        component.setObfuscated(true);
                        break;
                    case RESET:
                        component.setColor(ChatColor.WHITE);
                        break;
                    default:
                        component.setColor(format);
                        break;
                }
            } else if (c == '\uD805') {
                if (builder.length() != 0) {
                    component.setText(builder.toString());
                    builder = new StringBuilder();
                    ChatColor previousColor = component.getColorRaw();
                    Boolean bold = component.isBoldRaw();
                    Boolean italic = component.isItalicRaw();
                    Boolean underline = component.isUnderlinedRaw();
                    Boolean strikethrough = component.isStrikethroughRaw();
                    Boolean obfuscated = component.isObfuscatedRaw();
                    writeTo.addExtra(component);
                    component = new TextComponent("");
                    component.setColor(previousColor);
                    component.setBold(bold);
                    component.setItalic(italic);
                    component.setUnderlined(underline);
                    component.setStrikethrough(strikethrough);
                    component.setObfuscated(obfuscated);
                }
                if (clickEventStatus == InteractiveEventStatus.NONE) {
                    i++;
                    currentClickComponent = new TextComponent("");
                    writeTo = currentClickComponent;
                    currentClickComponent.setClickEvent(new ClickEvent(decodeClickAction(Integer.parseInt(Character.toString(message.charAt(i)))), ""));
                    clickEventStatus = InteractiveEventStatus.READING_VALUE;
                } else {
                    if (hoverEventStatus == InteractiveEventStatus.NONE)
                        writeTo = master;
                    else
                        writeTo = currentHoverComponent;
                    writeTo.addExtra(currentClickComponent);
                    clickEventStatus = InteractiveEventStatus.NONE;
                }
            } else if (c == '\uD806') {
                if (builder.length() != 0) {
                    component.setText(builder.toString());
                    builder = new StringBuilder();
                    ChatColor previousColor = component.getColorRaw();
                    Boolean bold = component.isBoldRaw();
                    Boolean italic = component.isItalicRaw();
                    Boolean underline = component.isUnderlinedRaw();
                    Boolean strikethrough = component.isStrikethroughRaw();
                    Boolean obfuscated = component.isObfuscatedRaw();
                    writeTo.addExtra(component);
                    component = new TextComponent("");
                    component.setColor(previousColor);
                    component.setBold(bold);
                    component.setItalic(italic);
                    component.setUnderlined(underline);
                    component.setStrikethrough(strikethrough);
                    component.setObfuscated(obfuscated);
                }
                if (hoverEventStatus == InteractiveEventStatus.NONE) {
                    i++;
                    currentHoverComponent = new TextComponent("");
                    writeTo = currentHoverComponent;
                    currentHoverComponent.setHoverEvent(new HoverEvent(decodeHoverAction(Integer.parseInt(Character.toString(message.charAt(i)))), null));
                    hoverEventStatus = InteractiveEventStatus.READING_VALUE;
                } else {
                    if (clickEventStatus == InteractiveEventStatus.NONE)
                        writeTo = master;
                    else
                        writeTo = currentClickComponent;
                    writeTo.addExtra(currentHoverComponent);
                    hoverEventStatus = InteractiveEventStatus.NONE;
                }
            } else if (c == '\uD807') {
                i++;
                StringBuilder key = new StringBuilder();
                while (i < message.length() && message.charAt(i) != '\uD807') {
                    key.append(message.charAt(i));
                    i++;
                }
                i++;
                StringBuilder uuid = new StringBuilder();
                while (i < message.length() && message.charAt(i) != '\uD807') {
                    uuid.append(message.charAt(i));
                    i++;
                }
                if (builder.length() != 0) {
                    component.setText(builder.toString());
                    builder = new StringBuilder();
                    ChatColor previousColor = component.getColorRaw();
                    Boolean bold = component.isBoldRaw();
                    Boolean italic = component.isItalicRaw();
                    Boolean underline = component.isUnderlinedRaw();
                    Boolean strikethrough = component.isStrikethroughRaw();
                    Boolean obfuscated = component.isObfuscatedRaw();
                    writeTo.addExtra(component);
                    component = new TextComponent("");
                    component.setColor(previousColor);
                    component.setBold(bold);
                    component.setItalic(italic);
                    component.setUnderlined(underline);
                    component.setStrikethrough(strikethrough);
                    component.setObfuscated(obfuscated);
                }
                TranslatableComponent tc = new TranslatableComponent(key.toString());
                tc.setColor(component.getColorRaw());
                tc.setBold(component.isBoldRaw());
                tc.setItalic(component.isItalicRaw());
                tc.setUnderlined(component.isUnderlinedRaw());
                tc.setStrikethrough(component.isStrikethroughRaw());
                tc.setObfuscated(component.isObfuscatedRaw());
                List<AdvancedComponent> argsAdvanced = advancedComponent.getTranslatableArguments(uuid.toString());
                if (argsAdvanced != null)
                    for (AdvancedComponent ac : argsAdvanced) {
                        BaseComponent[] bc = fromLegacyText(ac);
                        tc.addWith(bc == null ? new TextComponent("") : bc[0]);
                    }
                writeTo.addExtra(tc);
            } else {
                builder.append(c);
            }
        }
        if (builder.length() != 0) {
            component.setText(builder.toString());
            writeTo.addExtra(component);
        }
        return new BaseComponent[]{master};
    }

    public static AdvancedComponent toLegacyText(BaseComponent... components) {
        AdvancedComponent advancedComponent = new AdvancedComponent();
        StringBuilder builder = new StringBuilder();
        for (BaseComponent comp : components) {
            boolean hasClick = false;
            boolean hasHover = false;
            if (comp.hasFormatting()) {
                if (comp.getColor() != null)
                    builder.append(comp.getColor().toString());
                if (comp.isBold())
                    builder.append(ChatColor.BOLD.toString());
                if (comp.isItalic())
                    builder.append(ChatColor.ITALIC.toString());
                if (comp.isUnderlined())
                    builder.append(ChatColor.UNDERLINE.toString());
                if (comp.isStrikethrough())
                    builder.append(ChatColor.STRIKETHROUGH.toString());
                if (comp.isObfuscated())
                    builder.append(ChatColor.MAGIC.toString());
                if (comp.getClickEvent() != null && !comp.getClickEvent().getValue().endsWith("[/" + Triton.get().getConf().getChatSyntax().getLang() + "]")) {
                    builder.append("\uD805");
                    builder.append(encodeClickAction(comp.getClickEvent().getAction()));
                    UUID uuid = UUID.randomUUID();
                    advancedComponent.setComponent(uuid, comp.getClickEvent().getValue());
                    builder.append(uuid.toString());
                    builder.append("\uD805");
                    hasClick = true;
                }
                if (comp.getHoverEvent() != null) {
                    builder.append("\uD806");
                    builder.append(encodeHoverAction(comp.getHoverEvent().getAction()));
                    UUID uuid = UUID.randomUUID();
                    advancedComponent.setComponent(uuid, TextComponent.toLegacyText(comp.getHoverEvent().getValue()));
                    builder.append(uuid.toString());
                    builder.append("\uD806");
                    hasHover = true;
                }
            }
            if (comp instanceof TextComponent)
                builder.append(((TextComponent) comp).getText());
            if (comp instanceof TranslatableComponent) {
                TranslatableComponent tc = (TranslatableComponent) comp;
                UUID uuid = UUID.randomUUID();
                builder.append("\uD807")
                        .append(tc.getTranslate())
                        .append("\uD807")
                        .append(uuid)
                        .append("\uD807");
                List<AdvancedComponent> args = new ArrayList<>();
                if (tc.getWith() != null)
                    for (BaseComponent arg : tc.getWith())
                        args.add(toLegacyText(arg));
                advancedComponent.setTranslatableArguments(uuid, args);
            }
            if (comp.getExtra() != null) {
                AdvancedComponent component = toLegacyText(comp.getExtra().toArray(new BaseComponent[0]));
                builder.append(component.getText());
                for (Map.Entry<String, String> entry : component.getComponents().entrySet())
                    advancedComponent.setComponent(entry.getKey(), entry.getValue());
            }
            if (hasHover)
                builder.append("\uD806");
            if (hasClick)
                builder.append("\uD805");

        }
        advancedComponent.setText(builder.toString());
        return advancedComponent;
    }

    private static int encodeClickAction(ClickEvent.Action action) {
        switch (action) {
            case OPEN_URL:
                return 0;
            case OPEN_FILE:
                return 1;
            case RUN_COMMAND:
                return 2;
            case SUGGEST_COMMAND:
                return 3;
            case CHANGE_PAGE:
                return 4;
        }
        return 0;
    }

    private static ClickEvent.Action decodeClickAction(int action) {
        switch (action) {
            default:
                return ClickEvent.Action.OPEN_URL;
            case 1:
                return ClickEvent.Action.OPEN_FILE;
            case 2:
                return ClickEvent.Action.RUN_COMMAND;
            case 3:
                return ClickEvent.Action.SUGGEST_COMMAND;
            case 4:
                return ClickEvent.Action.CHANGE_PAGE;
        }
    }

    private static int encodeHoverAction(HoverEvent.Action action) {
        switch (action) {
            case SHOW_TEXT:
                return 0;
            case SHOW_ACHIEVEMENT:
                return 1;
            case SHOW_ITEM:
                return 2;
            case SHOW_ENTITY:
                return 3;
        }
        return 0;
    }

    private static HoverEvent.Action decodeHoverAction(int action) {
        switch (action) {
            default:
                return HoverEvent.Action.SHOW_TEXT;
            case 1:
                return HoverEvent.Action.SHOW_ACHIEVEMENT;
            case 2:
                return HoverEvent.Action.SHOW_ITEM;
            case 3:
                return HoverEvent.Action.SHOW_ENTITY;
        }
    }

    public static boolean isLink(String text) {
        return url.matcher(text).find();
    }

    private enum InteractiveEventStatus {
        NONE, READING_VALUE, READING_TEXT
    }

}
