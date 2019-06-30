package ru.jpanda.jenkinsci.plugins.telegrambot.jenkins.dsl;

import hudson.Extension;
import javaposse.jobdsl.plugin.ContextExtensionPoint;
import javaposse.jobdsl.plugin.DslExtensionMethod;

@Extension(optional = true)
public class PublisherJobDsl extends ContextExtensionPoint {

    @DslExtensionMethod(context = javaposse.jobdsl.dsl.helpers.publisher.PublisherContext.class)
    public Object telegramBot(Runnable closure) {
        PublisherContext context = new PublisherContext();
        executeInContext(closure, context);

        return new BotPublisher(
                context.chatId,
                context.message,
                context.whenSuccess,
                context.whenUnstable,
                context.whenFailed,
                context.whenAborted);
    }
}
