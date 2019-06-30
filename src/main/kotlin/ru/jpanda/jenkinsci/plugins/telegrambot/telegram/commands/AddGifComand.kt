package ru.jpanda.jenkinsci.plugins.telegrambot.telegram.commands

import org.telegram.telegrambots.meta.api.objects.Chat
import org.telegram.telegrambots.meta.api.objects.User
import org.telegram.telegrambots.meta.bots.AbsSender

class AddGifComand : AbstractBotCommand {


    constructor() : super("add_gif", "command.add_gif")

    constructor(commandIdentifier: String, descriptionKey: String): super(commandIdentifier, descriptionKey)

    override fun execute(absSender: AbsSender, user: User, chat: Chat, strings: Array<String>) {

    }
}