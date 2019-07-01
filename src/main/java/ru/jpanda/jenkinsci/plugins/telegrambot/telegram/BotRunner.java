package ru.jpanda.jenkinsci.plugins.telegrambot.telegram;

import hudson.ProxyConfiguration;
import ru.jpanda.jenkinsci.plugins.telegrambot.BotGlobalConfiguration;
import ru.jpanda.jenkinsci.plugins.telegrambot.Config;
import org.telegram.telegrambots.ApiContextInitializer;
import org.telegram.telegrambots.bots.DefaultBotOptions;
import org.telegram.telegrambots.meta.ApiContext;
import org.telegram.telegrambots.meta.TelegramBotsApi;
import org.telegram.telegrambots.meta.exceptions.TelegramApiRequestException;
import org.telegram.telegrambots.meta.generics.BotSession;

import java.io.IOException;
import java.net.Authenticator;
import java.net.PasswordAuthentication;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.logging.Level;
import java.util.logging.Logger;

public class BotRunner {
    private static BotRunner instance;

    private static final String LOGTAG = "BotRunner";

    private static final Logger LOG = Logger.getLogger(BotRunner.class.getName());

    private final TelegramBotsApi api = new TelegramBotsApi();
    private final ExecutorService executor = Executors.newSingleThreadExecutor();

    private TelegramNotifyBot bot;
    private BotSession botSession;

    private String botToken;
    private String botName;
    private String baseUrl;

    private boolean isProxy = false;

    static {
        System.setProperty("https.protocols", "TLSv1,TLSv1.1,TLSv1.2");
        ApiContextInitializer.init();
    }

    public synchronized static BotRunner getInstance() {
        if (instance == null) {
            instance = new BotRunner();
        }
        return instance;
    }

    public void runBot(String name, String token) {
        botName = name;
        botToken = token;
        executor.submit(task);
    }

    private final Runnable task = (() -> {
        if (bot == null
                || !botToken.isEmpty()
                || !botName.isEmpty()
                || !bot.getBotToken().equals(botToken)
                || !bot.getBotUsername().equals(botName)
                || !bot.getOptions().getProxyHost().equals(getConfig().getBotProxyHost())) {
            DefaultBotOptions defaultBotOptions = initializeProxy();
            LOG.log(Level.INFO, String.format("Connecting to %1s:%2s, type %3s ",defaultBotOptions.getProxyHost(),defaultBotOptions.getProxyPort(),defaultBotOptions.getProxyType()));
            bot = new TelegramNotifyBot(defaultBotOptions, botToken, botName,getConfig().getBaseUrl());
            LOG.log(Level.INFO, "Bot was created");
        } else {
            LOG.log(Level.INFO, "There is no reason for bot recreating");
            return;
        }
        try {
            botSession = api.registerBot(bot);
            LOG.log(Level.INFO, "New bot session was registered");
        } catch (TelegramApiRequestException e) {
            LOG.log(Level.SEVERE, "Telegram API error", e);
        }
    });

    public BotGlobalConfiguration getConfig() {
        return Config.getCONFIG();
    }

    private DefaultBotOptions initializeProxy() {
        DefaultBotOptions botOptions = ApiContext.getInstance(DefaultBotOptions.class);
        try {
            String botProxyHost = null;
            int botProxyPort = 0;
            if (getConfig().getCustomProxy()) {
                switch (getConfig().getBotProxyType()) {
                    case 0:
                        botOptions.setProxyType(DefaultBotOptions.ProxyType.NO_PROXY);
                        break;
                    case 1:
                        botOptions.setProxyType(DefaultBotOptions.ProxyType.HTTP);
                        isProxy = true;
                        break;
                    case 2:
                        botOptions.setProxyType(DefaultBotOptions.ProxyType.SOCKS4);
                        isProxy = true;
                        break;
                    case 3:
                        botOptions.setProxyType(DefaultBotOptions.ProxyType.SOCKS5);
                        isProxy = true;
                        break;
                }
                if (isProxy && null != (botProxyHost = getConfig().getBotProxyHost()) && !"".equalsIgnoreCase(botProxyHost)
                        && (botProxyPort = getConfig().getBotProxyPort()) != 0) {
                    LOG.log(Level.INFO, "PROXY is " + (isProxy ? "ENABLED" : "DISABLED"));
                    if (null != getConfig().getBotProxyUser() && !"".equalsIgnoreCase(getConfig().getBotProxyUser())
                            && null != getConfig().getBotProxyPassword() && !"".equalsIgnoreCase(getConfig().getBotProxyPassword())) {
                        LOG.log(Level.INFO, "Authenticator enabled");
                        Authenticator.setDefault(new Authenticator() {
                            @Override
                            protected PasswordAuthentication getPasswordAuthentication() {
                                return new PasswordAuthentication(getConfig().getBotProxyUser(), getConfig().getBotProxyPassword().toCharArray());
                            }
                        });
                    }
                    botOptions.setProxyHost(botProxyHost);
                    botOptions.setProxyPort(botProxyPort);
                    LOG.log(Level.INFO, "Custom proxy successfully initialized");
                } else {
                    LOG.log(Level.FINE, "Custom proxy empty");
                }
            } else {
                ProxyConfiguration proxyConfig = ProxyConfiguration.load();
                if (proxyConfig != null) {
                    LOG.log(Level.INFO, String.format("Proxy settings: %s:%d", proxyConfig.name, proxyConfig.port));
                    botOptions.setProxyHost(proxyConfig.name);
                    botOptions.setProxyPort(proxyConfig.port);
                    botOptions.setProxyType(DefaultBotOptions.ProxyType.HTTP);
                } else {
                    LOG.log(Level.INFO, "No proxy settings in Jenkins");
                }
            }
            LOG.log(Level.INFO, "Proxy successfully initialized");
        } catch (IOException e) {
            LOG.log(Level.SEVERE, "NotifyBot: Failed to set proxy", e);
        }
        return botOptions;
    }

    public String testConnection(String name, String token, String baseUrl) {
        this.botName = name;
        this.botToken = token;
        this.baseUrl = token;
        String result = null;
        TelegramNotifyBot testBot = new TelegramNotifyBot(initializeProxy(), botToken, botName, this.baseUrl);
        LOG.log(Level.INFO, "Test connection - Test connection");
        BotSession botSession = null;
        try {
            botSession = new TelegramBotsApi().registerBot(testBot);
        } catch (TelegramApiRequestException e) {
            result = e.getMessage();
            e.printStackTrace();
        }
        LOG.log(Level.INFO, "Test connection - New bot session was registered");
        if (botSession.isRunning()) {
            botSession.stop();
        }
        return result;
    }

    public TelegramNotifyBot getBot() {
        return bot;
    }
}
