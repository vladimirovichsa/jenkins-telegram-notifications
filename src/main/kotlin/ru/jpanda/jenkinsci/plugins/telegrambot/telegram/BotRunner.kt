package ru.jpanda.jenkinsci.plugins.telegrambot.telegram

import org.telegram.telegrambots.ApiContextInitializer
import org.telegram.telegrambots.bots.DefaultBotOptions
import org.telegram.telegrambots.meta.ApiContext
import org.telegram.telegrambots.meta.TelegramBotsApi
import org.telegram.telegrambots.meta.exceptions.TelegramApiRequestException
import org.telegram.telegrambots.meta.generics.BotSession
import java.io.IOException
import java.net.Authenticator
import java.net.PasswordAuthentication
import java.util.concurrent.Executors
import java.util.logging.Level
import java.util.logging.Logger
import hudson.ProxyConfiguration
import ru.jpanda.jenkinsci.plugins.telegrambot.jenkins.BotGlobalConfiguration
import ru.jpanda.jenkinsci.plugins.telegrambot.configuration.Config

class BotRunner {
    private val logger = Logger.getLogger(TelegramNotifyBot::class.java.name)

    private val api = TelegramBotsApi()

    private val executor = Executors.newSingleThreadExecutor()
    private var bot: TelegramNotifyBot? = null

    private var botSession: BotSession? = null
    private var botToken: String? = null

    private var botName: String? = null
    private var baseUrl: String? = null
    companion object {
        private var instance: BotRunner? = null

        init {
            System.setProperty("https.protocols", "TLSv1.1")
            ApiContextInitializer.init()
        }
        @JvmStatic
        @Synchronized
        fun getInstance(): BotRunner {
            if (instance == null) {
                instance = BotRunner()
            }
            return instance as BotRunner
        }
    }



    fun runBot(name: String, token: String) {
        botName = name
        botToken = token
        executor.submit(task)
    }

    private val task = {
        if (isReconnectionBot()) {
            bot = TelegramNotifyBot(initializeProxy(), botToken, botName, getConfig().baseUrl)
            logger.log(Level.INFO, "Bot was created")
        } else {
            logger.log(Level.INFO, "There is no reason for bot recreating")
        }
        try {
            botSession = api.registerBot(bot)
            logger.log(Level.INFO, "New bot session was registered")
        } catch (e: TelegramApiRequestException) {
            logger.log(Level.SEVERE, "Telegram API error", e)
        }
    }

    private fun isReconnectionBot(): Boolean {
        return (bot == null
                || ("" != botToken && "" != botName
                && (bot!!.botToken != botToken || bot!!.botUsername != botName))
                || getConfig().customProxy && (bot!!.options.proxyHost != getConfig().botProxyHost || bot!!.options.proxyType!= getConfig().botProxyType))
    }

    fun getConfig(): BotGlobalConfiguration {
        return Config.getCONFIG()
    }

    private fun initializeProxy(): DefaultBotOptions? {
        var botOptions: DefaultBotOptions? = null
        try {
            botOptions = if (getConfig().customProxy) {
                getProxyBot()
            } else {
                getSystemProxyBot()
            }
            logger.log(Level.INFO, "Proxy initialized")
        } catch (e: IOException) {
            logger.log(Level.SEVERE, "NotifyBot: Failed to set proxy", e)
        }

        return botOptions
    }

    private fun getProxyBot(): DefaultBotOptions {
        val botOptions = ApiContext.getInstance(DefaultBotOptions::class.java)
        if (isProxy() && !getConfig().botProxyHost.isEmpty() && getConfig().botProxyPort != 0) {
            logger.log(Level.INFO, "PROXY is " + if (isProxy()) "ENABLED" else "DISABLED")
            if (!getConfig().botProxyUser.isEmpty() && !getConfig().botProxyPassword.isEmpty()) {
                logger.log(Level.INFO, "Authenticator enabled")
                Authenticator.setDefault(object : Authenticator() {
                    override fun getPasswordAuthentication(): PasswordAuthentication {
                        return PasswordAuthentication(getConfig().botProxyUser, getConfig().botProxyPassword.toCharArray())
                    }
                })
            }
            botOptions.proxyHost = getConfig().botProxyHost
            botOptions.proxyPort = getConfig().botProxyPort
        }
        return botOptions
    }

    private fun getSystemProxyBot(): DefaultBotOptions {
        val botOptions = ApiContext.getInstance(DefaultBotOptions::class.java)
        val proxyConfig = ProxyConfiguration.load()
        if (proxyConfig != null) {
            logger.log(Level.INFO, String.format("Proxy system settings in Jenkins: %s:%d", proxyConfig!!.name, proxyConfig!!.port))
            botOptions.proxyHost = proxyConfig!!.name
            botOptions.proxyPort = proxyConfig!!.port
            botOptions.proxyType = DefaultBotOptions.ProxyType.HTTP
        }
        return botOptions
    }

    private fun isProxy(): Boolean = getConfig().botProxyType != DefaultBotOptions.ProxyType.NO_PROXY


    fun testConnection(name: String, token: String, baseUrl: String): String? {
        this.botName = name
        this.botToken = token
        this.baseUrl = baseUrl
        var result: String? = null
        val testBot = TelegramNotifyBot(initializeProxy(), botToken, botName, this.baseUrl)
        logger.log(Level.INFO, "Test connection - Test connection")
        var botSession: BotSession? = null
        try {
            botSession = TelegramBotsApi().registerBot(testBot)
        } catch (e: TelegramApiRequestException) {
            result = e.message
            e.printStackTrace()
        }

        logger.log(Level.INFO, "Test connection - New bot session was registered")
        if (botSession!!.isRunning) {
            botSession.stop()
        }
        return result
    }

    fun getBot(): TelegramNotifyBot? {
        return bot
    }
}