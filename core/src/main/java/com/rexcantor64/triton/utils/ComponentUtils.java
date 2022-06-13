package com.rexcantor64.triton.utils;

import com.rexcantor64.triton.language.parser.AdvancedComponent;
import net.md_5.bungee.api.ChatColor;
import net.md_5.bungee.api.chat.BaseComponent;
import net.md_5.bungee.api.chat.ClickEvent;
import net.md_5.bungee.api.chat.TextComponent;
import net.md_5.bungee.api.chat.TranslatableComponent;

import java.util.LinkedList;
import java.util.List;
import java.util.Objects;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class ComponentUtils {

    private static final Pattern url = Pattern.compile("^(?:(https?)://)?([-\\w_\\.]{2,}\\.[a-z]{2,4})(/\\S*)?$");

    public static int encodeClickAction(ClickEvent.Action action) {
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
            case COPY_TO_CLIPBOARD:
                return 5;
        }
        return 0;
    }

    public static ClickEvent.Action decodeClickAction(int action) {
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
            case 5:
                return ClickEvent.Action.COPY_TO_CLIPBOARD;
        }
    }

    public static boolean isLink(String text) {
        return url.matcher(text).find();
    }

    public static void copyFormatting(BaseComponent origin, BaseComponent target) {
        target.setColor(origin.getColorRaw());
        target.setBold(origin.isBoldRaw());
        target.setItalic(origin.isItalicRaw());
        target.setUnderlined(origin.isUnderlinedRaw());
        target.setStrikethrough(origin.isStrikethroughRaw());
        target.setObfuscated(origin.isObfuscatedRaw());
        try {
            target.setInsertion(origin.getInsertion());
        } catch (NoSuchMethodError ignore) {
            // Ignore, it's an outdated server
        }
        try {
            target.setFont(origin.getFontRaw());
        } catch (NoSuchMethodError ignore) {
            // Ignore, it's an outdated server
        }
    }

    public static ChatColor getColorFromBaseComponent(BaseComponent bc) {
        if (bc.getColorRaw() != null)
            return bc.getColorRaw();
        Object parent = NMSUtils.getDeclaredField(bc, "parent");
        return !(parent instanceof BaseComponent) ? ChatColor.RESET :
                getColorFromBaseComponent((BaseComponent) parent);
    }

    public static boolean hasExtra(BaseComponent bc) {
        return bc.getExtra() != null && bc.getExtra().size() != 0;
    }

    public static boolean haveSameFormatting(BaseComponent c1, BaseComponent c2) {
        try {
            if (!Objects.equals(c1.getInsertion(), c2.getInsertion()))
                return false;
        } catch (NoSuchMethodError ignore) {
            // Ignore, it's an outdated server
        }
        try {
            if (!Objects.equals(c1.getFontRaw(), c2.getFontRaw()))
                return false;
        } catch (NoSuchMethodError ignore) {
            // Ignore, it's an outdated server
        }
        return c1.getColorRaw() == c2.getColorRaw() &&
                c1.isBoldRaw() == c2.isBoldRaw() &&
                c1.isItalicRaw() == c2.isItalicRaw() &&
                c1.isUnderlinedRaw() == c2.isUnderlinedRaw() &&
                c1.isStrikethroughRaw() == c2.isStrikethroughRaw() &&
                c1.isObfuscatedRaw() == c2.isObfuscatedRaw() &&
                Objects.equals(c1.getHoverEvent(), c2.getHoverEvent()) &&
                Objects.equals(c1.getClickEvent(), c2.getClickEvent());
    }

    /**
     * Given BaseComponents, splits them by new lines, preserving style and hierarchy.
     *
     * @param comps A list of BaseComponent
     * @return A list of the split BaseComponent lists
     */
    public static List<List<BaseComponent>> splitByNewLine(List<BaseComponent> comps) {
        List<List<BaseComponent>> split = new LinkedList<>();
        List<BaseComponent> acc = new LinkedList<>();
        for (BaseComponent comp : comps) {
            if (!(comp instanceof TextComponent)) {
                acc.add(comp);
                continue;
            }
            TextComponent textComponent = (TextComponent) comp;
            String[] textSplit = textComponent.getText().split("\n", -1);
            for (int i = 0; i < textSplit.length; ++i) {
                TextComponent newSplit = new TextComponent(textSplit[i]);
                copyFormatting(textComponent, newSplit);
                acc.add(newSplit);
                if (i == textSplit.length - 1) {
                    // the last split keeps the extras
                    if (hasExtra(textComponent)) {
                        List<List<BaseComponent>> extraSplit = splitByNewLine(textComponent.getExtra());
                        for (int j = 0; j < extraSplit.size(); ++j) {
                            if (j == 0) {
                                // the first split add to the parent element
                                extraSplit.get(i).forEach(newSplit::addExtra);
                            } else {
                                // flush accumulator before adding new sibling
                                split.add(acc);
                                acc = new LinkedList<>();
                                BaseComponent extraWrapper = new TextComponent(extraSplit.get(j).toArray(new BaseComponent[0]));
                                copyFormatting(textComponent, extraWrapper);
                                acc.add(extraWrapper);
                            }
                        }
                    }
                } else {
                    // flush accumulator
                    split.add(acc);
                    acc = new LinkedList<>();
                }
            }
        }
        // flush accumulator
        if (acc.size() > 0) split.add(acc);
        return split;
    }

    /**
     * Given a Stream of BaseComponents, ensure they're not italic by setting italic to false if it's not set.
     * This is useful for translating item names and lores, where Minecraft makes them italic by default.
     * This does not do anything if the given component does not have any formatting whatsoever,
     * as to preserve the default Minecraft behaviour.
     *
     * @param baseComponents The components to check for italic
     * @return An array of the same components after they've been modified
     */
    public static BaseComponent[] ensureNotItalic(Stream<BaseComponent> baseComponents) {
        return baseComponents.peek(comp -> {
            if (comp.isItalicRaw() == null && hasAnyFormatting(comp)) {
                comp.setItalic(false);
            }
        }).toArray(BaseComponent[]::new);
    }

    /**
     * Checks if the given component or any of its children have formatting.
     *
     * @param component The component to check for formatting
     * @return Whether there is any kind of formatting in the given component and its children
     */
    private static boolean hasAnyFormatting(BaseComponent component) {
        return component.hasFormatting() ||
                (component.getExtra() != null &&
                        component.getExtra().stream().anyMatch(ComponentUtils::hasAnyFormatting));
    }

    /**
     * Converts the given components to legacy text and wraps it in a TextComponent.
     * For some reason, the Notchian client requires this on some packets in some versions.
     *
     * @param comps The components to flatten into legacy text
     * @return A singleton array with a TextComponent containing legacy text of the given components
     */
    public static BaseComponent[] mergeComponents(BaseComponent... comps) {
        if (hasTranslatableComponent(comps))
            return comps;
        return new BaseComponent[]{new TextComponent(AdvancedComponent.fromBaseComponent(true, comps).getText())};
    }

    /**
     * Finds whether any of the given components or their children are TranslatableComponents
     *
     * @param comps The components to check for TranslatableComponents
     * @return Whether any of the given components or their children are TranslatableComponents
     */
    public static boolean hasTranslatableComponent(BaseComponent... comps) {
        for (BaseComponent c : comps) {
            if (c instanceof TranslatableComponent)
                return true;
            if (c.getExtra() != null && hasTranslatableComponent(c.getExtra().toArray(new BaseComponent[0])))
                return true;
        }
        return false;
    }

}
