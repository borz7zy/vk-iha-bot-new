package com.fsoft.vktest.Communication.Account.Telegram;

import com.android.volley.TimeoutError;
import com.fsoft.vktest.AnswerInfrastructure.AnswerDatabase.Attachment;
import com.fsoft.vktest.AnswerInfrastructure.MessageBase;
import com.fsoft.vktest.ApplicationManager;
import com.fsoft.vktest.Communication.Account.VK.VkAccount;
import com.fsoft.vktest.Modules.CommandModule;
import com.fsoft.vktest.Utils.F;
import com.fsoft.vktest.Utils.User;

import java.util.ArrayList;

//todo определять чаты и игнорить их, заполнять поля message
public class MessageProcessor extends CommandModule {
    private TgAccount tgAccount = null;
    private long lastUpdateId = 0;
    private boolean isRunning = false;
    private boolean isChatsEnabled = true;
    private int errors = 0;
    private Runnable onMessagesReceivedCounterChangedListener = null;
    private int messagesReceivedCounter = 0;
    private Runnable onMessagesSentCounterChangedListener = null;
    private int messagesSentCounter = 0;


    public MessageProcessor(ApplicationManager applicationManager, TgAccount tgAccount) {
        super(applicationManager);
        this.tgAccount = tgAccount;
        isChatsEnabled = tgAccount.getFileStorage().getBoolean("chatsEnabled", isChatsEnabled);
    }

    @Override public void stop() {
        super.stop();
        stopModule();
    }

    public void startModule(){
        log("Обработка сообщений для аккаунта "+tgAccount+" запускается...");
        isRunning = true;
        new Thread(new Runnable() {
            @Override
            public void run() {
                update();
            }
        }).start();
    }

    public void stopModule(){
        log("Обработка сообщений для аккаунта "+tgAccount+" останавливается...");
        isRunning = false;
    }

    public int getMessagesReceivedCounter() {
        return messagesReceivedCounter;
    }

    public void inctementMessagesReceivedCounter(){
        messagesReceivedCounter++;
        if(onMessagesReceivedCounterChangedListener != null)
            onMessagesReceivedCounterChangedListener.run();
    }

    public int getMessagesSentCounter() {
        return messagesSentCounter;
    }

    public void inctementMessagesSentCounter(){
        messagesSentCounter++;
        if(onMessagesSentCounterChangedListener != null)
            onMessagesSentCounterChangedListener.run();
    }

    public boolean isChatsEnabled() {
        return isChatsEnabled;
    }

    public void setChatsEnabled(boolean chatsEnabled) {
        isChatsEnabled = chatsEnabled;
        tgAccount.getFileStorage().put("chatsEnabled", isChatsEnabled).commit();
    }

    public void setOnMessagesReceivedCounterChangedListener(Runnable onMessagesReceivedCounterChangedListener) {
        this.onMessagesReceivedCounterChangedListener = onMessagesReceivedCounterChangedListener;
    }

    public void setOnMessagesSentCounterChangedListener(Runnable onMessagesSentCounterChangedListener) {
        this.onMessagesSentCounterChangedListener = onMessagesSentCounterChangedListener;
    }

    public void update(){
        new Thread(new Runnable() {
            @Override
            public void run() {
                updateAsync();
            }
        }).start();
    }

    public void updateAsync(){
        log(". Sending request for "+tgAccount+" update...");
        tgAccount.getUpdates(new TgAccountCore.GetUpdatesListener() {
            @Override
            public void gotUpdates(final ArrayList<Update> updates) {
                new Thread(new Runnable() {
                    @Override
                    public void run() {
                        try {
                            log(". Received " + updates.size() + " " + tgAccount + " updates.");
                            errors = 0;
                            if (isRunning) {
                                processUpdates(updates);
                                update();
                            }
                        }
                        catch (Exception e){
                            e.printStackTrace();
                            log("! Ошибка "+e.getMessage()+" обработки апдейтов аккаунта " + tgAccount);
                        }
                    }
                }).start();
            }

            @Override
            public void error(final Throwable error) {
                new Thread(new Runnable() {
                    @Override
                    public void run() {
                        log(". Error getting "+tgAccount+" updates: " + error.getClass().getName() + " " +error.getMessage()+". Retry...");
                        errors ++;
//                        if(errors > 20){
//                            tgAccount.stopAccount();
//                            tgAccount.state("Слишком много ошибок " + error.getClass().getName() + " " +error.getMessage());
//                        }
                        if(isRunning) {
                            if(errors != 0) {
                                int wait = errors;
                                if(wait > 60)
                                    wait = 60;
                                log(tgAccount.state("Ожидание " + wait + " секунд после ошибки..."));
                                F.sleep(wait * 1000);
                            }
                            update();
                        }
                    }
                }).start();
            }
        }, lastUpdateId+1, 20);
    }

    public void processUpdates(ArrayList<Update> updates){
        for (Update update:updates){
            if(update.getUpdate_id() > lastUpdateId)
                lastUpdateId = update.getUpdate_id();
            if(update.getMessage() != null){
                processMessage(update.getMessage());
            }
        }
    }

    public void processMessage(final Message message){
        log(". ПОЛУЧЕНО СООБЩЕНИЕ: " + message);
        inctementMessagesReceivedCounter();

        //заполняем юзера
        com.fsoft.vktest.Utils.User brainUser = new User();
        brainUser.setName(message.getFrom().getName());
        brainUser.setNetwork(User.NETWORK_TELEGRAM);
        brainUser.setId(message.getFrom().getId());

        //функция отправки ответа юзеру
        com.fsoft.vktest.AnswerInfrastructure.Message.OnAnswerReady onAnswerReady;
        onAnswerReady = new com.fsoft.vktest.AnswerInfrastructure.Message.OnAnswerReady() {
            @Override
            public void sendAnswer(com.fsoft.vktest.AnswerInfrastructure.Message answer) {
                if(!answer.hasAnswer())
                    return;
                String replyText = answer.getAnswer().text;
                log("\n.");
                log("\nТы: " + message.getFrom());
                log("\nТы написал: " + message.getText());
                log("\nПринято сообщений: " + messagesReceivedCounter);
                log("\nОтправлено сообщений: " + messagesSentCounter);
                log("\nВыполнено запросов к API: " + tgAccount.getApiCounter());
                log("\nОшибок при доступе к API: " + tgAccount.getErrorCounter());
                int attachmentsSent = 0;

                final TgAccountCore.SendMessageListener listener = new TgAccountCore.SendMessageListener() {
                    @Override
                    public void sentMessage(Message message) {
                        log(". Отправлено сообщение: " + message);
                        inctementMessagesSentCounter();
                    }

                    @Override
                    public void error(Throwable error) {
                        log(error.getClass().getName() + " while sending message");
                    }
                };

                //если в сообщении есть вложения, отправим их
                for(Attachment attachment:answer.getAnswer().attachments){
                    try {
                        java.io.File file = attachment.getFile();
                        if(attachment.isPhoto()) {
                            if(attachment.hasTgCache())
                                tgAccount.sendPhoto(listener, message.getChat().getId(), replyText, attachment.getTgCache().getId());
                            else
                                tgAccount.sendPhoto(listener, message.getChat().getId(), replyText, file);
                            attachmentsSent++;
                        }
                        if(attachment.isDoc()) {
                            if(attachment.hasTgCache())
                                tgAccount.sendDocument(listener, message.getChat().getId(), replyText, attachment.getTgCache().getId());
                            else
                                tgAccount.sendDocument(listener, message.getChat().getId(), replyText, file);
                            attachmentsSent++;
                        }
                        if(attachment.isAudio()) {
                            if(attachment.hasTgCache())
                                tgAccount.sendAudio(listener, message.getChat().getId(), replyText, attachment.getTgCache().getId());
                            else
                                tgAccount.sendAudio(listener, message.getChat().getId(), replyText, file);
                            attachmentsSent++;
                        }
                    }
                    catch (Exception e){
                        e.printStackTrace();
                        log("! Ошибка получения файла вложения: " + e.getMessage());
                    }
                }
                //если вложений нет, отправим текстом
                if(attachmentsSent == 0)
                    tgAccount.sendMessage(new TgAccountCore.SendMessageListener() {
                    @Override
                    public void sentMessage(Message message) {
                        log(". Отправлено сообщение: " + message);
                        inctementMessagesSentCounter();
                    }

                    @Override
                    public void error(Throwable error) {
                        log(error.getClass().getName() + " while sending message");
                    }
                }, message.getChat().getId(), replyText);
            }
        };

        //формирует объект и вызываем систему
        com.fsoft.vktest.AnswerInfrastructure.Message brainMessage;
        brainMessage = new com.fsoft.vktest.AnswerInfrastructure.Message(
                MessageBase.SOURCE_CHAT,
                message.getText(),
                brainUser,
                tgAccount,
                onAnswerReady
        );
        brainMessage.setMessage_id(message.getMessage_id());
        brainMessage.setChat_id(message.getChat().getId());
        brainMessage.setChat_title(message.getChat().getTitle());
        if(message.getReply_to_message() != null && message.getReply_to_message().getFrom() != null) {
            User mentioned = new User().tg(message.getReply_to_message().getFrom().getId());
            mentioned.setUsername(message.getReply_to_message().getFrom().getUsername());
            mentioned.setName(message.getReply_to_message().getFrom().getName());
            brainMessage.getMentions().add(mentioned);
        }

        applicationManager.getBrain().processMessage(brainMessage);
    }
}
