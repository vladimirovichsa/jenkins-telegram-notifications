package ru.jpanda.jenkinsci.plugins.telegrambot.telegram.commands

import jenkins.model.GlobalConfiguration
import org.telegram.telegrambots.extensions.bots.commandbot.commands.BotCommand
import org.telegram.telegrambots.meta.api.objects.Chat
import org.telegram.telegrambots.meta.api.objects.User
import org.telegram.telegrambots.meta.bots.AbsSender
import ru.jpanda.jenkinsci.plugins.telegrambot.jenkins.BotGlobalConfiguration

abstract class AbstractBotCommand: BotCommand {
    companion object {
        @JvmStatic
        private val botGlobalConfiguration = GlobalConfiguration.all().get(BotGlobalConfiguration::class.java)
    }

    internal var botStrings: Map<String, String> = emptyMap()

    constructor(commandIdentifier:String, descriptionKey:String):super(commandIdentifier, botGlobalConfiguration!!.botStrings[descriptionKey]){
        botStrings = botGlobalConfiguration!!.botStrings
    }

    abstract override fun execute(absSender: AbsSender, user: User, chat: Chat, strings: Array<String>)
}