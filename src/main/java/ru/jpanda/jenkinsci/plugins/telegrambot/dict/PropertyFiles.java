package ru.jpanda.jenkinsci.plugins.telegrambot.dict;

public enum PropertyFiles {

    PROPERTY_CONFIG("bot.properties"),
    PROPERTY_GIF_ABORTED("bot_gif_ABORTED.properties"),
    PROPERTY_GIF_FAILURE("bot_gif_FAILURE.properties"),
    PROPERTY_GIF_SUCCESS("bot_gif_SUCCESS.properties"),
    PROPERTY_GIF_UNSTABLE("bot_gif_UNSTABLE.properties");

    public String string;

    PropertyFiles(String s) {
        this.string = s;
    }
}
