package com.fsoft.vktest.Communication.Account.VK;

import android.util.Log;

import com.fsoft.vktest.AnswerInfrastructure.Answer;
import com.fsoft.vktest.ApplicationManager;
import com.fsoft.vktest.Modules.CommandModule;
import com.fsoft.vktest.Utils.F;
import com.fsoft.vktest.Utils.CommandParser;
import com.fsoft.vktest.Utils.Parameters;
import com.perm.kate.api.Attachment;
import com.perm.kate.api.Message;
import com.fsoft.vktest.Modules.Commands.CommandDesc;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;

/**
 * Этот модуль отвечает за работу с личными сообщениями. В том числе за работу с диалогами, фильтрацией флуда и т.д.
 * Created by Dr. Failov on 16.02.2017.
 */
public class MessageProcessor extends CommandModule {
    private VkAccount vkAccount = null;
    private OffTopCounter offTopCounter = null;
    private BotsChatsFilter botsChatsFilter = null;
    private Instructor instructor = null;
    private Guard guard = null;
    private Timer messageProcessingTimer = null;
    private ArrayList<Long> excludedDialogs = new ArrayList<>(); //список чатов, которые бот будет игнорировать.
    private long lastMessageProcessed = 0;

    private boolean messageProcessingEnabled = true;
    private boolean answerInChatsEnabled = true;
    private boolean forwardMessagesInChatsEnabled = false;
    private int messageScanIntervalSec = 12;
    private long messageReceivedCounter = 0;
    private long messageSentCounter = 0;
    private Date counterResetDate = new Date();


    public MessageProcessor(ApplicationManager applicationManager, VkAccount vkAccount) {
        super(applicationManager);
        this.vkAccount = vkAccount;
        offTopCounter = new OffTopCounter(applicationManager);
        botsChatsFilter = new BotsChatsFilter(applicationManager);
        instructor = new Instructor(applicationManager);
        guard = new Guard(applicationManager);
        childCommands.add(offTopCounter);
        childCommands.add(botsChatsFilter);
        childCommands.add(instructor);
        childCommands.add(guard);

        messageProcessingEnabled = vkAccount.getFileStorage().getBoolean("messageProcessingEnabled", messageProcessingEnabled);
        forwardMessagesInChatsEnabled = vkAccount.getFileStorage().getBoolean("replyMessagesInChats", forwardMessagesInChatsEnabled);
        answerInChatsEnabled = vkAccount.getFileStorage().getBoolean("answerInChatsEnabled", answerInChatsEnabled);
        messageScanIntervalSec = vkAccount.getFileStorage().getInt("messageScanInterval", messageScanIntervalSec);

        messageReceivedCounter = vkAccount.getFileStorage().getLong("messageReceivedCounter", messageReceivedCounter);
        messageSentCounter = vkAccount.getFileStorage().getLong("messageSentCounter", messageSentCounter);
        counterResetDate = vkAccount.getFileStorage().getDate("counterResetDate", counterResetDate);

        childCommands.add(new MessageProcessing(applicationManager));
        childCommands.add(new AnswerInChats(applicationManager));
        childCommands.add(new ForwardMessagesInChats(applicationManager));
        childCommands.add(new MessageScanInterval(applicationManager));
        childCommands.add(new BackToChat(applicationManager));
        childCommands.add(new LeaveFromChat(applicationManager));
        childCommands.add(new InviteToChat(applicationManager));
        childCommands.add(new ResetMessageCounters(applicationManager));
    }
    @Override public void stop() {
        super.stop();
        stopModule();
    }
    public void startModule(){
        botsChatsFilter.start();
        guard.runGuard();
        if(messageProcessingTimer == null && messageProcessingEnabled){
            messageProcessingTimer = new Timer("message processing for " + vkAccount);
            new Thread(new Runnable() {
                @Override
                public void run() {
                    log(". Подготовка к запуску обработки сообщений аккаунта " + vkAccount+ " ...");
                    ArrayList<Message> messages = vkAccount.getMessages50();
                    for (Message m : messages)
                        setLastMessageProcessed(m.mid);
                    log(". Запуск обработки сообщений аккаунта " + vkAccount + " ...");
                    messageProcessingTimer.schedule(new TimerTask() {
                        @Override
                        public void run() {
                            try {
                                if (messageProcessingTimer != null)
                                    findNewMessages();
                            } catch (Throwable e) {
                                e.printStackTrace();
                                log("Ошибка обработки сообщений: " + e.toString());
                            }
                        }
                    }, 1000 * messageScanIntervalSec, 1000 * messageScanIntervalSec);
                }
            }).start();
        }
    }
    public void stopModule(){
        log(". Остановка обработки сообщений аккаунта " + vkAccount + " ...");
        botsChatsFilter.stop();
        guard.stopGuard();
        if (messageProcessingTimer != null) {
            messageProcessingTimer.cancel();
            messageProcessingTimer = null;
            lastMessageProcessed = 0;
            log(". Обработка сообщений аккаунта " + vkAccount + " остановлена.");
        }
    }

    public long getMessageReceivedCounter(){
        return messageReceivedCounter;
    }
    public long getMessageSentCounter() {
        return messageSentCounter;
    }
    public void setForwardMessagesInChatsEnabled(boolean forwardMessagesInChatsEnabled) {
        this.forwardMessagesInChatsEnabled = forwardMessagesInChatsEnabled;
        vkAccount.getFileStorage().put("replyMessagesInChats", forwardMessagesInChatsEnabled).commit();
    }
    public void setAnswerInChatsEnabled(boolean answerInChatsEnabled) {
        this.answerInChatsEnabled = answerInChatsEnabled;
        vkAccount.getFileStorage().put("answerInChatsEnabled", answerInChatsEnabled).commit();
    }
    public void setMessageScanIntervalSec(int messageScanIntervalSec) {
        this.messageScanIntervalSec = messageScanIntervalSec;
        vkAccount.getFileStorage().put("messageScanInterval", messageScanIntervalSec).commit();
    }
    public void setMessageProcessingEnabled(boolean messageProcessingEnabled){
        this.messageProcessingEnabled = messageProcessingEnabled;
        vkAccount.getFileStorage().put("messageProcessingEnabled", messageProcessingEnabled).commit();
        log(". Обработка сообщений для аккаунта " + vkAccount + " = " + messageProcessingEnabled);
        if(messageProcessingEnabled)
            startModule();
        else
            stopModule();
    }
    private void setLastMessageProcessed(long maxMessageId){
        lastMessageProcessed = Math.max(maxMessageId, lastMessageProcessed);
    }
    public void setMessageSentCounter(long messageSentCounter) {
        this.messageSentCounter = messageSentCounter;
        vkAccount.getFileStorage().put("messageSentCounter", messageSentCounter).commit();
    }
    public void setMessageReceivedCounter(long messageReceivedCounter) {
        this.messageReceivedCounter = messageReceivedCounter;
        vkAccount.getFileStorage().put("messageReceivedCounter", messageReceivedCounter).commit();
    }
    public void setCounterResetDate(Date counterResetDate) {
        this.counterResetDate = counterResetDate;
        vkAccount.getFileStorage().put("counterResetDate", counterResetDate).commit();
    }

    private boolean isNewMessage(long messageID){
        return messageID > lastMessageProcessed;
    }
    private void findNewMessages(){
        if(!applicationManager.isRunning()){//значит программа уже завершена
            stopModule();
            return;
        }
        if(lastMessageProcessed == 0) //значит программа не инициализирована
            return;
        guard.lastTimeReadMessage = System.currentTimeMillis();
        long maxMessageId = 0;
        try {
            ArrayList<Message> messages = vkAccount.getMessages50();
            for (int i = messages.size()-1; i >= 0; i--) {
                Message message = messages.get(i);
                maxMessageId = Math.max(maxMessageId, message.mid);
                if (isNewMessage(message.mid))
                    processNewMessage(messages.get(i));
            }
        } catch (Throwable e) {
            e.printStackTrace();
            log("! ошибка чтения сообщений у " + vkAccount + ": " + e.toString());
        }
        setLastMessageProcessed(maxMessageId);
    }
    private void processNewMessage(final Message kateMessage){
        new Thread(new Runnable() {
            @Override
            public void run() {
                messageReceivedCounter++;
                if(kateMessage.chat_id != null && !answerInChatsEnabled) //если это беседа и их обработка отключена
                    return;
                String messageText = kateMessage.body.replaceAll(" +", " ");
                log(". MESS ("+vkAccount+"): " + messageText);

                //подготовить обьект для передачи в программу
                String text = kateMessage.body.replaceAll(" +", " ");
                long author = kateMessage.uid;
                String source = com.fsoft.vktest.AnswerInfrastructure.Message.SOURCE_DIALOG;
                ArrayList<Attachment> attachments = kateMessage.attachments;
                com.fsoft.vktest.AnswerInfrastructure.Message.OnAnswerReady onAnswerReady =
                        new com.fsoft.vktest.AnswerInfrastructure.Message.OnAnswerReady() {
                    @Override
                    public void sendAnswer(com.fsoft.vktest.AnswerInfrastructure.Message answer) {
                        MessageProcessor.this.sendAnswer(answer);
                    }
                };
                com.fsoft.vktest.AnswerInfrastructure.Message message =
                        new com.fsoft.vktest.AnswerInfrastructure.Message(
                                source, text, author, attachments, vkAccount, onAnswerReady);
                message.setMessage_id(kateMessage.mid);
                if(kateMessage.chat_id != null) {
                    message.setSource(com.fsoft.vktest.AnswerInfrastructure.Message.SOURCE_CHAT);
                    message.setChat_id(kateMessage.chat_id);
                    message.setSource_id(kateMessage.chat_id);
                    message.setChat_users(kateMessage.chat_members);
                    message.setChat_title(kateMessage.title);
                }


                //отправить статус "набирает сообщение"
                if(applicationManager.getBrain() != null && (applicationManager.getBrain().hasTreatment(message) || applicationManager.getBrain().hasCommandMark(message)))
                    if(!vkAccount.isGroupToken())
                        vkAccount.markTyping(kateMessage.uid, kateMessage.chat_id);

                //успокоить охранника
                guard.registerRead();

                //найти ответ
                applicationManager.getBrain().processMessage(message);
            }
        }).start();
    }
    private void sendAnswer(com.fsoft.vktest.AnswerInfrastructure.Message message){
        if(message == null)
            return;
        if (message.getAnswer() != null && !message.getAnswer().text.equals("")) {
            log(". REPL ("+vkAccount+"): " + message.getAnswer().text);
            //send
            vkAccount.markAsRead(message.getMessage_id());
            messageSentCounter++;
            if(message.getSource().equals(com.fsoft.vktest.AnswerInfrastructure.Message.SOURCE_DIALOG))         //ЛИЧКА
                vkAccount.sendMessage(message.getAuthor(), 0L, message.getAnswer());
            else if(answerInChatsEnabled) {             //ЧАТ
                offTopCounter.registerNotOffTopMessage(message.getChat_id());

                if(forwardMessagesInChatsEnabled)
                    message.getAnswer().forwarded.add(message.getMessage_id());
                vkAccount.sendMessage(0L, message.getChat_id(), message.getAnswer());
            }
        }
        else{
            if(message.getSource().equals(com.fsoft.vktest.AnswerInfrastructure.Message.SOURCE_CHAT))
                offTopCounter.registerOffTopMessage(message.getChat_id());
            if(instructor.needInstruction(message.getAuthor())
                    && !applicationManager.getBrain().hasBotMark(message)
                    && !applicationManager.getBrain().getIgnor().has(message.getAuthor())
                    && message.getSource().equals(com.fsoft.vktest.AnswerInfrastructure.Message.SOURCE_DIALOG)){
                vkAccount.markAsRead(message.getMessage_id());
                log("! Инструкция (" + vkAccount+ "): " + instructor.getInstructionText());
                vkAccount.sendMessage(message.getAuthor(), 0L, new Answer(instructor.getInstructionText()));
            }
        }
    }



    class Status extends CommandModule{
        public Status(ApplicationManager applicationManager) {
            super(applicationManager);
        }
        public @Override String processCommand(com.fsoft.vktest.AnswerInfrastructure.Message message) {
            if(message.getText().equals("status") || message.getText().equals("acc status"))
                return
                        "Аккаунт " + vkAccount + " обработка сообщений: " + (messageProcessingEnabled?"включена":"выключена") + "\n" +
                        "Аккаунт " + vkAccount + " интервал сканирования сообщений: " + messageScanIntervalSec + " секунд\n" +
                        "Аккаунт " + vkAccount + " сообщения сейчас: " + (messageProcessingTimer != null?"обрабатываются":"не обрабатываются") + "\n" +
                        "Аккаунт " + vkAccount + " принято: "+ messageReceivedCounter + " сообщений\n" +
                        "Аккаунт " + vkAccount + " отправлено: "+ messageSentCounter + " сообщений\n" +
                        "Аккаунт " + vkAccount + " дата сброса счётчиков сообщений: "+ sdf.format(counterResetDate) + " \n" +
                        "Аккаунт " + vkAccount + " обработка бесед: "+ (answerInChatsEnabled?"включена":"выключена") + "\n" +
                        "Аккаунт " + vkAccount + " пересылать сообщения в беседах: "+ (forwardMessagesInChatsEnabled?"да":"нет") + "\n";
            return "";
        }
        public @Override ArrayList<CommandDesc> getHelp() {
            return new ArrayList<>();
        }
    }
    class MessageProcessing extends CommandModule{
        public MessageProcessing(ApplicationManager applicationManager) {
            super(applicationManager);
        }

        @Override
        public String processCommand(com.fsoft.vktest.AnswerInfrastructure.Message message) {
            CommandParser commandParser = new CommandParser(message.getText());
            if(commandParser.getWord().equals("acc"))
                if(vkAccount.isMine(commandParser.getWord()))
                    if(commandParser.getWord().toLowerCase().equals("messageprocessing")) {
                        setMessageProcessingEnabled(commandParser.getBoolean());
                        if(messageProcessingEnabled)
                            return "Обработка сообщений включена, "+vkAccount+" будет проверять личку и отвечать на сообщения в личке.";
                        else
                            return "Обработка сообщений отключена, "+vkAccount+" не будет проверять личку.\n" +
                                    "Обрати внивание, "+vkAccount+" теперь в личке даже на команды отвечать не будет.";
                    }
            return "";
        }

        @Override
        public ArrayList<CommandDesc> getHelp() {
            ArrayList<CommandDesc> result = new ArrayList<>();
            result.add(new CommandDesc("Включить или выключить обработку личных сообщений для аккаунта "+vkAccount,
                    "Этот параметр позволяет полностью отключить работу в личке. " +
                            "Тогда бот будет работать только на стенах.",
                    "botcmd acc " + vkAccount.getId() + " messageProcessing <on/off>"));
            return result;
        }
    }
    class AnswerInChats extends CommandModule {
        public AnswerInChats(ApplicationManager applicationManager) {
            super(applicationManager);
        }

        @Override
        public String processCommand(com.fsoft.vktest.AnswerInfrastructure.Message message) {
            CommandParser commandParser = new CommandParser(message.getText());
            if(commandParser.getWord().equals("acc"))
                if(vkAccount.isMine(commandParser.getWord()))
                    if(commandParser.getWord().toLowerCase().equals("answerinchats")) {
                        setAnswerInChatsEnabled(commandParser.getBoolean());
                        if(answerInChatsEnabled)
                            return "Теперь "+vkAccount+" будет отвечать также и в беседах, где много людей.";
                        else
                            return "Теперь "+vkAccount+" будет отвечать только в диалогах, где один человек.";
                    }
            return "";
        }

        @Override
        public ArrayList<CommandDesc> getHelp() {
            ArrayList<CommandDesc> result = new ArrayList<>();
            result.add(new CommandDesc(
                    "Переключить обработку бесед для аккаунта "+vkAccount,
                    "Если ты не хочешь, чтобы с ботом могли общатся в чатах (где много людей), то ты " +
                            "можешь отключить ответы в чатах. Тогда бот будет отвечать только в ЛС (один-на-один).",
                    "botcmd acc " + vkAccount.getId() + " answerInChats <on/off>"));
            return result;
        }
    }
    class ForwardMessagesInChats extends CommandModule{
        public ForwardMessagesInChats(ApplicationManager applicationManager) {
            super(applicationManager);
        }

        @Override
        public String processCommand(com.fsoft.vktest.AnswerInfrastructure.Message message) {
            CommandParser commandParser = new CommandParser(message.getText());
            if(commandParser.getWord().equals("acc"))
                if(vkAccount.isMine(commandParser.getWord()))
                    if(commandParser.getWord().toLowerCase().equals("forwardmessagesinchats")) {
                        setForwardMessagesInChatsEnabled(commandParser.getBoolean());
                        if(forwardMessagesInChatsEnabled)
                            return vkAccount+" будет в беседах прикреплять к своим ответам " +
                                    "пересланное сообщение на которое он отвечает.";
                        else
                            return vkAccount+" не будет прикреплять к своим ответам " +
                                    "пересланное сообщение на которое он отвечает.";
                    }
            return "";
        }

        @Override
        public ArrayList<CommandDesc> getHelp() {
            ArrayList<CommandDesc> result = new ArrayList<>();
            result.add(new CommandDesc("Вкл\\выкл пересылку сообщений в беседах для аккаунта "+vkAccount,
                    "Часто бывает, что в беседах боту пишет сразу много народу, и непонятно кому и на что ответил бот.\n" +
                            "Если опция включена, бот будет прикреплять к своему ответу сообщение на которое он отвечает, если в беседе",
                    "botcmd acc " + vkAccount.getId() + " ForwardMessagesInChats <on/off>"));
            return result;
        }
    }
    class MessageScanInterval extends CommandModule{
        public MessageScanInterval(ApplicationManager applicationManager) {
            super(applicationManager);
        }

        @Override
        public String processCommand(com.fsoft.vktest.AnswerInfrastructure.Message message) {
            CommandParser commandParser = new CommandParser(message.getText());
            if(commandParser.getWord().equals("acc"))
                if(vkAccount.isMine(commandParser.getWord()))
                    if(commandParser.getWord().toLowerCase().equals("messagescaninterval")) {
                        int newValue = commandParser.getInt();
                        if(newValue < 3)
                            return "Прости, что вмешиваюсь, но " + newValue + " секунды - это очень " +
                                    "малое значение для интервала. Я не буду его сохранять.\n" +
                                    "Бот будет создавать слишком большую нагрузку на сервера ВК и " +
                                    "попадёт на капчу. Поставь побольше.";
                        setMessageScanIntervalSec(newValue);
                        stopModule();
                        new Timer().schedule(new TimerTask() {
                            @Override
                            public void run() {
                                startModule();
                            }
                        }, 5000);
                        return "Теперь " + vkAccount+ " будет проверять личку каждые " +
                                messageScanIntervalSec + " секунд. \nИзменения вступят в силу " +
                                "через несколько секунд.";
                    }
            return "";
        }

        @Override
        public ArrayList<CommandDesc> getHelp() {
            ArrayList<CommandDesc> result = new ArrayList<>();
            result.add(new CommandDesc("Задать период проверки ЛС для аккаунта "+vkAccount,
                    "Интервал определяет то, как часто бот будет проверять наличие новых сообщений.\n" +
                            "Если интервал меньше 10 секунд, бот будет создавать слишком большую нагрузку на " +
                            "сервера ВК и он может получить капчу.\n" +
                            "Изначально интервал установлена на 12 секунд.\n" +
                            "Если ты хочешь, чтобы бот не так много жрал мегабайты, поставь интервал побольше.",
                    "botcmd acc " + vkAccount.getId() + " MessageScanInterval <число, интервал сканирования в секундах>"));
            return result;
        }
    }
    class ResetMessageCounters extends CommandModule {
        public ResetMessageCounters(ApplicationManager applicationManager) {
            super(applicationManager);
        }

        @Override
        public String processCommand(com.fsoft.vktest.AnswerInfrastructure.Message message) {
            CommandParser commandParser = new CommandParser(message.getText());
            if(commandParser.getWord().equals("acc"))
                if(vkAccount.isMine(commandParser.getWord()))
                    if(commandParser.getWord().toLowerCase().equals("resetmessagecounters")) {
                        setMessageReceivedCounter(0);
                        setMessageSentCounter(0);
                        setCounterResetDate(new Date());
                        return "Счётчики отправленных и принятых " + vkAccount + " сообщений были сброшены.";
                    }
            return "";
        }

        @Override
        public ArrayList<CommandDesc> getHelp() {
            ArrayList<CommandDesc> result = new ArrayList<>();
            result.add(new CommandDesc(
                    "Сбросить счётчики сообщений аккаунта "+vkAccount,
                    "Счётчики сообщений сохраняются после перезапуска бота. Чтобы их можно было сбросить, " +
                            "используется эта команда.",
                    "botcmd acc " + vkAccount.getId() + " ResetMessageCounters"));
            return result;
        }
    }

    class BackToChat extends CommandModule{
        public BackToChat(ApplicationManager applicationManager) {
            super(applicationManager);
        }

        @Override
        public String processCommand(com.fsoft.vktest.AnswerInfrastructure.Message message) {
            CommandParser commandParser = new CommandParser(message.getText());
            if(commandParser.getWord().equals("acc"))
                if(vkAccount.isMine(commandParser.getWord()))
                    if(commandParser.getWord().toLowerCase().equals("backtochat")) {
                        long cid = commandParser.getLong();
                        if(vkAccount.backToChat(cid))
                            return vkAccount + " успешно вернулся в чат " + cid + ".";
                        else
                            return vkAccount + " не смог вернулся в чат " + cid + ".";
                    }
            return "";
        }

        @Override
        public ArrayList<CommandDesc> getHelp() {
            ArrayList<CommandDesc> result = new ArrayList<>();
            result.add(new CommandDesc("Вернуть бота "+vkAccount+" в беседу",
                    "Если бот когда-то вышел из беседы, этой командой можно заставить его вернуться в неё.\n"+
                    "ID беседы можно узнать командой GetChatList или посмотреть " +
                            "в строке адреса браузера.",
                    "botcmd acc " + vkAccount.getId() + " BackToChat <ID чата>"));
            return result;
        }
    }
    class LeaveFromChat extends CommandModule{
        public LeaveFromChat(ApplicationManager applicationManager) {
            super(applicationManager);
        }

        @Override
        public String processCommand(com.fsoft.vktest.AnswerInfrastructure.Message message) {
            CommandParser commandParser = new CommandParser(message.getText());
            if(commandParser.getWord().equals("acc"))
                if(vkAccount.isMine(commandParser.getWord()))
                    if(commandParser.getWord().toLowerCase().equals("leavefromchat")) {
                        Long cid = commandParser.getLong();
                        vkAccount.exitFromChatAsync(cid);
                        return "Я попытаюсь выйти из чата " + cid + " через 5 секунд.";
                    }
            return "";
        }

        @Override
        public ArrayList<CommandDesc> getHelp() {
            ArrayList<CommandDesc> result = new ArrayList<>();
            result.add(new CommandDesc("Выйти из беседы ... для аккаунта "+vkAccount,
                    "Эта команда заставит бота покинуть любую беседу, в которой он есть.\n" +
                            "ID беседы можно узнать командой GetChatList или посмотреть " +
                            "в строке адреса браузера.",
                    "---| botcmd acc " + vkAccount.getId() + " LeaveFromChat <ID беседы>"));
            return result;
        }
    }
    class InviteToChat extends CommandModule{
        public InviteToChat(ApplicationManager applicationManager) {
            super(applicationManager);
        }

        @Override
        public String processCommand(com.fsoft.vktest.AnswerInfrastructure.Message message) {
            CommandParser commandParser = new CommandParser(message.getText());
            if(commandParser.getWord().toLowerCase().equals("acc"))
                if(vkAccount.isMine(commandParser.getWord()))
                    if(commandParser.getWord().toLowerCase().equals("invitetochat")) {
                        Long cid = commandParser.getLong();
                        String userID = commandParser.getWord();
                        Long uid = vkAccount.resolveScreenName(userID);
                        if(uid > 0) {
                            if(vkAccount.addUserToChat(cid, uid))
                                return "Я добавил пользователя "+uid+" в чат "+cid+".";
                            else
                                return "Я пытался добавить пользователя "+uid+" в чат "+cid+", но что-то пошло не так.";
                        }
                    }
            return "";
        }

        @Override
        public ArrayList<CommandDesc> getHelp() {
            ArrayList<CommandDesc> result = new ArrayList<>();
            result.add(new CommandDesc("Пригласить пользователя в беседу с аккаунта "+vkAccount,
                    "С помощью этой команды ты можешь пригласить кого угодно в любую " +
                            "беседу в которой есть бот.\n" +
                            "ID беседы можно узнать командой GetChatList или посмотреть " +
                            "в строке адреса браузера.",
                    "botcmd acc " + vkAccount.getId() + " InviteToChat <ID чата> <ID пользователя>"));
            return result;
        }
    }

    class OffTopCounter extends CommandModule {
        private boolean exitFromOffTopChatsEnabled = true;
        private HashMap<Long, Integer> chatOffTopCounter = new HashMap<>();
        private long offtopChatsCounter = 0;

        public OffTopCounter(ApplicationManager applicationManager) {
            super(applicationManager);
            exitFromOffTopChatsEnabled = vkAccount.getFileStorage().getBoolean("exitFromOffTopChatsEnabled", exitFromOffTopChatsEnabled);
            offtopChatsCounter = vkAccount.getFileStorage().getLong("offtopChatsCounter", offtopChatsCounter);

            childCommands.add(new Status(applicationManager));
            childCommands.add(new GetChatCounter(applicationManager));
            childCommands.add(new ExitFromOffTopChats(applicationManager));
            childCommands.add(new GetChatList(applicationManager));
            childCommands.add(new FindChat(applicationManager));
        }
        public void registerOffTopMessage(Long chat_id){
            try {
                if (chat_id == null)
                    return;
                int current = getChatOffTopCounter(chat_id);
                chatOffTopCounter.put(chat_id, current + 1);
                int offTopWarning1Limit = applicationManager.getParameters().get(
                        "offTopWarning1Limit",
                        800,
                        "Количество оффтопа до первого предупреждения",
                        "Количество сообщений в чате не к боту, после которого участники чата получат ПЕРВОЕ предупреждение от бота.");
                int offTopWarning2Limit = applicationManager.getParameters().get(
                        "offTopWarning2Limit",
                        900,
                        "Количество оффтопа до второго предупреждения",
                        "Количество сообщений в чате не к боту, после которого участники чата получат ВТОРОЕ предупреждение от бота.");
                int offTopExitLimit = applicationManager.getParameters().get(
                        "offTopExitLimit",
                        1000,
                        "Количество оффтопа до выхода из чата",
                        "Количество сообщений в чате не к боту, после которого бот покинет чат.");
                String offTopWarning1Text = applicationManager.getParameters().get(
                        "offTopWarning1Text",
                        "Напоминаю, я бот, и я всё ещё здесь.",
                        "Текст первого предупреждения",
                        "Текст ПЕРВОГО предупреждения, которое получат участники чата, которые общаются не с ботом.");
                String offTopWarning2Text = applicationManager.getParameters().get(
                        "offTopWarning2Text",
                        "Зачем вы меня сюда пригласили, если не общаетесь со мной?",
                        "Текст первого предупреждения",
                        "Текст ВТОРОГО предупреждения, которое получат участники чата, которые общаются не с ботом.");
                String offTopExitText = applicationManager.getParameters().get(
                        "offTopExitText",
                        "Оставлю вас наедине, не буду мешать.",
                        "Текст выхода из чата",
                        "Текст, который получат участники чата перед тем, как бот покинет чат.");

                if (current == offTopWarning1Limit)
                    vkAccount.sendMessage(null, chat_id, new Answer(offTopWarning1Text));
                else if (current == offTopWarning2Limit)
                    vkAccount.sendMessage(null, chat_id, new Answer(offTopWarning2Text));
                else if (current > offTopExitLimit) {
                    vkAccount.sendMessage(null, chat_id, new Answer(offTopExitText));
                    excludedDialogs.add(chat_id);
                    vkAccount.exitFromChatAsync(chat_id);
                    chatOffTopCounter.remove(chat_id);
                    addOfftopChatsCounter();
                }
            }
            catch (Exception e){
                e.printStackTrace();
                log("! Ошибка обработки оффтоп сообщения: " + e.toString());
            }
        }
        public void registerNotOffTopMessage(Long chat_id) {
            if (chat_id == null)
                return;
            chatOffTopCounter.put(chat_id, 0);
        }

        private void setExitFromOffTopChatsEnabled(boolean exitFromOffTopChats) {
            this.exitFromOffTopChatsEnabled = exitFromOffTopChats;
            vkAccount.getFileStorage().put("exitFromOffTopChatsEnabled", exitFromOffTopChatsEnabled).commit();
        }
        private int getChatOffTopCounter(long cid){
            if(chatOffTopCounter.containsKey(cid))
                return chatOffTopCounter.get(cid);
            else
                return 0;
        }
        private void addOfftopChatsCounter() {
            offtopChatsCounter++;
            vkAccount.getFileStorage().put("offtopChatsCounter", offtopChatsCounter).commit();
        }

        class Status extends CommandModule{
            public Status(ApplicationManager applicationManager) {
                super(applicationManager);
            }
            public @Override String processCommand(com.fsoft.vktest.AnswerInfrastructure.Message message) {
                if(message.getText().equals("status") || message.getText().equals("acc status")) {
                    return
                            "Аккаунт " + vkAccount + " выходить из оффтоп-бесед: " + (exitFromOffTopChatsEnabled?"включено":"выключено") + "\n" +
                            "Аккаунт " + vkAccount + ": по причине оффтопа было покинуто " + offtopChatsCounter + " бесед\n";
                }
                return "";
            }
            public @Override ArrayList<CommandDesc> getHelp() {
                return new ArrayList<>();
            }
        }
        class ExitFromOffTopChats extends CommandModule{
            public ExitFromOffTopChats(ApplicationManager applicationManager) {
                super(applicationManager);
            }

            @Override
            public String processCommand(com.fsoft.vktest.AnswerInfrastructure.Message message) {
                CommandParser commandParser = new CommandParser(message.getText());
                if(commandParser.getWord().equals("acc"))
                    if(vkAccount.isMine(commandParser.getWord()))
                        if(commandParser.getWord().equals("exitfromofftopchats")) {
                            setExitFromOffTopChatsEnabled(commandParser.getBoolean());
                            if (exitFromOffTopChatsEnabled)
                                return "Теперь "+vkAccount+" будет выходить из бесед, " +
                                        "в которых с ним давно не общаются.";
                            else
                                return "Больше "+vkAccount+" не будет выходить из бесед, " +
                                        "в которых с ним давно не общаются.";
                        }
                return "";
            }

            @Override
            public ArrayList<CommandDesc> getHelp() {
                ArrayList<CommandDesc> result = new ArrayList<>();
                result.add(new CommandDesc("Включить или выключить выход из оффтоп-бесед для аккаунта "+vkAccount,
                        "Если эта функция включена, бот будет напоминать о себе в тех чатах, где ему давно не писали. " +
                                "А если его напоминания будут игнорировать, он будет покидать такие чаты.\n" +
                                "Текст и лимиты напоминаний можно поменять в списке параметров.",
                        "botcmd acc " + vkAccount.getId() + " exitfromofftopchats <on/off>"));
                return result;
            }
        }
        class GetChatCounter extends CommandModule{
            public GetChatCounter(ApplicationManager applicationManager) {
                super(applicationManager);
            }

            @Override
            public String processCommand(com.fsoft.vktest.AnswerInfrastructure.Message message) {
                CommandParser commandParser = new CommandParser(message.getText());
                if(commandParser.getWord().equals("acc"))
                    if(vkAccount.isMine(commandParser.getWord()))
                        if(commandParser.getWord().equals("getchatofftop")) {
                            String result = "Счетчики оффтопа по диалогам: \n";
                            Iterator<Map.Entry<Long, Integer>> iterator = chatOffTopCounter.entrySet().iterator();
                            while (iterator.hasNext()) {
                                Map.Entry<Long, Integer> cur = iterator.next();
                                result += "Диалог: " + cur.getKey() + ", сообщений оффтопа: " + cur.getValue() + " \n";
                            }
                            return result;
                        }
                return "";
            }

            @Override
            public ArrayList<CommandDesc> getHelp() {
                ArrayList<CommandDesc> result = new ArrayList<>();
                result.add(new CommandDesc("Получить счетчики оффтопа для чатов аккаунта "+vkAccount,
                        "Бот ведёт учёт сообщений, которые адресованы не боту для каждого чата.\n" +
                                "Эта команда позволяет увидеть, как давно в каком чате общались с ботом.",
                        "botcmd acc " + vkAccount.getId()+ " getchatofftop"));
                return result;
            }
        }
        class GetChatList extends CommandModule{
            public GetChatList(ApplicationManager applicationManager) {
                super(applicationManager);
            }

            @Override
            public String processCommand(com.fsoft.vktest.AnswerInfrastructure.Message message) {
                CommandParser commandParser = new CommandParser(message.getText());
                if(commandParser.getWord().equals("acc"))
                    if(vkAccount.isMine(commandParser.getWord()))
                        if(commandParser.getWord().toLowerCase().equals("getchatlist")) {
                            int count = commandParser.getInt();
                            if(count < 5) count = 20;
                            if(count > 200) count = 200;
                            int offset = commandParser.getInt();
                            String result = "Список диалогов аккаунта " + vkAccount + ": \n";
                            ArrayList<Message> dialogs = vkAccount.getDialogs(offset, count);
                            for (Message m:dialogs)
                                if(m.chat_id != null)
                                    result += m.chat_id + " : " + m.title + " ("+m.chat_members.size()+"уч., "+getChatOffTopCounter(m.chat_id)+"оффт.) \n";
                            return result;
                        }
                return "";
            }

            @Override
            public ArrayList<CommandDesc> getHelp() {
                ArrayList<CommandDesc> result = new ArrayList<>();
                result.add(new CommandDesc("Получить список бесед для аккаунта "+vkAccount,
                        "Эта команда позволит просматривать какие у " +
                                "бота сейчас активны беседы и что в них происходит.",
                        "botcmd acc " + vkAccount.getId()+ " GetChatList <количество> <сдвиг>"));
                return result;
            }
        }
        class FindChat extends CommandModule{
            public FindChat(ApplicationManager applicationManager) {
                super(applicationManager);
            }

            @Override
            public String processCommand(com.fsoft.vktest.AnswerInfrastructure.Message message) {
                CommandParser commandParser = new CommandParser(message.getText());
                if(commandParser.getWord().equals("acc"))
                    if(vkAccount.isMine(commandParser.getWord()))
                        if(commandParser.getWord().toLowerCase().equals("findchat")) {
                            String query = commandParser.getText();
                            ArrayList<Message> results = new ArrayList<>();

                            int count = 200;
                            int offset = 0;

                            while(offset < 5000){
                                ArrayList<Message> dialogs = vkAccount.getDialogs(offset, count);
                                for (Message m:dialogs)
                                    if(m.chat_id != null && m.title.contains(query))
                                        results.add(m);
                                if(dialogs.size() == 0)
                                    break;
                                if(results.size() > 30)
                                    break;
                                offset += count;
                                if(offset%1000==0)
                                    message.sendAnswer(new Answer("Поиск продолжается, просмотрено уже " + offset + " диалогов. Найдено " + results.size() + " диалогов."));
                            }

                            String result = "Список найденных диалогов: \n";
                            for (Message m:results)
                                if(m.chat_id != null)
                                    result += m.chat_id + " : " + m.title + " ("+m.chat_members.size()+"уч.) \n";
                            return result;
                        }
                return "";
            }

            @Override
            public ArrayList<CommandDesc> getHelp() {
                ArrayList<CommandDesc> result = new ArrayList<>();
                result.add(new CommandDesc("Выполнить поиск по беседам аккаунта "+vkAccount,
                        "Эта команда позволит найди беседы по названию.\n" +
                                "Команда просматривает не более чем 5000 последних бесед и выдает " +
                                "не более 30 результатов поиска.",
                        "botcmd acc " + vkAccount.getId()+ " FindChat <запрос для поиска>"));
                return result;
            }
        }
    }
    class BotsChatsFilter extends CommandModule {
        private boolean exitFromBotsChatsEnabled = true;
        private Timer exitFromBotsChatsTimer = null;
        private long exitFromBotsChatsCounter = 0;

        public BotsChatsFilter(ApplicationManager applicationManager) {
            super(applicationManager);
            exitFromBotsChatsEnabled = vkAccount.getFileStorage().getBoolean("exitFromBotsChatsEnabled", exitFromBotsChatsEnabled);
            exitFromBotsChatsCounter = vkAccount.getFileStorage().getLong("exitFromBotsChatsCounter", exitFromBotsChatsCounter);

            childCommands.add(new Status(applicationManager));
            childCommands.add(new ExitFromBotsChats(applicationManager));
        }
        public void start(){
            if(exitFromBotsChatsTimer == null){
                log(". Запуск модуля выхода из бесед с другими ботами для аккаунта " + vkAccount + "...");
                int period = applicationManager.getParameters().get(
                        "exitFromBotsChatsPeriod",
                        60,
                        "Период проверки на чаты с ботами",
                        "Период в секундах между проверками на наличие других наших ботов в беседах.");
                exitFromBotsChatsTimer = new Timer();
                exitFromBotsChatsTimer.schedule(new TimerTask() {
                    @Override
                    public void run() {
                        excludeFromBotsDialogs();
                    }
                }, 5000, period*1000);

            }
        }
        public void stop(){
            if(exitFromBotsChatsTimer != null){
                log(". Остановка модуля выхода из бесед с другими ботами для аккаунта " + vkAccount + "...");
                exitFromBotsChatsTimer.cancel();
                exitFromBotsChatsTimer = null;
            }
        }

        private void excludeFromBotsDialogs(){
            try {
                ArrayList<Message> dialogs = vkAccount.getDialogs100();
                for(Message message : dialogs){
                    if(message.chat_members != null && !excludedDialogs.contains(message.chat_id)) {
                        ArrayList<Long> members = message.chat_members;
                        for (Long user : members) {
                            if (user != null && user != vkAccount.getId()
                                    && applicationManager.getCommunicator().containsAccount(user)) {
                                log(". (" + vkAccount + ") EXCLUDE FROM CHAT" + message.chat_id);
                                vkAccount.sendMessage(null, message.chat_id,
                                        new Answer("В беседе может быть только один бот."));
                                excludedDialogs.add(message.chat_id);
                                vkAccount.exitFromChat(message.chat_id);
                                addExitFromBotsChatsCounter();
                                break;
                            }
                        }
                    }
                }
            }
            catch (Throwable e){
                e.printStackTrace();
                log("! Ошибка выхода из бот-диалогов: " + e.toString());
            }
        }
        private void setExitFromBotsChatsEnabled(boolean exitFromBotsChatsEnabled){
            this.exitFromBotsChatsEnabled = exitFromBotsChatsEnabled;
            vkAccount.getFileStorage().put("exitFromBotsChatsEnabled", exitFromBotsChatsEnabled).commit();
        }
        private void addExitFromBotsChatsCounter(){
            exitFromBotsChatsCounter++;
            vkAccount.getFileStorage().put("exitFromBotsChatsCounter", exitFromBotsChatsCounter).commit();
        }

        class Status extends CommandModule{
            public Status(ApplicationManager applicationManager) {
                super(applicationManager);
            }
            public @Override String processCommand(com.fsoft.vktest.AnswerInfrastructure.Message message) {
                if(message.getText().equals("status") || message.getText().equals("acc status")) {
                    return
                            "Аккаунт " + vkAccount + " выходить из бесед где много ботов: " + (exitFromBotsChatsEnabled?"включено":"выключено") + "\n" +
                            "Аккаунт " + vkAccount + ": по причине других ботов было покинуто " + exitFromBotsChatsCounter + " бесед\n";
                }
                return "";
            }
            public @Override ArrayList<CommandDesc> getHelp() {
                return new ArrayList<>();
            }
        }
        class ExitFromBotsChats extends CommandModule{
            public ExitFromBotsChats(ApplicationManager applicationManager) {
                super(applicationManager);
            }

            @Override
            public String processCommand(com.fsoft.vktest.AnswerInfrastructure.Message message) {
                CommandParser commandParser = new CommandParser(message.getText());
                if(commandParser.getWord().equals("acc"))
                    if(vkAccount.isMine(commandParser.getWord()))
                        if(commandParser.getWord().toLowerCase().equals("exitfrombotchats")) {
                            setExitFromBotsChatsEnabled(commandParser.getBoolean());
                            if(exitFromBotsChatsEnabled)
                                return "Теперь " + vkAccount + " будет выходить из чатов, в которых будут другие твои боты";
                            else
                                return "Больше " + vkAccount + " не будет выходить из чатов, в которых будут другие твои боты";
                        }
                return "";
            }

            @Override
            public ArrayList<CommandDesc> getHelp() {
                ArrayList<CommandDesc> result = new ArrayList<>();
                result.add(new CommandDesc("Включить или выключить выход из бесед с другими ботами для аккаунта "+vkAccount,
                        "Иногда так получается, что в одну беседу добавляют несколько твоих ботов одновременно.\n" +
                                "Поскольку держать в одной беседе несколько ботов с одинаковой базой достаточно бессмысленно," +
                                "боты могут автоматически выходить из таких бесед.\n" +
                                "Эта функция будет обнаруживать в беседе ботов, которые работают на этом же телефоне. " +
                                "Если у тебя на телефоне работает только один бот, использовать эту функцию смысла нет.",
                        "botcmd acc " + vkAccount.getId() + " exitfrombotchats <on/off>"));
                return result;
            }
        }
    }
    class Instructor extends CommandModule {
        private ArrayList<Long> instructed = new ArrayList<>();
        private boolean instructionEnabled = false;
        private String instructionText =
                "Если ты хочешь со мной поговорить, начни своё сообщение с текста \"Бот, \", и тогда я тебе отвечу.";

        public Instructor(ApplicationManager applicationManager) {
            super(applicationManager);

            instructionEnabled = vkAccount.getFileStorage().getBoolean("instructionEnabled", instructionEnabled);
            instructionText = vkAccount.getFileStorage().getString("instructionText", instructionText);

            childCommands.add(new Status(applicationManager));
            childCommands.add(new InstructionEnabled(applicationManager));
            childCommands.add(new SetInstruction(applicationManager));
        }
        public String getInstructionText() {
            return instructionText;
        }
        public boolean needInstruction(Long id){
            if(!instructionEnabled)
                return false;
            if(instructed.contains(id))
                return false;
            instructed.add(id);
            while(instructed.size() > 5000)
                instructed.remove(0);
            return true;
        }

        private void setInstructionText(String instructionText) {
            this.instructionText = instructionText;
            vkAccount.getFileStorage().put("instructionText", instructionText).commit();
        }
        private void setInstructionEnabled(boolean instructionEnabled) {
            this.instructionEnabled = instructionEnabled;
            vkAccount.getFileStorage().put("instructionEnabled", instructionEnabled).commit();
        }
        public boolean isInstructionEnabled() {
            return instructionEnabled;
        }

        class Status extends CommandModule{
            public Status(ApplicationManager applicationManager) {
                super(applicationManager);
            }
            public @Override String processCommand(com.fsoft.vktest.AnswerInfrastructure.Message message) {
                if(message.getText().equals("status") || message.getText().equals("acc status")) {
                    return
                            "Аккаунт " + vkAccount + " отвечать инструкцией: "+ (instructionEnabled?"да":"нет") + "\n" +
                            "Аккаунт " + vkAccount + " текст инструкции: "+instructionText + "\n" +
                            "Аккаунт " + vkAccount + " инструкцию получили " + instructed.size() + " пользователей\n";
                }
                return "";
            }
            public @Override ArrayList<CommandDesc> getHelp() {
                return new ArrayList<>();
            }
        }
        class InstructionEnabled extends CommandModule{
            public InstructionEnabled(ApplicationManager applicationManager) {
                super(applicationManager);
            }

            @Override
            public String processCommand(com.fsoft.vktest.AnswerInfrastructure.Message message) {
                CommandParser commandParser = new CommandParser(message.getText());
                if(commandParser.getWord().equals("acc")) {
                    if (vkAccount.isMine(commandParser.getWord())) {
                        if (commandParser.getWord().toLowerCase().equals("instructionenabled")) {
                            setInstructionEnabled(commandParser.getBoolean());
                            if(instructionEnabled)
                                return "(" + vkAccount + ") Инструкция включена. " +
                                        "Теперь когда боту будут писать без обращения, он один раз ответит текстом инструкции." ;
                            else
                                return "(" + vkAccount + ") Инструкция выключена. " +
                                        "Когда боту будут писать без обращения, он будет игнорировать." ;
                        }
                    }
                }
                return "";
            }

            @Override
            public ArrayList<CommandDesc> getHelp() {
                ArrayList<CommandDesc> result = new ArrayList<>();
                result.add(new CommandDesc("Включить или выключить ответ инструкцией для аккаунта "+vkAccount,
                        "Не все знают, что боту надо писать с обращением. Поэтому, бот может " +
                                "присылать инструкцию для тех, кто пишет ему без обращеня.\n" +
                                "Каждый человек будет получать эту инструкцию только один раз, " +
                                "если напишет боту без обращения.",
                        "botcmd acc " + vkAccount.getId() + " instructionenabled <on/off>"));
                return result;
            }
        }
        class SetInstruction extends CommandModule{
            public SetInstruction(ApplicationManager applicationManager) {
                super(applicationManager);
            }

            @Override
            public String processCommand(com.fsoft.vktest.AnswerInfrastructure.Message message) {
                CommandParser commandParser = new CommandParser(message.getText());
                if(commandParser.getWord().equals("acc"))
                    if(vkAccount.isMine(commandParser.getWord()))
                        if(commandParser.getWord().equals("setinstruction")) {
                            setInstructionText(commandParser.getText());
                            if(instructionEnabled)
                                return "Теперь на аккаунте " + vkAccount + " такая инструкция:\n" + instructionText;
                            else
                                return "Несмотря на то, что на аккаунте " + vkAccount + " ответ инструкцией отключён," +
                                        "инструкция была сохранена:\n" + instructionText +
                                        "\n Если ты хочешь, чтобы "+vkAccount+" её использовал, включи ответ инструкцией.";
                        }
                return "";
            }

            @Override
            public ArrayList<CommandDesc> getHelp() {
                ArrayList<CommandDesc> result = new ArrayList<>();
                result.add(new CommandDesc("Изменить инструкцию для аккаунта "+vkAccount,
                        "Этой командой ты можешь поменять текст инструкции.\n" +
                                "Не все знают, что боту надо писать с обращением. Поэтому, бот может " +
                                "присылать инструкцию для тех, кто пишет ему без обращеня.\n" +
                                "Каждый человек будет получать эту инструкцию только один раз, " +
                                "если напишет боту без обращения.",
                        "botcmd acc " + vkAccount.getId() + " setinstruction <новый текст инструкции>"));
                return result;
            }
        }
    }
    class Guard extends CommandModule{
        private Timer messagesGuardTimer = null;//охранник отслеживает время чтения сообщений и обраруживая зависание, перезапускает личку.
        private long lastTimeReadMessage = 0;

        public Guard(ApplicationManager applicationManager) {
            super(applicationManager);
            childCommands.add(new Status(applicationManager));
        }

        public void runGuard(){
            if(messagesGuardTimer != null)
                return;
            messagesGuardTimer = new Timer("Message guard for " + vkAccount);
            log(". Запуск охранника для аккаунта " + vkAccount + " с интервалом 5 минут");
            messagesGuardTimer.schedule(new TimerTask() {
                @Override
                public void run() {
                    try {
                        long sinceLastRead = System.currentTimeMillis() - lastTimeReadMessage;
                        //если личка не читается уже больше 5ти минут
                        //log("GUARD sinceLastRead = " + sinceLastRead);
                        //log("GUARD messageProcessingEnabled = " + messageProcessingEnabled);
                        //log("GUARD isActive = " + isActive());
                        if (sinceLastRead > (5 * 60 * 1000) && messageProcessingEnabled && vkAccount.isRunning()) {//5 минут
                            //// TODO: 08.12.2017  applicationManager.messageBox(log("GUARD (" + vkAccount + ") Обнаружены проблемы в работе ЛС. Перезапуск личных сообщений..."));
                            stopModule();
                            F.sleep(5000);
                            startModule();
                        }
                    }
                    catch (Exception e){
                        e.printStackTrace();
                        //// TODO: 08.12.2017  applicationManager.messageBox(log("GUARD (" + vkAccount + ") Охраннику не удаётся перезапустить ЛС!!!"));
                    }
                }
            }, 180000, 180000);//3 минуты
        }
        public void stopGuard(){
            if(messagesGuardTimer != null) {
                messagesGuardTimer.cancel();
                messagesGuardTimer = null;
            }
        }
        public void registerRead(){
            //вызывать эту команду каждый раз после того как прочитали личку успешно
            lastTimeReadMessage = System.currentTimeMillis();
        }

        class Status extends CommandModule{
            public Status(ApplicationManager applicationManager) {
                super(applicationManager);
            }
            public @Override String processCommand(com.fsoft.vktest.AnswerInfrastructure.Message message) {
                if(message.getText().equals("status") || message.getText().equals("acc status")) {
                    long now = System.currentTimeMillis();
                    return
                            "Аккаунт " + vkAccount + " последний раз успешно прочитал сообщение " + F.getTimeText(now-lastTimeReadMessage) + " назад\n";
                }
                return "";
            }
            public @Override ArrayList<CommandDesc> getHelp() {
                return new ArrayList<>();
            }
        }
    }
}
