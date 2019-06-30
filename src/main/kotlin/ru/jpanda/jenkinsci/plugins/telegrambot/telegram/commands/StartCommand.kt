package ru.jpanda.jenkinsci.plugins.telegrambot.telegram.commands

import org.telegram.telegrambots.meta.api.methods.send.SendMessage
import org.telegram.telegrambots.meta.api.objects.Chat
import org.telegram.telegrambots.meta.api.objects.Message
import org.telegram.telegrambots.meta.api.objects.User
import org.telegram.telegrambots.meta.bots.AbsSender
import org.telegram.telegrambots.meta.exceptions.TelegramApiException
import org.telegram.telegrambots.meta.logging.BotLogger

class StartCommand: AbstractBotCommand {

    private val LOG_TAG = "/start"

    constructor():super("start", "command.start")

    override fun execute(absSender: AbsSender, user: User, chat: Chat, strings: Array<String>) {
        val answer = SendMessage()
        answer.chatId = chat.id!!.toString()
        answer.text = botStrings["message.start"]?.let { String.format(it, chat.id) }

        try {
            absSender.execute<Message, SendMessage>(answer)
        } catch (e: TelegramApiException) {
            BotLogger.error(LOG_TAG, e)
        }

    }
}