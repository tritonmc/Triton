package com.rexcantor64.triton.config;

import com.rexcantor64.triton.Triton;
import com.rexcantor64.triton.config.interfaces.ConfigurationProvider;
import com.rexcantor64.triton.config.interfaces.YamlConfiguration;
import com.rexcantor64.triton.utils.YAMLUtils;
import lombok.val;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.ComponentLike;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;
import net.kyori.adventure.text.minimessage.tag.resolver.TagResolver;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

public class MessagesConfig {

    private HashMap<String, Object> messages = new HashMap<>();
    private HashMap<String, Object> defaultMessages = new HashMap<>();

    public void setup() {
        val conf = Triton.get().loadYAML("messages", "messages");
        messages = YAMLUtils.deepToMap(conf, "");

        val defaultConf = ConfigurationProvider.getProvider(YamlConfiguration.class)
                .load(Triton.get().getLoader().getResourceAsStream("messages.yml"));
        defaultMessages = YAMLUtils.deepToMap(defaultConf, "");

        if (!defaultMessages.keySet().equals(messages.keySet())) {
            Triton.get().getLogger()
                    .logWarning("It seems like your messages.yml file is outdated");
            Triton.get().getLogger()
                    .logWarning("You can get an up-to-date copy at https://triton.rexcantor64.com/messagesyml");
        }
    }

    private String getString(String code) {
        Object msg = messages.get(code);
        if (msg == null)
            msg = defaultMessages.get(code);

        if (msg instanceof List) {
            return ((List<?>) msg).stream().map(Objects::toString).collect(Collectors.joining("<reset><newline>"));
        }

        return Objects.toString(msg, "Unknown message");
    }

    private String getMessage(String code, int argsLength) {
        String s = getString(code);
        for (int i = 0; i < argsLength; i++)
            s = s.replace("%" + (i + 1), "<arg" + i + ">");
        return s;
    }

    public Component getMessageComponent(String code, ComponentLike... args) {
        String msg = getMessage(code, args.length);
        val resolvers = new TagResolver[args.length];
        for (int i = 0; i < args.length; i++) {
            resolvers[i] = Placeholder.component("arg" + i, args[i]);
        }
        return MiniMessage.miniMessage().deserialize(msg, resolvers);
    }
}
