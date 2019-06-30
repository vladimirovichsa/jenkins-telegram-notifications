package ru.jpanda.jenkinsci.plugins.telegrambot.configuration

import jenkins.model.GlobalConfiguration
import ru.jpanda.jenkinsci.plugins.telegrambot.jenkins.BotGlobalConfiguration

class Config {
    companion object {
        @JvmStatic
        val CONFIG = GlobalConfiguration.all()[BotGlobalConfiguration::class.java]
    }

    fun reloadConfig() {
        //        CONFIG = GlobalConfiguration.all().get(BotGlobalConfiguration.class);
    }
}