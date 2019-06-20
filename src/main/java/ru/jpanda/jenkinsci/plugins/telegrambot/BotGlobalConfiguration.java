package ru.jpanda.jenkinsci.plugins.telegrambot;

import hudson.Extension;
import hudson.model.AbstractProject;
import hudson.util.FormFieldValidator;
import hudson.util.FormValidation;
import hudson.util.ListBoxModel;
import jenkins.model.GlobalConfiguration;
import ru.jpanda.jenkinsci.plugins.telegrambot.dict.PropertyFiles;
import ru.jpanda.jenkinsci.plugins.telegrambot.telegram.BotRunner;
import ru.jpanda.jenkinsci.plugins.telegrambot.utils.StaplerRequestContainer;
import net.sf.json.JSONObject;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.QueryParameter;
import org.kohsuke.stapler.StaplerRequest;
import org.kohsuke.stapler.StaplerResponse;

import javax.annotation.Nonnull;
import javax.servlet.ServletException;
import java.io.IOException;
import java.util.*;
import java.util.function.Function;
import java.util.logging.Logger;
import java.util.stream.Collectors;

/**
 * This class if user for the storing global plugin configuration.
 */
@Extension
public class BotGlobalConfiguration extends GlobalConfiguration {

    private static final Logger LOG = Logger.getLogger(BotGlobalConfiguration.class.getName());

    final static String PLUGIN_DISPLAY_NAME = "NotifyBot";
    private final Map<String, String> botStrings;
    private final Map<String, String> botStringsGifFailure;
    private final Map<String, String> botStringsGifSuccess;
    private final Map<String, String> botStringsGifUnstable;
    private final Map<String, String> botStringsGifAborted;

    private Boolean shouldLogToConsole;
    private String botProxyHost;
    private int botProxyPort;
    private String botProxyUser;
    private String botProxyPassword;
    private boolean botCustomProxy;
    private String botToken;
    private String botName;
    private int botProxyType;
    private String baseUrl;

    /**
     * Called when Jenkins is starting and it's config is loading
     */
    @DataBoundConstructor
    public BotGlobalConfiguration() {
        //load property files in map
        botStrings = loadProperties(PropertyFiles.PROPERTY_CONFIG.string);
        botStringsGifFailure = loadProperties(PropertyFiles.PROPERTY_GIF_FAILURE.string);
        botStringsGifSuccess = loadProperties(PropertyFiles.PROPERTY_GIF_SUCCESS.string);
        botStringsGifUnstable = loadProperties(PropertyFiles.PROPERTY_GIF_UNSTABLE.string);
        botStringsGifAborted = loadProperties(PropertyFiles.PROPERTY_GIF_ABORTED.string);

        // Load global Jenkins config
        load();

        // Run the bot after Jenkins config has been loaded
        BotRunner.getInstance().runBot(botName, botToken);
    }

    private Map<String, String> loadProperties(String fileName) {
        final Map<String, String> config;
        try {
            Properties properties = new Properties();
            properties.load(BotGlobalConfiguration.class.getClassLoader().getResourceAsStream(fileName));
            config = Collections.unmodifiableMap(properties.stringPropertyNames().stream()
                    .collect(Collectors.toMap(Function.identity(), properties::getProperty)));
        } catch (IOException e) {
            throw new RuntimeException("Bot properties file not found", e);
        }
        return config;
    }

    /**
     * Test connection bot
     * */
    public void doTestConnection(StaplerRequest req, StaplerResponse rsp,
                                 @QueryParameter("botProxyHost") final String botProxyHost,
                                 @QueryParameter("botProxyPort") final String botProxyPort,
                                 @QueryParameter("botName") final String botName,
                                 @QueryParameter("botToken") final String botToken,
                                 @QueryParameter("baseUrl") final String baseUrl) throws IOException, ServletException {
        new FormFieldValidator(req, rsp, true) {
            protected void check() throws IOException, ServletException {
                setBotProxyHost(botProxyHost);
                setBotProxyPort(Integer.parseInt(botProxyPort));
                String res = null;
                res = BotRunner.getInstance().testConnection(botName, botToken, baseUrl);
                if (res == null) {
                    ok("Success");
                } else {
                    error("Connect error : " + res);
                }

            }
        }.process();
    }


    /**
     * View select in jelly form
     * */
    public ListBoxModel doFillSelectProxyTypeItems() {
        ListBoxModel listBoxModel = new ListBoxModel();
        listBoxModel.add(new ListBoxModel.Option("No Proxy", "0"));
        listBoxModel.add(new ListBoxModel.Option("HTTP", "1"));
        listBoxModel.add(new ListBoxModel.Option("SOCKS4", "2"));
        listBoxModel.add(new ListBoxModel.Option("SOCKS5", "3"));

        for (int i = 0; i < listBoxModel.size(); i++) {
            if (getBotProxyType() == i) {
                listBoxModel.get(i).selected = true;
            }
        }
        return listBoxModel;
    }

    /**
     * Called when Jenkins config is saving via web-interface
     */
    @Override
    public boolean configure(StaplerRequest req, JSONObject formData) throws FormException {

        // Save for the future using
        StaplerRequestContainer.req = req;
        // Getting simple params from formData
        setLogToConsole(formData.getBoolean("shouldLogToConsole"));
        setBotToken(formData.getString("botToken"));
        setBotName(formData.getString("botName"));
        setBaseUrl(formData.getString("baseUrl"));
        if (null != formData.get("customProxyName")) {
            setCustomProxy(true);
            setBotProxyHost(formData.getJSONObject("customProxyName").getString("botProxyHost"));
            setBotProxyPort(formData.getJSONObject("customProxyName").getInt("botProxyPort"));
            setBotProxyUser(formData.getJSONObject("customProxyName").getString("botProxyUser"));
            setBotProxyPassword(formData.getJSONObject("customProxyName").getString("botProxyPassword"));
            setBotProxyType(Integer.parseInt(formData.getJSONObject("customProxyName").getString("selectProxyType")));
        } else {
            setCustomProxy(false);
        }

        BotRunner.getInstance().runBot(botName, botToken);

        save();
        return super.configure(req, formData);
    }

    private void setBaseUrl(String baseUrl) {
        this.baseUrl = baseUrl;
    }

    @Override
    public String toString() {
        return "BotGlobalConfiguration{" +
                "botStrings=" + botStrings +
                ", shouldLogToConsole=" + shouldLogToConsole +
                ", botProxyHost='" + botProxyHost + '\'' +
                ", botProxyPort=" + botProxyPort +
                ", botProxyUser='" + botProxyUser + '\'' +
                ", botProxyPassword='" + botProxyPassword + '\'' +
                ", botCustomProxy=" + botCustomProxy +
                ", botToken='" + botToken + '\'' +
                ", botName='" + botName + '\'' +
                ", botProxyType=" + botProxyType +
                '}';
    }

    public int getBotProxyType() {
        return botProxyType;
    }

    private void setBotProxyType(int port) {
        botProxyType = port;
    }

    private void setBotProxyPassword(String proxyPassword) {
        this.botProxyPassword = proxyPassword;
    }

    private void setBotProxyUser(String proxyUser) {
        this.botProxyUser = proxyUser;
    }

    public int getBotProxyPort() {
        return this.botProxyPort;
    }

    public String getBotProxyHost() {
        return this.botProxyHost;
    }

    public boolean getCustomProxy() {
        return this.botCustomProxy;
    }

    private void setBotProxyPort(int botProxyPort) {
        this.botProxyPort = botProxyPort;
    }

    private void setBotProxyHost(String botProxyHost) {
        this.botProxyHost = botProxyHost;
    }

    private void setCustomProxy(boolean customProxy) {
        this.botCustomProxy = customProxy;
    }

    public FormValidation doCheckMessage(@QueryParameter String value) throws IOException, ServletException {
        return value.length() == 0 ? FormValidation.error("Please set a message") : FormValidation.ok();
    }

    public boolean isApplicable(Class<? extends AbstractProject> clazz) {
        return true;
    }

    @Nonnull
    @Override
    public String getDisplayName() {
        return PLUGIN_DISPLAY_NAME;
    }

    public Map<String, String> getBotStrings() {
        return botStrings;
    }

    public Map<String, String> getBotStringsGifFailure() {
        return botStringsGifFailure;
    }

    public Map<String, String> getBotStringsGifSuccess() {
        return botStringsGifSuccess;
    }

    public Map<String, String> getBotStringsGifUnstable() {
        return botStringsGifUnstable;
    }

    public Map<String, String> getBotStringsGifAborted() {
        return botStringsGifAborted;
    }

    public Boolean shouldLogToConsole() {
        return shouldLogToConsole;
    }

    public void setLogToConsole(Boolean shouldLogToConsole) {
        this.shouldLogToConsole = shouldLogToConsole;
    }

    public String getBotToken() {
        return botToken;
    }

    public void setBotToken(String botToken) {
        this.botToken = botToken;
    }

    public String getBotName() {
        return botName;
    }

    public void setBotName(String botName) {
        this.botName = botName;
    }

    public String getBotProxyUser() {
        return this.botProxyUser;
    }

    public String getBotProxyPassword() {
        return this.botProxyPassword;
    }

    public String getBaseUrl() {
        return this.baseUrl;
    }
}
