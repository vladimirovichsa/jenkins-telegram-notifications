package ru.jpanda.jenkinsci.plugins.telegrambot.telegram;

import hudson.FilePath;
import hudson.model.Run;
import hudson.model.TaskListener;
import ru.jpanda.jenkinsci.plugins.telegrambot.telegram.commands.AddGifComand;
import ru.jpanda.jenkinsci.plugins.telegrambot.telegram.commands.HelpCommand;
import ru.jpanda.jenkinsci.plugins.telegrambot.telegram.commands.StartCommand;
import org.apache.http.HttpEntity;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.BufferedHttpEntity;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.mime.MultipartEntityBuilder;
import org.apache.http.util.EntityUtils;
import org.jenkinsci.plugins.tokenmacro.MacroEvaluationException;
import org.jenkinsci.plugins.tokenmacro.TokenMacro;
import org.telegram.telegrambots.bots.DefaultBotOptions;
import org.telegram.telegrambots.extensions.bots.commandbot.TelegramLongPollingCommandBot;
import org.telegram.telegrambots.facilities.TelegramHttpClientBuilder;
import org.telegram.telegrambots.meta.api.methods.send.SendAnimation;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.Chat;
import org.telegram.telegrambots.meta.api.objects.Message;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;
import org.telegram.telegrambots.meta.logging.BotLogger;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.logging.Level;
import java.util.logging.Logger;


public class NotifyBot extends TelegramLongPollingCommandBot {
    private static final Logger LOG = Logger.getLogger(NotifyBot.class.getName());

    private final String token;


    public NotifyBot(DefaultBotOptions defaultBotOptions, String token, String name) {
        super(defaultBotOptions,name);
        this.token = token;

        Arrays.asList(
                new StartCommand(),
                new HelpCommand(),
                new AddGifComand()
        ).forEach(this::register);
    }

//    @Override
//    public void onUpdatesReceived(List<Update> updates) {
//        for (Update update : updates) {
//            if(update.sendHttpPostRequest().hasAnimation()){
//                BotLogger.info("LISTENER_CHAT_ANIMATION", update.sendHttpPostRequest().getAnimation().toString());
//            }
//        }
//    }



    public void sendMessage(Long chatId, String message) {
        final SendMessage sendMessageRequest = new SendMessage();
        sendMessageRequest.setChatId(chatId.toString());
        sendMessageRequest.setText(message);
        sendMessageRequest.enableMarkdown(true);

        try {
            execute(sendMessageRequest);
        } catch (TelegramApiException e) {
            LOG.log(Level.SEVERE, String.format(
                    "NotifyBot: Error while sending message: %s%n%s", chatId, message), e);
        }
    }

    public void sendMessage(Long chatId, final SendAnimation animation) {
        try {
            animation.enableNotification();
            execute(animation.setChatId(chatId.toString()));
        } catch (TelegramApiException e) {
            LOG.log(Level.SEVERE, String.format(
                    "NotifyBot: Error while sending animation: %s%n%s", chatId, animation.getAnimation().getAttachName()), e);
        }
    }

    @Override
    public Message execute(SendAnimation sendAnimation) throws TelegramApiException {
        if (sendAnimation == null) {
            throw new TelegramApiException("Parameter " + sendAnimation + " can not be null");
        }
        sendAnimation.validate();
        try {
            String url = getBaseUrl() + SendAnimation.PATH;
            HttpPost httppost = new HttpPost(url);
            httppost.setConfig(getOptions().getRequestConfig());
            MultipartEntityBuilder builder = MultipartEntityBuilder.create();
            builder.setLaxMode();
            builder.setCharset(StandardCharsets.UTF_8);
            builder.addTextBody(SendAnimation.CHATID_FIELD, sendAnimation.getChatId(), ContentType.create("text/plain", StandardCharsets.UTF_8));
            builder.addTextBody(SendAnimation.ANIMATION_FIELD, sendAnimation.getAnimation().getAttachName(), ContentType.create("text/plain", StandardCharsets.UTF_8));

            if (sendAnimation.getDisableNotification() != null) {
                builder.addTextBody(SendAnimation.DISABLENOTIFICATION_FIELD, sendAnimation.getDisableNotification().toString(), ContentType.create("text/plain", StandardCharsets.UTF_8));
            }

            if (sendAnimation.getCaption() != null) {
                builder.addTextBody(SendAnimation.CAPTION_FIELD, sendAnimation.getCaption(), ContentType.create("text/plain", StandardCharsets.UTF_8));
                if (sendAnimation.getParseMode() != null) {
                    builder.addTextBody(SendAnimation.PARSEMODE_FIELD, sendAnimation.getParseMode(), ContentType.create("text/plain", StandardCharsets.UTF_8));
                }
            }
            HttpEntity multipart = builder.build();
            httppost.setEntity(multipart);
            return sendAnimation.deserializeResponse(sendHttpPostRequest(httppost));
        } catch (IOException e) {
            throw new TelegramApiException("Unable to edit message media", e);
        }
    }

    private String sendHttpPostRequest(HttpPost httppost) throws IOException {
        try (CloseableHttpResponse response = TelegramHttpClientBuilder.build(getOptions()).execute(httppost, getOptions().getHttpContext())) {
            HttpEntity ht = response.getEntity();
            BufferedHttpEntity buf = new BufferedHttpEntity(ht);
            return EntityUtils.toString(buf, StandardCharsets.UTF_8);
        }
    }

    public void sendMessage(Long chatId,
                            String message, Run<?, ?> run, FilePath filePath, TaskListener taskListener)
            throws IOException, InterruptedException {

        String logMessage = message;
        try {
            logMessage = TokenMacro.expandAll(run, filePath, taskListener, message);
        } catch (MacroEvaluationException e) {
            LOG.log(Level.SEVERE, "Error while expanding the message", e);
        }

        try {
            final String finalLogMessage = logMessage;

            this.sendMessage(chatId, finalLogMessage);

        } catch (Exception e) {
            LOG.log(Level.SEVERE, "Error while sending the message", e);
        }

        if (BotRunner.getInstance().getConfig().shouldLogToConsole()) taskListener.getLogger().println(logMessage);
    }

    @Override
    public void processNonCommandUpdate(Update update) {
        if (update == null) {
            LOG.log(Level.WARNING, "Update is null");
            return;
        }

        final String nonCommandMessage = BotRunner.getInstance().getConfig().getBotStrings()
                .get("message.noncommand");

        final Message message = update.getMessage();
        final Chat chat = message.getChat();

        if(update.getMessage().hasAnimation()){
            BotLogger.info("LISTENER_CHAT_ANIMATION", update.getMessage().getAnimation().toString());
        }

        if (chat.isUserChat()) {
            sendMessage(chat.getId(), nonCommandMessage);
            return;
        }

        final String text = message.getText();

        try {
            if(null == text || "".equals(text))return;
            final String[] tmp = text.split(" ");
            if (text.length() < 1 || text.charAt(0) != '@' || null == tmp[0]) return;
            if (tmp.length < 2 || !BotRunner.getInstance().getConfig().getBotName().equals(tmp[0].substring(1, tmp[0].length()))) return;
        } catch (Exception e) {
            LOG.log(Level.SEVERE, "Something bad happened while message processing", e);
            return;
        }

        sendMessage(chat.getId(), nonCommandMessage);
    }

    @Override
    public String getBotToken() {
        return token;
    }

    @Override
    public String toString() {
        return "NotifyBot{" + token + "}";
    }

}
