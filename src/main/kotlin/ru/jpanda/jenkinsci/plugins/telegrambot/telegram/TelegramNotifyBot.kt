package ru.jpanda.jenkinsci.plugins.telegrambot.telegram

import hudson.FilePath
import hudson.model.Run
import hudson.model.TaskListener
import org.apache.http.client.methods.HttpPost
import org.apache.http.entity.BufferedHttpEntity
import org.apache.http.entity.ContentType
import org.apache.http.entity.mime.MultipartEntityBuilder
import org.apache.http.util.EntityUtils
import org.jenkinsci.plugins.tokenmacro.MacroEvaluationException
import org.jenkinsci.plugins.tokenmacro.TokenMacro
import org.telegram.telegrambots.bots.DefaultBotOptions
import org.telegram.telegrambots.extensions.bots.commandbot.TelegramLongPollingCommandBot
import org.telegram.telegrambots.facilities.TelegramHttpClientBuilder
import org.telegram.telegrambots.meta.api.methods.send.SendAnimation
import org.telegram.telegrambots.meta.api.methods.send.SendMessage
import org.telegram.telegrambots.meta.api.objects.Message
import org.telegram.telegrambots.meta.api.objects.Update
import org.telegram.telegrambots.meta.exceptions.TelegramApiException
import org.telegram.telegrambots.meta.logging.BotLogger
import ru.jpanda.jenkinsci.plugins.telegrambot.telegram.commands.AbstractBotCommand
import ru.jpanda.jenkinsci.plugins.telegrambot.telegram.commands.AddGifComand
import ru.jpanda.jenkinsci.plugins.telegrambot.telegram.commands.HelpCommand
import ru.jpanda.jenkinsci.plugins.telegrambot.telegram.commands.StartCommand
import java.io.IOException
import java.nio.charset.StandardCharsets
import java.util.*
import java.util.function.Consumer
import java.util.logging.Level
import java.util.logging.Logger

class TelegramNotifyBot: TelegramLongPollingCommandBot {
    private val LOG = Logger.getLogger(TelegramNotifyBot::class.java.name)

    private val token: String
    private val baseUrl: String?


    constructor(defaultBotOptions: DefaultBotOptions, token: String, name: String, baseUrl: String):super(defaultBotOptions, name) {
        this.token = token
        this.baseUrl = baseUrl

        Arrays.asList<AbstractBotCommand>(
                StartCommand(),
                HelpCommand(),
                AddGifComand()
        ).forEach(Consumer<AbstractBotCommand> { this.register(it) })
    }

    fun sendMessage(chatId: Long?, message: String) {
        val sendMessageRequest = SendMessage()
        sendMessageRequest.chatId = chatId!!.toString()
        sendMessageRequest.text = message
        sendMessageRequest.enableMarkdown(true)

        try {
            execute(sendMessageRequest)
        } catch (e: TelegramApiException) {
            LOG.log(Level.SEVERE, String.format(
                    "NotifyBot: Error while sending message: %s%n%s", chatId, message), e)
        }

    }

    fun sendMessage(chatId: Long?, animation: SendAnimation) {
        try {
            animation.enableNotification()
            execute(animation.setChatId(chatId!!.toString()))
        } catch (e: TelegramApiException) {
            LOG.log(Level.SEVERE, String.format(
                    "NotifyBot: Error while sending animation: %s%n%s", chatId, animation.animation.attachName), e)
        }

    }

    @Throws(TelegramApiException::class)
    override fun execute(sendAnimation: SendAnimation): Message {
        if (sendAnimation == null) {
            throw TelegramApiException("Parameter $sendAnimation can not be null")
        }
        sendAnimation.validate()
        try {
            val url = getBaseUrl() + SendAnimation.PATH
            val httppost = HttpPost(url)
            httppost.config = options.requestConfig
            val builder = MultipartEntityBuilder.create()
            builder.setLaxMode()
            builder.setCharset(StandardCharsets.UTF_8)
            builder.addTextBody(SendAnimation.CHATID_FIELD, sendAnimation.chatId, ContentType.create("text/plain", StandardCharsets.UTF_8))
            builder.addTextBody(SendAnimation.ANIMATION_FIELD, sendAnimation.animation.attachName, ContentType.create("text/plain", StandardCharsets.UTF_8))

            if (sendAnimation.disableNotification != null) {
                builder.addTextBody(SendAnimation.DISABLENOTIFICATION_FIELD, sendAnimation.disableNotification!!.toString(), ContentType.create("text/plain", StandardCharsets.UTF_8))
            }

            if (sendAnimation.caption != null) {
                builder.addTextBody(SendAnimation.CAPTION_FIELD, sendAnimation.caption, ContentType.create("text/plain", StandardCharsets.UTF_8))
                if (sendAnimation.parseMode != null) {
                    builder.addTextBody(SendAnimation.PARSEMODE_FIELD, sendAnimation.parseMode, ContentType.create("text/plain", StandardCharsets.UTF_8))
                }
            }
            val multipart = builder.build()
            httppost.entity = multipart
            return sendAnimation.deserializeResponse(sendHttpPostRequest(httppost))
        } catch (e: IOException) {
            throw TelegramApiException("Unable to edit message media", e)
        }

    }

    @Throws(IOException::class)
    private fun sendHttpPostRequest(httppost: HttpPost): String {
        TelegramHttpClientBuilder.build(options).execute(httppost, options.httpContext).use { response ->
            val ht = response.entity
            val buf = BufferedHttpEntity(ht)
            return EntityUtils.toString(buf, StandardCharsets.UTF_8)
        }
    }

    @Throws(IOException::class, InterruptedException::class)
    fun sendMessage(chatId: Long?,
                    message: String, run: Run<*, *>, filePath: FilePath, taskListener: TaskListener) {

        var logMessage = message
        try {
            logMessage = TokenMacro.expandAll(run, filePath, taskListener, message)
        } catch (e: MacroEvaluationException) {
            LOG.log(Level.SEVERE, "Error while expanding the message", e)
        }

        try {
            val finalLogMessage = logMessage

            this.sendMessage(chatId, finalLogMessage)

        } catch (e: Exception) {
            LOG.log(Level.SEVERE, "Error while sending the message", e)
        }

        if (BotRunner.getInstance().getConfig().shouldLogToConsole()) taskListener.logger.println(logMessage)
    }

    override fun processNonCommandUpdate(update: Update?) {
        if (update == null) {
            LOG.log(Level.WARNING, "Update is null")
            return
        }

        val nonCommandMessage = BotRunner.getInstance().getConfig().botStrings["message.noncommand"]

        val message = update.message
        val chat = message.chat

        if (update.message.hasAnimation()) {
            BotLogger.info("LISTENER_CHAT_ANIMATION", update.message.animation.toString())
        }

        if (chat.isUserChat!!) {
            sendMessage(chat.id, nonCommandMessage!!)
            return
        }

        val text = message.text

        try {
            if (null == text || "" == text) return
            val tmp = text.split(" ".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()
            if (text.length < 1 || text[0] != '@' || null == tmp[0]) return
            if (tmp.size < 2 || BotRunner.getInstance().getConfig().botName != tmp[0].substring(1, tmp[0].length)) return
        } catch (e: Exception) {
            LOG.log(Level.SEVERE, "Something bad happened while message processing", e)
            return
        }

        sendMessage(chat.id, nonCommandMessage!!)
    }

    override fun getBaseUrl(): String {
        return if (baseUrl != null && "" != baseUrl) baseUrl else super.getBaseUrl()
    }

    override fun getBotToken(): String {
        return token
    }

    override fun toString(): String {
        return "NotifyBot{$token}"
    }
}