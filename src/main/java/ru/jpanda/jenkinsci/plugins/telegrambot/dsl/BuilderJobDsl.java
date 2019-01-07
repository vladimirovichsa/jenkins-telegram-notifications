package ru.jpanda.jenkinsci.plugins.telegrambot.dsl;

import ru.jpanda.jenkinsci.plugins.telegrambot.BotBuilder;
import hudson.Extension;
import javaposse.jobdsl.dsl.helpers.step.StepContext;
import javaposse.jobdsl.plugin.ContextExtensionPoint;
import javaposse.jobdsl.plugin.DslExtensionMethod;

@Extension(optional = true)
public class BuilderJobDsl extends ContextExtensionPoint {

    @DslExtensionMethod(context = StepContext.class)
    public Object telegramBot(Runnable closure) {
        BuilderContext context = new BuilderContext();
        executeInContext(closure, context);

        return new BotBuilder(context.chatId, context.message);
    }
}
