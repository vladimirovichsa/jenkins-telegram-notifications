package ru.jpanda.jenkinsci.plugins.telegrambot;

import hudson.Extension;
import hudson.FilePath;
import hudson.Launcher;
import hudson.model.AbstractProject;
import hudson.model.Run;
import hudson.model.TaskListener;
import hudson.tasks.BuildStepDescriptor;
import hudson.tasks.BuildStepMonitor;
import hudson.tasks.Notifier;
import hudson.tasks.Publisher;
import jenkins.tasks.SimpleBuildStep;
import ru.jpanda.jenkinsci.plugins.telegrambot.telegram.BotRunner;
import org.jenkinsci.Symbol;
import org.kohsuke.stapler.DataBoundConstructor;

import javax.annotation.Nonnull;
import java.io.IOException;

public class BotMessanger extends Notifier implements SimpleBuildStep {

    /**
     * The message that will be expanded and sent to users
     */
    private final Long chatId;
    private final String message;


    @DataBoundConstructor
    public BotMessanger(Long chatId,
                        String message) {
        this.chatId = chatId;
        this.message = message;
    }

    @Extension @Symbol("telegramSend")
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

            BotRunner.getInstance().getBot()
                    .sendMessage(getChatId(),getMessage(), run, filePath, taskListener);

    }

    public Long getChatId() {
        return chatId;
    }

    public String getMessage() {
        return message;
    }


}
