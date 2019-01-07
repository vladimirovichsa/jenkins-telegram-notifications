package ru.jpanda.jenkinsci.plugins.telegrambot.telegram.commands;

//import org.telegram.telegrambots.api.methods.send.SendMessage;
//import org.telegram.telegrambots.api.objects.Chat;
//import org.telegram.telegrambots.api.objects.User;
//import org.telegram.telegrambots.bots.AbsSender;
//import org.telegram.telegrambots.exceptions.TelegramApiException;
//import org.telegram.telegrambots.logging.BotLogger;

import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.Chat;
import org.telegram.telegrambots.meta.api.objects.User;
import org.telegram.telegrambots.meta.bots.AbsSender;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;
import org.telegram.telegrambots.meta.logging.BotLogger;

public class HelpCommand extends AbstractBotCommand {

    private static final String LOG_TAG = "/help";

    public HelpCommand() {
        super("help", "command.help");
    }

    @Override
    public void execute(AbsSender absSender, User user, Chat chat, String[] strings) {
        SendMessage answer = new SendMessage();
        answer.setChatId(chat.getId().toString());
        answer.setText(botStrings.get("message.help"));

        try {
            absSender.execute(answer);
        } catch (TelegramApiException e) {
            BotLogger.error(LOG_TAG, e);
        }
    }
}
