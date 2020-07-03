package com.rexcantor64.triton.language.parser;

import com.rexcantor64.triton.Triton;
import com.rexcantor64.triton.utils.ComponentUtils;
import lombok.Getter;
import lombok.val;
import lombok.var;
import net.md_5.bungee.api.ChatColor;
import net.md_5.bungee.api.chat.*;

import java.util.*;

public class AdvancedComponent {

    @Getter
    private String text;
    private HashMap<String, String> components = new HashMap<>();
    private HashMap<String, List<AdvancedComponent>> translatableArguments = new HashMap<>();

    public static AdvancedComponent fromBaseComponent(BaseComponent... components) {
        return fromBaseComponent(false, components);
    }

    public static AdvancedComponent fromBaseComponent(boolean onlyText, BaseComponent... components) {
        AdvancedComponent advancedComponent = new AdvancedComponent();
        StringBuilder builder = new StringBuilder();
        for (BaseComponent comp : components) {
            boolean hasClick = false;
            boolean hasHover = false;
            builder.append(ComponentUtils.getColorFromBaseComponent(comp).toString());
            if (comp.hasFormatting()) {
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
                if (!onlyText) {
                    if (comp.getClickEvent() != null && !comp.getClickEvent().getValue()
                            .endsWith("[/" + Triton.get().getConf().getChatSyntax().getLang() + "]")) {
                        builder.append("\uE400");
                        builder.append(ComponentUtils.encodeClickAction(comp.getClickEvent().getAction()));
                        UUID uuid = UUID.randomUUID();
                        advancedComponent.setComponent(uuid, comp.getClickEvent().getValue());
                        builder.append(uuid.toString());
                        hasClick = true;
                    }
                    if (comp.getHoverEvent() != null) {
                        builder.append("\uE500");
                        builder.append(ComponentUtils.encodeHoverAction(comp.getHoverEvent().getAction()));
                        UUID uuid = UUID.randomUUID();
                        advancedComponent
                                .setComponent(uuid, TextComponent.toLegacyText(comp.getHoverEvent().getValue()));
                        builder.append(uuid.toString());
                        hasHover = true;
                    }
                }
            }
            if (comp instanceof TextComponent)
                builder.append(((TextComponent) comp).getText());

            if (!onlyText && comp instanceof TranslatableComponent) {
                TranslatableComponent tc = (TranslatableComponent) comp;
                UUID uuid = UUID.randomUUID();
                builder.append("\uE600")
                        .append(tc.getTranslate())
                        .append("\uE600")
                        .append(uuid)
                        .append("\uE600");
                List<AdvancedComponent> args = new ArrayList<>();
                if (tc.getWith() != null)
                    for (BaseComponent arg : tc.getWith())
                        args.add(fromBaseComponent(false, arg));
                advancedComponent.setTranslatableArguments(uuid.toString(), args);
            }
            if (comp.getExtra() != null) {
                AdvancedComponent component = fromBaseComponent(onlyText, comp.getExtra()
                        .toArray(new BaseComponent[0]));
                builder.append(component.getText());
                for (Map.Entry<String, String> entry : component.getComponents().entrySet())
                    advancedComponent.setComponent(entry.getKey(), entry.getValue());
                for (Map.Entry<String, List<AdvancedComponent>> entry :
                        component.getAllTranslatableArguments().entrySet())
                    advancedComponent.setTranslatableArguments(entry.getKey(), entry.getValue());
            }
            if (hasHover)
                builder.append("\uE501");
            if (hasClick)
                builder.append("\uE401");

        }
        advancedComponent.setText(builder.toString());
        return advancedComponent;
    }

    public BaseComponent[] toBaseComponent() {
        return new BaseComponent[]{new TextComponent(toBaseComponent(this.text).toArray(new BaseComponent[0]))};
    }

    private List<BaseComponent> toBaseComponent(String text) {
        List<BaseComponent> list = new ArrayList<>();
        StringBuilder builder = new StringBuilder();
        TextComponent component = new TextComponent("");
        for (int i = 0; i < text.length(); i++) {
            char c = text.charAt(i);
            if (c == '\u00A7') {
                i++;
                if (i >= text.length()) {
                    builder.append(c);
                    continue;
                }
                val lowercaseChar = Character.toLowerCase(text.charAt(i));

                ChatColor format;
                if (lowercaseChar == 'x' && i + 12 < text.length()) {
                    val color = text.substring(i + 1, i + 13);
                    format = ChatColor.of("#" + color.replace("\u00A7", ""));
                    i += 12;
                } else {
                    format = ChatColor.getByChar(lowercaseChar);
                }
                if (format == null) {
                    builder.append(c);
                    i--;
                    continue;
                }
                if (builder.length() != 0) {
                    component.setText(builder.toString());
                    builder = new StringBuilder();
                    ChatColor previousColor = ComponentUtils.getColorFromBaseComponent(component);
                    list.add(component);
                    component = new TextComponent("");
                    component.setColor(previousColor);
                }
                if (ChatColor.BOLD.equals(format)) {
                    component.setBold(true);
                } else if (ChatColor.ITALIC.equals(format)) {
                    component.setItalic(true);
                } else if (ChatColor.UNDERLINE.equals(format)) {
                    component.setUnderlined(true);
                } else if (ChatColor.STRIKETHROUGH.equals(format)) {
                    component.setStrikethrough(true);
                } else if (ChatColor.MAGIC.equals(format)) {
                    component.setObfuscated(true);
                } else if (ChatColor.RESET.equals(format)) {
                    component.setBold(null);
                    component.setItalic(null);
                    component.setUnderlined(null);
                    component.setStrikethrough(null);
                    component.setObfuscated(null);
                    component.setColor(null);
                } else {
                    component.setColor(format);
                }
            } else if (c == '\uE400' || c == '\uE500') {
                if (builder.length() != 0) {
                    component.setText(builder.toString());
                    builder = new StringBuilder();
                    BaseComponent previousComponent = component;
                    list.add(component);
                    component = new TextComponent("");
                    ComponentUtils.copyFormatting(previousComponent, component);
                }
                String uuid = text.substring(i + 2, i + 2 + 36);
                int actionCode = Integer.parseInt(Character.toString(text.charAt(i + 1)));
                if (c == '\uE400') {
                    ClickEvent.Action action = ComponentUtils.decodeClickAction(actionCode);
                    component.setClickEvent(new ClickEvent(action, this.getComponent(uuid)));
                } else { // c == '\uE500'
                    HoverEvent.Action action = ComponentUtils.decodeHoverAction(actionCode);
                    component.setHoverEvent(new HoverEvent(action, TextComponent
                            .fromLegacyText(this.getComponent(uuid))));
                }
                i += 2 + 36;
                int deep = 0;
                StringBuilder content = new StringBuilder();
                while (text.charAt(i) != c + 1 || deep != 0) {
                    char c1 = text.charAt(i);
                    if (c1 == c) deep++; // c == \uE400 || c == \uE500
                    if (c1 == c + 1) deep--; // c + 1 == \uE401 || c + 1 == \uE501
                    content.append(c1);
                    i++;
                }
                List<BaseComponent> extra = toBaseComponent(content.toString());
                if (extra.size() > 0)
                    component.setExtra(extra);
                BaseComponent previousComponent = component;
                list.add(component);
                component = new TextComponent("");
                ComponentUtils.copyFormatting(previousComponent, component);
            } else if (c == '\uE600') {
                i++;
                StringBuilder key = new StringBuilder();
                while (text.charAt(i) != '\uE600') {
                    key.append(text.charAt(i));
                    i++;
                }
                i++;
                StringBuilder uuid = new StringBuilder();
                while (text.charAt(i) != '\uE600') {
                    uuid.append(text.charAt(i));
                    i++;
                }
                if (builder.length() != 0) {
                    component.setText(builder.toString());
                    builder = new StringBuilder();
                    BaseComponent previousComponent = component;
                    list.add(component);
                    component = new TextComponent("");
                    ComponentUtils.copyFormatting(previousComponent, component);
                }
                TranslatableComponent tc = new TranslatableComponent(key.toString());
                ComponentUtils.copyFormatting(component, tc);
                List<AdvancedComponent> argsAdvanced = this.getTranslatableArguments(uuid.toString());
                if (argsAdvanced != null)
                    for (AdvancedComponent ac : argsAdvanced) {
                        BaseComponent[] bc = ac.toBaseComponent();
                        tc.addWith(bc == null ? new TextComponent("") : bc[0]);
                    }
                list.add(tc);
            } else
                builder.append(c);
        }
        if (builder.length() != 0) {
            component.setText(builder.toString());
            list.add(component);
        }
        return list;
    }

    public String getTextClean() {
        var result = text;
        while (result.startsWith(ChatColor.RESET.toString()))
            result = result.substring(2);
        return result;
    }

    public void setText(String text) {
        this.text = text;
    }

    private void setComponent(UUID uuid, String text) {
        components.put(uuid.toString(), text);
    }

    public void setComponent(String uuid, String text) {
        components.put(uuid, text);
    }

    private String getComponent(String uuid) {
        return components.get(uuid);
    }

    private void setTranslatableArguments(String uuid, List<AdvancedComponent> list) {
        translatableArguments.put(uuid, list);
    }

    private List<AdvancedComponent> getTranslatableArguments(String uuid) {
        return translatableArguments.get(uuid);
    }

    public HashMap<String, List<AdvancedComponent>> getAllTranslatableArguments() {
        return translatableArguments;
    }

    public HashMap<String, String> getComponents() {
        return components;
    }

    @Override
    public String toString() {
        return "AdvancedComponent{" +
                "text='" + text + '\'' +
                ", components=" + components +
                ", translatableArguments=" + translatableArguments +
                '}';
    }
}