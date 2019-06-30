package ru.jpanda.jenkinsci.plugins.telegrambot.utils

import org.telegram.telegrambots.bots.DefaultBotOptions

class Utils {

    companion object {
        @JvmStatic
        fun parseProxyType(port: Int): DefaultBotOptions.ProxyType = when (port) {
            1 -> DefaultBotOptions.ProxyType.HTTP
            2 -> DefaultBotOptions.ProxyType.SOCKS4
            3 -> DefaultBotOptions.ProxyType.SOCKS5
            else -> DefaultBotOptions.ProxyType.NO_PROXY

        }
    }

}