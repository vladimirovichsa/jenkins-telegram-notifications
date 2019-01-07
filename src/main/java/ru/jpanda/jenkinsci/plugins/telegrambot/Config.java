package ru.jpanda.jenkinsci.plugins.telegrambot;

import jenkins.model.GlobalConfiguration;

public final class Config {

    private final static BotGlobalConfiguration CONFIG = GlobalConfiguration.all().get(BotGlobalConfiguration.class);

    public static BotGlobalConfiguration getCONFIG() {
        return CONFIG;
    }

    public static void reloadConfig() {
//        CONFIG = GlobalConfiguration.all().get(BotGlobalConfiguration.class);
    }
}
