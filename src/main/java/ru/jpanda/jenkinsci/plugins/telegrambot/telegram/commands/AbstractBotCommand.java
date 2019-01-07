package ru.jpanda.jenkinsci.plugins.telegrambot.telegram.commands;

import jenkins.model.GlobalConfiguration;
import ru.jpanda.jenkinsci.plugins.telegrambot.BotGlobalConfiguration;
import org.telegram.telegrambots.extensions.bots.commandbot.commands.BotCommand;
import org.telegram.telegrambots.meta.api.objects.Chat;
import org.telegram.telegrambots.meta.api.objects.User;
import org.telegram.telegrambots.meta.bots.AbsSender;

import java.util.Map;

public abstract class AbstractBotCommand extends BotCommand {
    private static final BotGlobalConfiguration CONFIG = GlobalConfiguration.all().get(BotGlobalConfiguration.class);

    final Map<String, String> botStrings;

    public AbstractBotCommand(final String commandIdentifier, final String descriptionKey) {
        super(commandIdentifier, CONFIG.getBotStrings().get(descriptionKey));
        botStrings = CONFIG.getBotStrings();
    }

    public abstract void execute(AbsSender absSender, User user, Chat chat, String[] strings);
}
