package com.fsoft.vktest;

import android.app.Service;
import android.content.Context;
import android.net.wifi.WifiManager;
import android.os.Environment;
import android.os.Handler;
import android.os.PowerManager;

import com.fsoft.vktest.AnswerInfrastructure.BotBrain;
import com.fsoft.vktest.AnswerInfrastructure.Message;
import com.fsoft.vktest.Modules.Commands.ClearCache;
import com.fsoft.vktest.Modules.Commands.Command;
import com.fsoft.vktest.Modules.Commands.CpuTemp;
import com.fsoft.vktest.Modules.Commands.Decode;
import com.fsoft.vktest.Modules.Commands.Encode;
import com.fsoft.vktest.Modules.Commands.SetBotMark;
import com.fsoft.vktest.Modules.Commands.Version;
import com.fsoft.vktest.Communication.VkCommunicator;
import com.fsoft.vktest.Modules.Autoreboot;
import com.fsoft.vktest.Modules.CommandModule;
import com.fsoft.vktest.Modules.DatabaseBackuper;
import com.fsoft.vktest.Modules.FileManager;
import com.fsoft.vktest.Modules.HttpServer;
import com.fsoft.vktest.Modules.SecurityProvider;
import com.fsoft.vktest.Utils.CommandParser;
import com.fsoft.vktest.Utils.F;
import com.fsoft.vktest.Utils.FileStorage;
import com.fsoft.vktest.Utils.Parameters;
import com.fsoft.vktest.Utils.SimpleEntry;
import com.fsoft.vktest.Utils.UserList;
import com.fsoft.vktest.ViewsLayer.MainActivity;
import com.perm.kate.api.User;

import java.io.*;
import java.util.*;

/**
 * manage components
 * Created by Dr. Failov on 05.08.2014.
 */
/**
 * Спустя два года самое время всё нахуй переписать.
 *
 * Менеджер занимается хранением связи между модулями, является контейнером для команд общего назначения (обслуживание бота,
 * файловой системы, учёт времени, работа с бэкамами, например)
 *
 * Какой модуль чем занимается:
 *
 * Service. Менеджер должен создаваться исключительно из сервиса. Ссылка на сервис нужна для контекста.
 *
 * Activity обслуживает все окна. Экраны с командами, экраны с аккаунтами, логом, сообщениями,
 * Активити получает доступ к сервису при помощи статической функции GetInstance,
 * а далее обращается к ответственным модулям.
 * \\\ что делать с командами работы с элементами на экране (на активити, которое не всегда есть)
 * Когда модулю нужно отобразить на экране сообщение, он обращается к активити.
 *
 * BotBrain занимается подбором ответов. Он является контейнером для функций, для базы. Там содержатся и синонимы, и базы,
 * и все(!) модули которые отвечают НЕ на команды.
 * Текст переходит между модулями в его изначальном виде, т.е. с обращением.
 * Этот же модуль отвечает за отображение сообщений на активити.
 * Этот модуль содержит функции проверки наличия обращения, убирания обращения из Message
 *
 * VkCommunicator занимается работой с сетью. С аккаунтами. Именно этот модуль хранит аккаунты, запускает их. Аккаунты
 * инициируют вызовы других модулей.
 * /// как должны идти сообщения написанные боту изнутри программы?
 *
 *
 *
 *
 * Created by Dr. Failov on 14.08.2018.
 */
public class ApplicationManager {
    //-------------- НЕ МЕНЯТЬ!!! Иначе надо будет переписывать хэш-суммы!!!!---------------------------------------
    static public String getVisibleName(){
        //обязательно должно содержать "Dr.Failov iHA bot"
        return "Dr.Failov iHA bot™ v5.0 beta 0";
    }
    static public String getShortName(){
        return "FP iHA bot";
    }
    static public String getGroupLink(){
        return "http://vk.com/ihabotclub";
    }
    static public String getFullVersionLink(){
        return "market://details?id=com.fsoft.ihabotdonate";
    }
    static public String getDevelopersList(){
        return "Dr. Failov - Главный разработчик. Сделал почти всё сам и с нуля.\n" +
                "CyberTailor - во многом помог, в частности, разработал визуальный облик символов для модуля \"напиши\".\n" +
                "Shmeile - Тестировка, полезные советы, помощь в организации сообщества, обучение, помощь в чистке базы.\n" +
                "FaNtA_DDD - Тестировка, полезные советы, помог решить проблему с выключенным экраном.\n" +
                "melnik_arthur - Тестировка, полезные советы, обучение бота, помог очистить базу.\n" +
                "koshatikvk - Учитель.\n" +
                "И еще несколько:)";
    }
    //-------------- НЕ МЕНЯТЬ!!! Иначе надо будет переписывать хэш-суммы!!!!---------------------------------------
    private static ApplicationManager applicationManager = null;

    private BotService service = null;//это в общем то наш сервис. Он должен быть по любому
    private MainActivity activity = null;
    private VkCommunicator vkCommunicator;
    private BotBrain brain;
    private Parameters parameters;

    private ArrayList<CommandModule> commands;
    private SecurityProvider securityProvider = null;
    private WifiManager.WifiLock wifiLock = null;
    private PowerManager.WakeLock wakeLock = null;
    private DatabaseBackuper databaseBackuper = null;
    private HttpServer httpServer = null;
    private UserList ignorUsersList = null;
    private UserList allowUsersList = null;


    private long startedTime = 0; //нужен для учёта времени аптайма
    private boolean standby = false;

    public ApplicationManager(BotService service){
        applicationManager = this;
        this.service = service;
        startedTime = System.currentTimeMillis();
        parameters = new Parameters(this);
        CommandParser.applicationManager = this;

        ignorUsersList = new UserList("ignor",
                "Список игнорируемых пользователей",
                "Список пользователей, которым бот не будет отвечать на сообщения.\n" +
                        "В игнор пользователя можно добавить вручную, также нарушители правил бота попадают в игнор автоматически.",
                //// TODO: 27.09.2017 дополнить комментариями когда будет понятно что к чему
                this);
        ignorUsersList = new UserList("allow",
                "Список доверенных пользователей",
                "Список пользователей, которые имеют право давать боту команды.\n" +
                        "Команды позволяют настраивать бота, редактировать базы, получать служебную информацию...\n" +
                        "Список всех команд можно увидеть по команде botcmd help, или на экране \"Команды\".\n" +
                        "Команды боту можно писать там же, где обычные сообщения.\n" +
                        "Все команды начинаются со слова botcmd.\n" +
                        "Для всех остальных пользователей (не доверенных) при попытке отправить боту команду будет выдана ошибка.",
                this);
        vkCommunicator = new VkCommunicator(this);
        brain = new BotBrain(this);
        databaseBackuper = new DatabaseBackuper(this);
        securityProvider = new SecurityProvider(this);
        httpServer = new HttpServer(this);
        commands = new ArrayList<>();
        commands.add(new SetBotMark(this));
        commands.add(new Status(this));
        commands.add(new Version(this));
        commands.add(new Encode(this));
        commands.add(new Decode(this));
        commands.add(new CpuTemp(this));
        commands.add(new ClearCache(this));
        commands.add(new FileManager(this));
        commands.add(new Autoreboot(this));
        commands.add(ignorUsersList);
        commands.add(allowUsersList);
        commands.add(vkCommunicator);
        commands.add(parameters);
        commands.add(activity);
        commands.add(brain);
        commands.add(databaseBackuper);
        commands.add(securityProvider);
        commands.add(httpServer);
    }
    public String getHomeFolder(){
        return Environment.getExternalStorageDirectory() + File.separator + programName;
    }
    public Context getContext(){
        return service;
    }
    public String log(String text){
        if(activity.consoleView != null)
            activity.consoleView.log(" " + text);
        return text;
    }
    public boolean isDonated(){
        return securityProvider.isDonated();
    }
    public void messageBox(String text){
        if(activity != null)
            activity.messageBox(text);
    }
    public MainActivity getActivity() {
        return activity;
    }
    public void setActivity(MainActivity activity) {
        this.activity = activity;
    }
    public VkCommunicator getCommunicator() {
        return vkCommunicator;
    }
    public BotBrain getBrain() {
        return brain;
    }
    public Parameters getParameters() {
        return parameters;
    }
    public UserList getIgnorUsersList() {
        return ignorUsersList;
    }
    public UserList getAllowUsersList() {
        return allowUsersList;
    }
    public boolean isRunning(){
        //если какой-то из модулей работает по таймеру, то эта проверка позволит ему понять что программа закрыта
        return service != null;
    }
    public void stop(){
        applicationManager = null;
        service = null;
        for(CommandModule commandModule:commands)
            commandModule.stop();
    }
    //================================= ДАЛЬШЕ ИДЁТ СТАРОЕ =============================================================


    //--------------------------- STATIC DATA --------------------------
    static public String programName = "DrFailov_VK_iHA_bot";
    static public String botcmd = "botcmd,bcd";
    static public String constAutoRun = "autorun";

    //--------------------------- CONTEXT DATA --------------------------
    public Handler handler = new Handler();
    private String botMark = "bot";
    public boolean running = true; // снимать только при закрытии программы

    public boolean loaded = false;//обозначается как true когда функция load была успешно выполнена
    public boolean dontsave = false;

    //--------------------------- PUBLIC FUNCTIONS --------------------------

    public void load(){
        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    F.sleep(500);
                    log(". Загрузка программы " + botName + " ...");
                    if(!securityProvider.allowStart()) {
                        //log("! Запуск программы запрещён.");
                        return;
                    }
                    startAutoSaving();
                    securityProvider.addDonationStateChangedListener(new SecurityProvider.OnDonationStateChangedListener() {
                        @Override
                        public void donationStateChanged(Boolean donated) {
                            processNewDonationState(donated);
                        }
                    });
                    messageComparer.load();
                    brain.load();
                    httpServer.load();
                    vkAccounts.load();
                    vkCommunicator.load();
                    databaseBackuper.load();
                    securityProvider.load();
                    FileStorage fileStorage = new FileStorage("application_manager_data");
                    botName = fileStorage.getString("botName", botName);
                    //SharedPreferences sp = activity.getPreferences(Activity.MODE_PRIVATE);
                    //botName = sp.getString("botName", botName);
                    log(". Имя бота " + botName + " загружено.\n");
                    log(". Программа " + botName + " загружена.");
                    loaded = true;
                    activity.showToast("Загрузка стен...");
                }
                catch (Exception e){
                    e.printStackTrace();
                    log("! Ошибка загрузки: " + e.toString());
                }
            }
        }).start();
    }
    private void save(){
        //сохранять по факту изменения
        FileStorage fileStorage = new FileStorage("application_manager_data");
        //fileStorage.putString("botName", botName);
        fileStorage.putString("botMark", botMark);
        fileStorage.commit();
        log(". Имя бота (" + botMark + ") сохранено.\n");
    }
    public void close(){
        try {
            running = false;
            stopAutoSaving();
            stopWiFiLock();
            F.sleep(500);
            if(loaded)
                processCommands("save", getUserID());
            else
                log("Программа не была полностью загружена.");
            F.sleep(500);
            vkAccounts.close();
            vkCommunicator.close();
            brain.close();
            messageComparer.close();
            databaseBackuper.close();
            securityProvider.close();
            F.sleep(500);
            httpServer.close();
        }
        catch (Exception e){
            e.printStackTrace();
            log("! Ошибка завершения: " + e.toString());
        }
    }
    public boolean isStandby(){
        return vkCommunicator.standby;
    }
    public String processMessage(Message message){
        try {
            //обработать
            return brain.processMessage(message);
        }
        catch (Exception e){
            e.printStackTrace();
            return Parameters.get("error_message", "Глобальная ошибка обработки сообщения: ERROR", "Текст который увидит пользователь если возникнет ошибка обработки сообщения").replace("ERROR", e.toString());
        }
        catch (OutOfMemoryError e){
            e.printStackTrace();
            return Parameters.get("error_memory_message", "Глобальная нехватка памяти: ERROR", "Текст который увидит пользователь если возникнет нехватка памяти при обработке сообщения").replace("ERROR", e.toString());
        }
    }
    public String processCommands(String text, Long senderId){
        try {
            String result = "";
            String[] commandsToExecute = text.split("\n");
            for (int i = 0; i < commandsToExecute.length; i++) {
                if(i > 0)
                    result += "---------------------\n";
                result += processCommand(commandsToExecute[i], senderId);
            }
            return result;
        }
        catch (Throwable e){
            e.printStackTrace();
            return "Глобальная ошибка обработки пакета команды: " + e.toString();
        }
    }
    public String processCommand(String command){
        return processCommand(command, getUserID());
    }
    public String getOnlyCommandsList(){
        String result = "";
        String[] lines = getCommandsHelp().split("\\n");
        for(String s: lines)
            if(s.contains("---|"))
                result += s.replace("---|", "").trim() + "\n";
        return  result;
    }
    public String getSuggestions(String command){
        log(". Подготовка списка подсказок...");
        String list = getCommandsHelp();
        String[] blocks = list.split("\n\n");

        ArrayList<Map.Entry<String, Float>> chart = new ArrayList<>();
        log(". Сравнение команд...");
        command = command.toLowerCase();
        for (String block:blocks){
            String[] lines = block.split("\n");
            String desc = "";
            String comm = "";
            for (String line:lines){
                if(line.contains("[ "))
                    desc += line.replace("[ ", "").replace(" ]", "").toLowerCase() + "\n";
                if(line.contains("---|"))
                    comm += line.replace("---| ", "").replace("botcmd ", "") + "\n";
            }
            if(desc.contains(command))
                chart.add(new SimpleEntry<String, Float>(comm, 1.0f));
            else
                chart.add(new SimpleEntry<String, Float>(comm, messageComparer.compareMessages(comm, command)));
        }
        log(". Сортировка списка...");
        for (int i = 0; i < chart.size(); i++)
            for (int j = 1; j < chart.size(); j++)
                if(chart.get(j).getValue() > chart.get(j-1).getValue())
                    Collections.swap(chart, j, j-1);
        log(". Оформление результата...");
        String result = "";
        for (int i = 0; i < Math.min(chart.size(), 10); i++)
            result += "- " + chart.get(i).getKey();
        return  result;
    }
    public String getSuggestionsOld(String command){
        log(". Подготовка списка подсказок...");
        String commands = getOnlyCommandsList();
        String[] lines = commands.split("\\n");
        ArrayList<Map.Entry<String, Float>> chart = new ArrayList<>();
        log(". Сравнение команд...");
        for (String line:lines){
            line = line.replace("botcmd ", "");
            float comparation = messageComparer.compareMessages(line, command);
            chart.add(new SimpleEntry<String, Float>(line, comparation));
        }
        log(". Сортировка списка...");
        for (int i = 0; i < chart.size(); i++)
            for (int j = 1; j < chart.size(); j++)
                if(chart.get(j).getValue() > chart.get(j-1).getValue())
                    Collections.swap(chart, j, j-1);
        log(". Оформление результата...");
        String result = "\n";
        for (int i = 0; i < Math.min(chart.size(), 7); i++)
            result += "- " + chart.get(i).getKey() + " \n";
        return  result;
    }
    public String getCommandsHelp(){
        try {
            String result = getVisibleName()+", разработанный Dr. Failov.\n" +
                    "Команды можно писать везде, где бот может отвечать, например, на стене, в настройках \"Написать сообщение боту\" , или в ЛС, если их обработка включена.\n\n" +
                    "[ Сохранить все внесенные в базы изменения ]\n" +
                    "---| botcmd save\n\n" +
                    "[ Получить подробный отчёт о состоянии программы ]\n" +
                    "---| botcmd status\n\n";
            result += Parameters.getHelp();
            for (int i = 0; i < commands.size(); i++) {
                result += commands.get(i).getHelp();
            }
            return result;
        }
        catch (Exception e){
            e.printStackTrace();
            return "! Глобальная ошибка получения справки: " + e.toString();
        }
    }
    public boolean isEmptyMark(String botMark){
        return botMark.equals("") || !botMark.toLowerCase().trim().equals("empty");
    }
    public Message addMarkAndTreatment(Message message){
        if(message.answer != null) {
            boolean needToAddMark = !isEmptyMark(botMark);
            boolean donated = isDonated();
            User user = message.getBotAccount().getUserAccount(message.getAuthor());
            String nameOfUser = user.first_name;

            if(!needToAddMark && !donated)
                needToAddMark = true;

            if(needToAddMark){
                message.answer.text = "("+botMark+") " + nameOfUser + ", " + message.answer.text;
            }
        }
        return message;
    }



    public void setBotMark(String botMark) {
        this.botMark = botMark;
        save();
    }
    public String getBotMark() {
        return botMark;
    }

    //--------------------------- PRIVATE FUNCTIONS --------------------------
    private void startAutoSaving(){
        log(". Запуск автосохранения...");
        if(autoSaveTimer == null){
            autoSaveTimer = new Timer();
            autoSaveTimer.schedule(new TimerTask() {
                @Override
                public void run() {
                    log(". Автосохранение...");
                    processCommands("save", getUserID());
                }
            }, 1800000, 1800000);
        }
    }
    private void stopAutoSaving(){
        log(". Остановка автосохранения...");
        if(autoSaveTimer != null){
            autoSaveTimer.cancel();
            autoSaveTimer= null;
        }
    }
    private void startWiFiLock(){
        try {
            if(wifiLock == null) {
                log(". Блокировка состояния Wi-Fi...");
                WifiManager wifiManager = (WifiManager) activity.getSystemService(Context.WIFI_SERVICE);
                wifiLock = wifiManager.createWifiLock(programName);
                wifiLock.acquire();
            }

            if(wakeLock == null) {
                log(". Блокировка состояния CPU...");
                PowerManager pm = (PowerManager) activity.getSystemService(Context.POWER_SERVICE);
                wakeLock = pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "My Tag");
                wakeLock.acquire();
            }
        }
        catch (Exception e){
            e.printStackTrace();
            log("Ошибка блокировки Wi-Fi: " + e.toString());
        }
    }
    private void stopWiFiLock(){
        try {
            if(wifiLock != null) {
                log(". Разблокировка Wi-Fi...");
                wifiLock.release();
                wifiLock = null;
            }
            if(wakeLock != null) {
                log(". Разблокировка CPU...");
                wakeLock.release();
                wakeLock = null;
            }
        }
        catch (Exception e){
            e.printStackTrace();
            log("Ошибка разблокировки Wi-Fi: " + e.toString());
        }
    }
    private String processCommand(String text, Long senderId){
        try {
            if(text.equals("save")) {
                while(dontsave) {
                    log(". Waiting while saving allow...");
                    F.sleep(1000);
                }
                saving = true;
            }
            String result = "";
            result += Parameters.process(text, senderId);

            for (int i = 0; i < commands.size(); i++) {
                result += commands.get(i).processCommand(text, senderId);
            }
            if (result.equals(""))
                result = "Такой команды нет. Возможные варианты:\n"+getSuggestions(text);
            return result;
        }
        catch (Throwable e){
            e.printStackTrace();
            return "Глобальная ошибка обработки команды: " + e.toString();
        }
        finally {
            if(text.equals("save"))
                saving = false;
        }
    }
    private void processNewDonationState(boolean donationState){
        if(donationState){//донатка валидна
            startWiFiLock();
        }
        else { //донатка не валидна
            stopWiFiLock();
        }
    }

    //--------------------------- INNER CLASSES --------------------------
    private class Status extends CommandModule{
        public Status(ApplicationManager applicationManager) {
            super(applicationManager);
        }

        @Override
        public ArrayList<CommandDesc> getHelp() {
            ArrayList<CommandDesc> result= new ArrayList<>();
            result.add(new CommandDesc(
                    "Получить отчёт о состоянии",
                    "Эта команда собирает со всех модулей программы отчёты о их состоянии " +
                            "и выводит полный перечень всех параметров бота.",
                    "botcmd status"
            ));
            return result;
        }

        @Override
        public String processCommand(Message message) {
            CommandParser commandParser = new CommandParser(message.getText());
            if(commandParser.getWord().equals("status")) {
                return "Метка бота: " + applicationManager.getBotMark() + "\n"+
                        "Использование оперативной памяти: "+applicationManager.getRamUsagePercent()+" % \n";
            }
            return "";
        }
    }

    private class Standby implements Command{
        @Override public
        String process(String input, Long senderId) {
            CommandParser commandParser = new CommandParser(input);
            if(commandParser.getWord().equals("standby")) {
                setStandby(commandParser.getBoolean());
                return "standby = " + standby;
            }
            return "";
        }

        @Override public
        String getHelp() {
            return "[ Включить\\выключить режим ожидания ]\n" +
                    "---| botcmd standby <on/off>\n\n";
        }
    }

    private class SetUpTime implements Command{
        @Override
        public String process(String input, Long senderId) {
            CommandParser commandParser = new CommandParser(input);
            if(commandParser.getWord().equals("setuptime")){
                long now = System.currentTimeMillis();
                startupTime = now - commandParser.getLong();
                return "Текущее время работы: " + getWorkingTime() + "\n";
            }
            return "";
        }

        @Override
        public String getHelp() {
            return "";
        }
    }

}