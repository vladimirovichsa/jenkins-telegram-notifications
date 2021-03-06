package ru.jpanda.jenkinsci.plugins.telegrambot;

import hudson.Extension;
import hudson.FilePath;
import hudson.Launcher;
import hudson.model.AbstractProject;
import hudson.model.Result;
import hudson.model.Run;
import hudson.model.TaskListener;
import hudson.tasks.BuildStepDescriptor;
import hudson.tasks.BuildStepMonitor;
import hudson.tasks.Notifier;
import hudson.tasks.Publisher;
import jenkins.tasks.SimpleBuildStep;
import ru.jpanda.jenkinsci.plugins.telegrambot.telegram.BotRunner;
import org.kohsuke.stapler.DataBoundConstructor;
import org.telegram.telegrambots.meta.api.methods.send.SendAnimation;
import org.telegram.telegrambots.meta.logging.BotLogger;

import javax.annotation.Nonnull;
import java.io.IOException;
import java.util.Map;
import java.util.Random;

public class BotPublisher extends Notifier implements SimpleBuildStep {

    /**
     * The message that will be expanded and sent to users
     */
    private final Long chatId;
    private final String message;

    private final boolean whenSuccess;
    private final boolean whenUnstable;
    private final boolean whenFailed;
    private final boolean whenAborted;

    @DataBoundConstructor
    public BotPublisher(
            Long chatId, String message,
            boolean whenSuccess,
            boolean whenUnstable,
            boolean whenFailed,
            boolean whenAborted) {

        this.chatId = chatId;
        this.message = message;
        this.whenSuccess = whenSuccess;
        this.whenUnstable = whenUnstable;
        this.whenFailed = whenFailed;
        this.whenAborted = whenAborted;
    }

    @Extension
    public static class Descriptor extends BuildStepDescriptor<Publisher> {
        @Override
        public boolean isApplicable(Class<? extends AbstractProject> jobType) {
            return true;
        }

        @Nonnull
        @Override
        public String getDisplayName() {
            return BotGlobalConfiguration.PLUGIN_DISPLAY_NAME;
        }
    }

    @Override
    public BuildStepMonitor getRequiredMonitorService() {
        return BuildStepMonitor.NONE;
    }

    @Override
    public void perform(
            @Nonnull Run<?, ?> run,
            @Nonnull FilePath filePath,
            @Nonnull Launcher launcher,
            @Nonnull TaskListener taskListener) throws InterruptedException, IOException {

        Result result = run.getResult();

        boolean success = result == Result.SUCCESS && whenSuccess;
        boolean unstable = result == Result.UNSTABLE && whenUnstable;
        boolean failed = result == Result.FAILURE && whenFailed;
        boolean aborted = result == Result.ABORTED && whenAborted;

        boolean neededToSend = success || unstable || failed || aborted;

        if (neededToSend) {
            BotRunner.getInstance().getBot()
                    .sendMessage(getChatId(), getMessage(), run, filePath, taskListener);
        }
        final SendAnimation animationMessage = new SendAnimation();

        if (success) {
            animationMessage.setAnimation(getRandomAnimation(Config.getCONFIG().getBotStringsGifSuccess()));
        }
//        if (unstable) {
//            animationMessage.setAnimation(getRandomAnimation(Config.getCONFIG().getBotStringsGifUnstable()));
//        }
        if (failed) {
            animationMessage.setAnimation(getRandomAnimation(Config.getCONFIG().getBotStringsGifFailure()));
        }
//        if (aborted) {
//            animationMessage.setAnimation(getRandomAnimation(Config.getCONFIG().getBotStringsGifAborted()));
//        }

        if(success || failed){
            BotRunner.getInstance().getBot()
                    .sendMessage(getChatId(), animationMessage);
        }
    }

    private String getRandomAnimation(Map<String, String> gifMap){
        Object[] crunchifyKeys = gifMap.keySet().toArray();
        String fileId = gifMap.get(crunchifyKeys[new Random().nextInt(crunchifyKeys.length)]);
        BotLogger.info("ANIMATION",fileId);
        return fileId;
    }

    public Long getChatId() {
        return chatId;
    }

    public String getMessage() {
        return message;
    }

    public boolean isWhenSuccess() {
        return whenSuccess;
    }

    public boolean isWhenUnstable() {
        return whenUnstable;
    }

    public boolean isWhenFailed() {
        return whenFailed;
    }

    public boolean isWhenAborted() {
        return whenAborted;
    }
}
