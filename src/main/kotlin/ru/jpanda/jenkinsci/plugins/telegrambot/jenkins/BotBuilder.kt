package ru.jpanda.jenkinsci.plugins.telegrambot.jenkins

import hudson.Extension
import hudson.FilePath
import hudson.Launcher
import hudson.model.AbstractProject
import hudson.model.Run
import hudson.model.TaskListener
import hudson.tasks.BuildStepDescriptor
import hudson.tasks.Builder
import jenkins.tasks.SimpleBuildStep
import org.kohsuke.stapler.DataBoundConstructor
import ru.jpanda.jenkinsci.plugins.telegrambot.telegram.BotRunner
import java.io.IOException

class BotBuilder : Builder, SimpleBuildStep {

    /**
     * The message that will be expanded and sent to users
     */

    private val chatId: Long?
    private val message: String

    @DataBoundConstructor
    constructor(chatId: Long?, message: String) {
        this.chatId = chatId
        this.message = message
    }

    @Extension
    class Descriptor : BuildStepDescriptor<Builder>() {
        override fun isApplicable(jobType: Class<out AbstractProject<*, *>>): Boolean {
            return true
        }

        override fun getDisplayName(): String {
            return BotGlobalConfiguration.PLUGIN_DISPLAY_NAME
        }
    }

    fun getChatId(): Long? {
        return chatId
    }

    fun getMessage(): String {
        return message
    }

    @Throws(InterruptedException::class, IOException::class)
    override fun perform(
            run: Run<*, *>,
            filePath: FilePath,
            launcher: Launcher,
            taskListener: TaskListener) {

        BotRunner.getInstance().getBot()!!
                .sendMessage(getChatId(), getMessage(), run, filePath, taskListener)
    }
}