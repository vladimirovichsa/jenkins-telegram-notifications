package ru.jpanda.jenkinsci.plugins.telegrambot.jenkins

import hudson.Extension
import hudson.FilePath
import hudson.Launcher
import hudson.model.AbstractProject
import hudson.model.Result
import hudson.model.Run
import hudson.model.TaskListener
import hudson.tasks.BuildStepDescriptor
import hudson.tasks.BuildStepMonitor
import hudson.tasks.Notifier
import hudson.tasks.Publisher
import jenkins.tasks.SimpleBuildStep
import org.kohsuke.stapler.DataBoundConstructor
import org.telegram.telegrambots.meta.api.methods.send.SendAnimation
import org.telegram.telegrambots.meta.logging.BotLogger
import ru.jpanda.jenkinsci.plugins.telegrambot.configuration.Config
import ru.jpanda.jenkinsci.plugins.telegrambot.telegram.BotRunner
import java.io.IOException
import java.util.*

class BotPublisher : Notifier, SimpleBuildStep {


    /**
     * The message that will be expanded and sent to users
     */
    private val chatId: Long?
    private val message: String

    private val whenSuccess: Boolean
    private val whenUnstable: Boolean
    private val whenFailed: Boolean
    private val whenAborted: Boolean

    @DataBoundConstructor
    constructor(chatId: Long?, message: String,
                whenSuccess: Boolean,
                whenUnstable: Boolean,
                whenFailed: Boolean,
                whenAborted: Boolean) {

        this.chatId = chatId
        this.message = message
        this.whenSuccess = whenSuccess
        this.whenUnstable = whenUnstable
        this.whenFailed = whenFailed
        this.whenAborted = whenAborted
    }

    @Extension
    class Descriptor : BuildStepDescriptor<Publisher>() {
        override fun isApplicable(jobType: Class<out AbstractProject<*, *>>): Boolean {
            return true
        }

        override fun getDisplayName(): String {
            return BotGlobalConfiguration.PLUGIN_DISPLAY_NAME
        }
    }

    override fun getRequiredMonitorService(): BuildStepMonitor {
        return BuildStepMonitor.NONE
    }

    @Throws(InterruptedException::class, IOException::class)
    override fun perform(
            run: Run<*, *>,
            filePath: FilePath,
            launcher: Launcher,
            taskListener: TaskListener) {

        val result = run.getResult()

        val success = result == Result.SUCCESS && whenSuccess
        val unstable = result == Result.UNSTABLE && whenUnstable
        val failed = result == Result.FAILURE && whenFailed
        val aborted = result == Result.ABORTED && whenAborted

        val neededToSend = success || unstable || failed || aborted

        if (neededToSend) {
            BotRunner.getInstance().getBot()!!
                    .sendMessage(getChatId(), getMessage(), run, filePath, taskListener)
        }
        val animationMessage = SendAnimation()

        if (success) {
            animationMessage.setAnimation(getRandomAnimation(Config.CONFIG!!.botStringsGifSuccess))
        }
        //        if (unstable) {
        //            animationMessage.setAnimation(getRandomAnimation(Config.getCONFIG().getBotStringsGifUnstable()));
        //        }
        if (failed) {
            animationMessage.setAnimation(getRandomAnimation(Config.CONFIG!!.botStringsGifFailure))
        }
        //        if (aborted) {
        //            animationMessage.setAnimation(getRandomAnimation(Config.getCONFIG().getBotStringsGifAborted()));
        //        }

        if (success || failed) {
            BotRunner.getInstance().getBot()!!
                    .sendMessage(getChatId(), animationMessage)
        }
    }

    private fun getRandomAnimation(gifMap: Map<String, String>): String {
        val crunchifyKeys = gifMap.keys.toTypedArray()
        val fileId = gifMap[crunchifyKeys[Random().nextInt(crunchifyKeys.size)]]
        BotLogger.info("ANIMATION", fileId)
        return fileId!!
    }

    fun getChatId(): Long? {
        return chatId
    }

    fun getMessage(): String {
        return message
    }

    fun isWhenSuccess(): Boolean {
        return whenSuccess
    }

    fun isWhenUnstable(): Boolean {
        return whenUnstable
    }

    fun isWhenFailed(): Boolean {
        return whenFailed
    }

    fun isWhenAborted(): Boolean {
        return whenAborted
    }
}