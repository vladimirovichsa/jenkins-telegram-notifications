package ru.jpanda.jenkinsci.plugins.telegrambot.jenkins.dsl;

import javaposse.jobdsl.dsl.Context;

public class BuilderContext implements Context {
    Long chatId;
    String message;

    public void message(Long chatId, String message) {
        this.chatId = chatId;
        this.message = message;
    }
}
