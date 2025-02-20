package com.fsoft.vktest.Communication.Account.VK;

import com.fsoft.vktest.AnswerInfrastructure.Message;
import com.fsoft.vktest.ApplicationManager;
import com.fsoft.vktest.Modules.CommandModule;
import com.fsoft.vktest.Utils.CommandParser;
import com.fsoft.vktest.Utils.Parameters;
import com.fsoft.vktest.NewViewsLayer.MainActivity;
import com.fsoft.vktest.Modules.Commands.CommandDesc;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Timer;
import java.util.TimerTask;

/**
 * Класс, который занимается трансляцией статуса на аккаунте
 * Created by Dr. Failov on 16.02.2017.
 */
public class StatusBroadcaster extends CommandModule {
    //требуется для обращения к серверным методам
    VkAccount vkAccount = null;
    //требуется для того, чтобы при отключении функции восстановить
    //тот статус, который был при запуске трансляции.
    private String oldStatusText = "";
    //опиция позволяющая пользователю включать или отключать трансляцию
    private boolean statusBroadcastingEnabled = false;
    //если таймер null - значит трансляция сейчас не работает
    private Timer statusBroadcastingTimer = null;
    //что транслировать, собственно
    private String statusBroadcastingText = "NAME | TIME | UpTime: WORKING | Принято сбщ: RECEIVED | Отправлено: PROCESSED | АРІ: API";


    public StatusBroadcaster(ApplicationManager applicationManager, VkAccount vkAccount) {
        super(applicationManager);
        this.vkAccount = vkAccount;
        try {
            statusBroadcastingEnabled = vkAccount.getFileStorage().getBoolean("statusBroadcastingEnabled", statusBroadcastingEnabled);
            statusBroadcastingText = vkAccount.getFileStorage().getString("statusBroadcastingText", statusBroadcastingText);

            childCommands.add(new StatusBroadcasting(applicationManager));
            childCommands.add(new SetStatusText(applicationManager));
            childCommands.add(new GetStatusText(applicationManager));
        }
        catch (Exception e){
            log("! Ошибка создания объекта StatusBroadcaster: " + e.toString());
            e.printStackTrace();
        }
    }
    @Override public void stop() {
        super.stop();
        if(statusBroadcastingTimer != null)
            stopModule();
    }
    public void setStatusBroadcastingEnabled(boolean statusBroadcasting){
        this.statusBroadcastingEnabled = statusBroadcasting;
        vkAccount.getFileStorage().put("statusBroadcastingEnabled", statusBroadcastingEnabled).commit();
        log(". Трансляция статуса для аккаунта " + vkAccount + ": " + (statusBroadcastingEnabled?"Включена":"Выключена"));
        if(statusBroadcastingEnabled && statusBroadcastingTimer == null)
            startModule();
        else if(!statusBroadcastingEnabled && statusBroadcastingTimer != null)
            stopModule();
    }
    public void setStatusBroadcastingText(String statusBroadcastingText) {
        this.statusBroadcastingText = statusBroadcastingText;
        vkAccount.getFileStorage().put("statusBroadcastingText", statusBroadcastingText).commit();
    }
    public void startModule(){
        if(statusBroadcastingTimer == null && statusBroadcastingEnabled) {
            statusBroadcastingTimer = new Timer("Status Broadcasting for " + vkAccount);
            saveStatus();
            int periodMin = applicationManager.getParameters().get("StatusBroadcastingPeriod",
                    3,
                    "Период обновления статуса аккаунта в минутах",
                    "Определяет как часто бот будет обновлять статус на странице.\n" +
                            "По умолчанию это 3 минуты. Слишком малое время может привести к капче!");
            if(periodMin == 0)
                periodMin = 1;
            int periodMs = periodMin * 60 * 1000;
            statusBroadcastingTimer.schedule(new TimerTask(){
                @Override
                public void run() {
                    broadcastStatus();
                }
            }, 10000, periodMs);
        }
    }
    public void stopModule(){
        if(statusBroadcastingTimer != null){
            statusBroadcastingTimer.cancel();
            statusBroadcastingTimer = null;
            restoreStatus();
        }
    }

    private void broadcastStatus(){
        if(!applicationManager.isRunning()){
            stopModule();
            return;
        }
        if(statusBroadcastingEnabled == false || statusBroadcastingTimer == null)
            return;
        Calendar calendar = Calendar.getInstance();
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm");
        try {
            vkAccount.setOnline();
            String newStatus = statusBroadcastingText;

            newStatus = newStatus.replace("TIME", sdf.format(calendar.getTime()));

            newStatus = newStatus.replace("NAME", ApplicationManager.getVisibleName());

            //// TODO: 13.07.2017 fix it
            //// TODO: 13.12.2017 обновить методы получения времени сколько работает бот
//            if(applicationManager.getCommunicator() != null)
//                newStatus = newStatus.replace("WORKING", applicationManager.getWorkingTime());

            //// TODO: 13.12.2017 обновить методы получения количества сообщений отправленных ботом
            //newStatus = newStatus.replace("PROCESSED", String.valueOf(MainActivity.messageList.getMessagesProcessed()));


            //// TODO: 13.12.2017 обновить методы получения количества сообщений принятых ботом
//            if(applicationManager.getCommunicator() != null)
//                newStatus = newStatus.replace("RECEIVED", String.valueOf(applicationManager.getCommunicator().getTotalReceivedMessagesCount()));

            //// TODO: 13.12.2017 обновить методы получения количества запросов
//            if(applicationManager.getCommunicator() != null)
//                newStatus = newStatus.replace("API", String.valueOf(applicationManager.getCommunicator().getTotalApiCount()));

            vkAccount.setStatus(newStatus);
        }
        catch (Throwable e){
            log("! Ошибка установки статуса на аккаунте "+vkAccount+": " + e.toString());
            e.printStackTrace();
        }
    }
    private void saveStatus(){
        new Thread(new Runnable() {
            @Override
            public void run() {
                oldStatusText = vkAccount.getStatus(vkAccount.getId());
            }
        }, "Saving Status Thread").start();
    }
    private void restoreStatus(){
        new Thread(new Runnable() {
            @Override
            public void run() {
                vkAccount.setStatus(oldStatusText);
            }
        }, "Restore Status Thread").start();
    }


    class Status extends CommandModule{
        public Status(ApplicationManager applicationManager) {
            super(applicationManager);
        }
        public @Override String processCommand(com.fsoft.vktest.AnswerInfrastructure.Message message) {
            if(message.getText().equals("status") || message.getText().equals("acc status")) {
                String result = "";
                result += "Аккаунт " + vkAccount + " обновление статуса: " + (statusBroadcastingEnabled?"ВКЛ":"ВЫКЛ") + "\n";
                if(statusBroadcastingEnabled)
                    result += "Аккаунт " + vkAccount + " текст статуса: " + statusBroadcastingText + "\n";
                return result;
            }
            return "";
        }
        public @Override ArrayList<CommandDesc> getHelp() {
            return new ArrayList<>();
        }
    }
    class StatusBroadcasting extends CommandModule {
        public StatusBroadcasting(ApplicationManager applicationManager) {
            super(applicationManager);
        }

        @Override
        public String processCommand(Message message) {
            CommandParser commandParser = new CommandParser(message.getText());
            if(commandParser.getWord().equals("acc"))
                if(vkAccount.isMine(commandParser.getWord()))
                    if(commandParser.getWord().equals("status")) {
                        boolean oldValue = statusBroadcastingEnabled;
                        boolean newValue = commandParser.getBoolean();
                        setStatusBroadcastingEnabled(newValue);
                        if(oldValue && newValue)
                            return "("+vkAccount+") Трансляция статуса уже была включена.\n" +
                                    "Если ты хотел её выключить, то надо писать \"false\", " +
                                    "\"off\" или \"disable\".\n" +
                                    "Если статус почему-то не обновляется, попробуй " +
                                    "перезапустить программу, перезапустить телефон.\n" +
                                    "Если не помогает, попробуй сменить текст статуса, " +
                                    "пересоздать аккаунт. Если это всё не помогает, почитай логи: " +
                                    "Возможно, там возникают какие-то ошибки во время обновления.\n" +
                                    "По тексту этих ошибок можно попробовать загуглить.";
                        if(!oldValue && !newValue)
                            return "("+vkAccount+") Трансляция статуса уже была выключена.\n" +
                                    "Если ты хотел её включить, то надо писать \"true\", " +
                                    "\"on\" или \"enable\".\n" +
                                    "Если ты выключил трансляцию, а она всё равно обновляется, " +
                                    "то проверь не запущен ли у тебя бот ещё где-то:)\n" +
                                    "Если нет, то наверное, есть какая-то ошибка в программе. " +
                                    "Просто перезапусти бота.";
                        if(oldValue && !newValue)
                            return "("+vkAccount+") Готово, ты остановил трансляцию статуса для "+vkAccount+".";
                        if(!oldValue && newValue)
                            return "("+vkAccount+") Готово, ты запустил трансляцию статуса для "+vkAccount+".";
                    }
            return "";
        }

        @Override
        public ArrayList<CommandDesc> getHelp() {
            ArrayList<CommandDesc> result = new ArrayList<>();
            result.add(new CommandDesc("Включить или выключить трансляцию статуса для аккаунта "+vkAccount,
                    "Бот может, пока работает, отображать в " +
                            "статусе аккаунта какой нибудь текст. Этот текст может " +
                            "содержать время, название и версию бота, время работы бота с момента " +
                            "перезагрузки, счетчик принятых сообщений, счетчик отправленных сообщений " +
                            "и количество обращений к серверу.",
                    "botcmd acc " + vkAccount.getId() + " status <on/off>"));
            return result;
        }
    }
    class GetStatusText extends CommandModule{
        public GetStatusText(ApplicationManager applicationManager) {
            super(applicationManager);
        }

        @Override
        public String processCommand(Message message) {
            CommandParser commandParser = new CommandParser(message.getText());
            if(commandParser.getWord().equals("acc"))
                if(vkAccount.isMine(commandParser.getWord()))
                    if(commandParser.getWord().equals("getstatustext")) {
                        return "Сейчас для трансляции статуса на аккаунте "+vkAccount+
                                " используется этот текст: " + statusBroadcastingText;
                    }
            return "";
        }

        @Override
        public ArrayList<CommandDesc> getHelp() {
            ArrayList<CommandDesc> result = new ArrayList<>();
            result.add(new CommandDesc("Получить текст статуса для аккаунта "+vkAccount,
                    "Эта команда покажет, какой статус будет транслировать бот, " +
                            "если включить трансляцию статуса.",
                    "botcmd acc " + vkAccount.getId() + " getstatustext"));
            return result;
        }
    }
    class SetStatusText extends CommandModule{
        public SetStatusText(ApplicationManager applicationManager) {
            super(applicationManager);
        }

        @Override
        public String processCommand(Message message) {
            CommandParser commandParser = new CommandParser(message.getText());
            if(commandParser.getWord().equals("acc"))
                if(vkAccount.isMine(commandParser.getWord()))
                    if(commandParser.getWord().equals("setstatustext")) {
                        setStatusBroadcastingText(commandParser.getText());
                        return "Теперь для трансляции статуса на аккаунте "+vkAccount+
                                " используется текст: " + statusBroadcastingText;
                    }
            return "";
        }

        @Override
        public ArrayList<CommandDesc> getHelp() {
            ArrayList<CommandDesc> result = new ArrayList<>();
            result.add(new CommandDesc("Изменить текст статуса для аккаунта "+vkAccount,
                    "Эта команда позволит тебе поменять текст статуса, который бот будет транслировать " +
                            "на аккаунте, если трансляция статуса включена.\n" +
                            "фрагмент TIME заменяется на время обновления статуса\n" +
                            "фрагмент NAME заменяется на название и версию бота\n" +
                            "фрагмент WORKING заменяется на время работы с момента перезагрузки\n" +
                            "фрагмент PROCESSED заменяется на количество обработанных сообщений\n" +
                            "фрагмент RECEIVED заменяется на общее количество принятых сообщений\n" +
                            "фрагмент API заменяется на количество обращений программы к API\n",
                    "botcmd acc " + vkAccount.getId()+ " setstatustext <текст статуса>"));
            return result;
        }
    }
}
