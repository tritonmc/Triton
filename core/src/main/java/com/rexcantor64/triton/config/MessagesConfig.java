package com.rexcantor64.triton.config;

import com.rexcantor64.triton.Triton;
import com.rexcantor64.triton.config.interfaces.ConfigurationProvider;
import com.rexcantor64.triton.config.interfaces.YamlConfiguration;
import com.rexcantor64.triton.utils.YAMLUtils;
import lombok.val;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;

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

        if (messages.size() != defaultMessages.size()) {
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

    private String getMessage(String code, Object... args) {
        String s = getString(code);
        for (int i = 0; i < args.length; i++)
            if (args[i] != null)
                s = s.replace("%" + (i + 1), args[i].toString());
        return s;
    }

    public String getMessageUnsafe(String code, Object... args) {
        return this.getMessage(code, args);
    }

    public Component getMessageComponent(String code, Object... args) {
        String msg = getMessage(code, args);
        return MiniMessage.miniMessage().deserialize(msg);
    }
}
