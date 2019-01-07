package ru.jpanda.jenkinsci.plugins.telegrambot.dsl;

import javaposse.jobdsl.dsl.Context;

public class PublisherContext implements Context {

    Long chatId;
    String message;
    boolean whenSuccess;
    boolean whenFailed;
    boolean whenAborted;
    boolean whenUnstable;

    public void message(Long chatId, String message) {
        this.chatId = chatId;
        this.message = message;
    }

    public void onSuccess() {
        this.whenSuccess = true;
    }

    public void onFailed() {
        this.whenFailed = true;
    }

    public void onAborted() {
        this.whenAborted = true;
    }

    public void onUnstable() {
        this.whenUnstable = true;
    }
}
