package ru.jpanda.jenkinsci.plugins.telegrambot.jenkins

import hudson.model.AbstractProject
import hudson.util.FormFieldValidator
import hudson.util.FormValidation
import hudson.util.ListBoxModel
import jenkins.model.GlobalConfiguration
import net.sf.json.JSONObject
import org.kohsuke.stapler.DataBoundConstructor
import org.kohsuke.stapler.QueryParameter
import org.kohsuke.stapler.StaplerRequest
import org.kohsuke.stapler.StaplerResponse
import org.telegram.telegrambots.bots.DefaultBotOptions
import ru.jpanda.jenkinsci.plugins.telegrambot.configuration.PropertyFiles
import ru.jpanda.jenkinsci.plugins.telegrambot.telegram.BotRunner
import ru.jpanda.jenkinsci.plugins.telegrambot.utils.StaplerRequestContainer
import ru.jpanda.jenkinsci.plugins.telegrambot.utils.Utils
import java.io.IOException
import java.util.*
import java.util.logging.Logger
import java.util.stream.Collectors
import javax.servlet.ServletException
import java.util.function.Function

class BotGlobalConfiguration : GlobalConfiguration {


    private val LOG = Logger.getLogger(BotGlobalConfiguration::class.java.name)

    companion object {
        @JvmStatic
        internal val PLUGIN_DISPLAY_NAME = "NotifyBot"
    }

    var botStrings: Map<String, String>
    var botStringsGifFailure: Map<String, String>
    var botStringsGifSuccess: Map<String, String>
    var botStringsGifUnstable: Map<String, String>
    var botStringsGifAborted: Map<String, String>

    var shouldLogToConsole: Boolean = false
    var botProxyHost: String = ""
    var botProxyPort: Int = 0
    var botProxyUser: String = ""
    var botProxyPassword: String = ""
    var botCustomProxy: Boolean = false
    var botToken: String = ""
    var botName: String = ""
    var botProxyType: DefaultBotOptions.ProxyType = DefaultBotOptions.ProxyType.NO_PROXY
    var baseUrl: String = ""

    /**
     * Called when Jenkins is starting and it's config is loading
     */
    @DataBoundConstructor
    constructor() {
        //load property files in map
        botStrings = loadProperties(PropertyFiles.PROPERTY_CONFIG.string)
        botStringsGifFailure = loadProperties(PropertyFiles.PROPERTY_GIF_FAILURE.string)
        botStringsGifSuccess = loadProperties(PropertyFiles.PROPERTY_GIF_SUCCESS.string)
        botStringsGifUnstable = loadProperties(PropertyFiles.PROPERTY_GIF_UNSTABLE.string)
        botStringsGifAborted = loadProperties(PropertyFiles.PROPERTY_GIF_ABORTED.string)

        // Load global Jenkins config
        load()

        // Run the bot after Jenkins config has been loaded
        BotRunner.getInstance().runBot(botName!!, botToken!!)
    }

    private fun loadProperties(fileName: String): Map<String, String> {
        val config: Map<String, String>
        try {
            val properties = Properties()
            properties.load(BotGlobalConfiguration::class.java.classLoader.getResourceAsStream(fileName))
            config = Collections.unmodifiableMap((properties.stringPropertyNames().stream()
                    .collect(Collectors
                            .toMap(Function.identity(), Function<String, String> { properties.getProperty(it) })) as MutableMap<out String, out String>?)!!)
        } catch (e: IOException) {
            throw RuntimeException("Bot properties file not found", e)
        }

        return config
    }

    /**
     * Test connection bot
     */
    @Throws(IOException::class, ServletException::class)
    fun doTestConnection(req: StaplerRequest, rsp: StaplerResponse,
                         @QueryParameter("botProxyHost") botProxyHost: String,
                         @QueryParameter("botProxyPort") botProxyPort: String,
                         @QueryParameter("botName") botName: String,
                         @QueryParameter("botToken") botToken: String,
                         @QueryParameter("baseUrl") baseUrl: String) {
        object : FormFieldValidator(req, rsp, true) {
            @Throws(IOException::class, ServletException::class)
            override fun check() {
                this@BotGlobalConfiguration.botProxyHost = botProxyHost
                this@BotGlobalConfiguration.botProxyPort = Integer.parseInt(botProxyPort)

                val response = BotRunner.getInstance().testConnection(botName, botToken, baseUrl)
                if (response == null) {
                    ok("Success")
                } else {
                    error("Connect error : $response")
                }

            }
        }.process()
    }


    /**
     * View select in jelly form
     */
    fun doFillSelectProxyTypeItems(): ListBoxModel {
        val listBoxModel = ListBoxModel()
        listBoxModel.add(ListBoxModel.Option("No Proxy", "0"))
        listBoxModel.add(ListBoxModel.Option("HTTP", "1"))
        listBoxModel.add(ListBoxModel.Option("SOCKS4", "2"))
        listBoxModel.add(ListBoxModel.Option("SOCKS5", "3"))

        for (i in listBoxModel.indices) {
            if (botProxyType.ordinal == i) {
                listBoxModel[i].selected = true
            }
        }
        return listBoxModel
    }

    /**
     * Called when Jenkins config is saving via web-interface
     */
    @Throws(hudson.model.Descriptor.FormException::class)
    override fun configure(req: StaplerRequest, formData: JSONObject?): Boolean {
        // Save for the future using
        StaplerRequestContainer.req = req
        // Getting simple params from formData
        shouldLogToConsole = (formData!!.getBoolean("shouldLogToConsole"))
        botToken = formData.getString("botToken")
        botName = formData.getString("botName")
        baseUrl = formData.getString("baseUrl")
        if (null != formData.get("customProxyName")) {
            botCustomProxy = true
            botProxyHost = formData.getJSONObject("customProxyName").getString("botProxyHost")
            botProxyPort = formData.getJSONObject("customProxyName").getInt("botProxyPort")
            botProxyUser = formData.getJSONObject("customProxyName").getString("botProxyUser")
            botProxyPassword = formData.getJSONObject("customProxyName").getString("botProxyPassword")
            botProxyType = Utils.parseProxyType(Integer.parseInt(formData.getJSONObject("customProxyName").getString("selectProxyType")))
        }

        BotRunner.getInstance().runBot(botName, botToken)

        save()
        return super.configure(req, formData)
    }


    override fun toString(): String {
        return "BotGlobalConfiguration{" +
                "botStrings=" + botStrings +
                ", shouldLogToConsole=" + shouldLogToConsole +
                ", botProxyHost='" + botProxyHost + '\''.toString() +
                ", botProxyPort=" + botProxyPort +
                ", botProxyUser='" + botProxyUser + '\''.toString() +
                ", botProxyPassword='" + botProxyPassword + '\''.toString() +
                ", botCustomProxy=" + botCustomProxy +
                ", botToken='" + botToken + '\''.toString() +
                ", botName='" + botName + '\''.toString() +
                ", botProxyType=" + botProxyType +
                '}'.toString()
    }

    @Throws(IOException::class, ServletException::class)
    fun doCheckMessage(@QueryParameter value: String): FormValidation {
        return if (value.length == 0) FormValidation.error("Please set a message") else FormValidation.ok()
    }

}