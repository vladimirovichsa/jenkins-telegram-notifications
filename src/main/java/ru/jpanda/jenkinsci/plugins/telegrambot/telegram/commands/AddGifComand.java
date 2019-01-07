package ru.jpanda.jenkinsci.plugins.telegrambot.telegram.commands;

import org.telegram.telegrambots.meta.api.objects.Chat;
import org.telegram.telegrambots.meta.api.objects.User;
import org.telegram.telegrambots.meta.bots.AbsSender;

public class AddGifComand extends AbstractBotCommand {

    public AddGifComand() {
        super("add_gif", "command.add_gif");
    }

    public AddGifComand(String commandIdentifier, String descriptionKey) {
        super(commandIdentifier, descriptionKey);
    }

    @Override
    public void execute(AbsSender absSender, User user, Chat chat, String[] strings) {

    }
}
