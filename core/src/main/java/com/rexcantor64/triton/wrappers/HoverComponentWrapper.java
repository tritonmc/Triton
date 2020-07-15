package com.rexcantor64.triton.wrappers;

import com.rexcantor64.triton.Triton;
import com.rexcantor64.triton.api.config.FeatureSyntax;
import lombok.val;
import net.md_5.bungee.api.chat.BaseComponent;
import net.md_5.bungee.api.chat.HoverEvent;
import net.md_5.bungee.api.chat.TextComponent;
import net.md_5.bungee.api.chat.hover.content.Content;
import net.md_5.bungee.api.chat.hover.content.Entity;
import net.md_5.bungee.api.chat.hover.content.Item;
import net.md_5.bungee.api.chat.hover.content.Text;

import java.util.ArrayList;
import java.util.concurrent.atomic.AtomicBoolean;

public class HoverComponentWrapper {

    public static HoverEvent handleHoverEvent(HoverEvent hoverEvent, String language, FeatureSyntax syntax) {
        val changed = new AtomicBoolean(false);

        val contents = hoverEvent.getContents();
        val newContents = new ArrayList<Content>();
        val languageParser = Triton.get().getLanguageParser();

        contents.forEach((content -> {
            if (content instanceof Text) {
                val text = (Text) content;
                if (text.getValue() instanceof BaseComponent[]) {
                    val string = TextComponent.toLegacyText((BaseComponent[]) text.getValue());
                    // TODO implement check in replaceLanguages instead?
                    val replaced = languageParser.replaceLanguages(string, language, syntax);
                    if (!string.equals(replaced)) {
                        changed.set(true);
                        if (replaced != null) // handle disabled line
                            newContents.add(new Text(TextComponent.fromLegacyText(replaced)));
                        return;
                    }
                }
                newContents.add(text);
            } else if (content instanceof Item) {
                val item = (Item) content;
                if (item.getTag() != null) {
                    val tag = item.getTag();
                    // TODO
                }
                newContents.add(item);
            } else if (content instanceof Entity) {
                val entity = (Entity) content;
                if (entity.getName() != null) {
                    val string = TextComponent.toLegacyText(entity.getName());
                    // TODO implement check in replaceLanguages instead?
                    val replaced = languageParser.replaceLanguages(string, language, syntax);
                    if (!string.equals(replaced)) {
                        changed.set(true);
                        if (replaced != null) // handle disabled line
                            newContents.add(new Entity(entity.getType(), entity.getId(), new TextComponent(TextComponent
                                    .fromLegacyText(replaced))));
                        return;
                    }
                }
                newContents.add(entity);
            }
        }));

        if (changed.get()) {
            if (newContents.size() == 0) return null;
            return new HoverEvent(hoverEvent.getAction(), newContents);
        }
        return hoverEvent;
    }

}
