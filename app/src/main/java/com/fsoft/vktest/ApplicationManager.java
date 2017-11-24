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
 * Edited  by Dr. Failov on 23.11.2017.
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


    private long startedTime = 0; //нужен для учёта времени аптайма
    private boolean standby = false;

    public ApplicationManager(BotService service){
        applicationManager = this;
        this.service = service;
        startedTime = System.currentTimeMillis();
        parameters = new Parameters(this);
        CommandParser.applicationManager = this;
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
    public HttpServer getHttpServer() {
        return httpServer;
    }
    public boolean isRunning(){
        //если какой-то из модулей работает по таймеру, то эта проверка позволит ему понять что программа закрыта
        return service != null;
    }
    public void stop(){
        applicationManager = null;
        service = null;
        stopWiFiLock();
        for(CommandModule commandModule:commands)
            commandModule.stop();
    }


    private void startWiFiLock(){
        try {
            if(wifiLock == null) {
                log(". Блокировка состояния Wi-Fi...");
                WifiManager wifiManager = (WifiManager) activity.getApplicationContext().getSystemService(Context.WIFI_SERVICE);
                wifiLock = wifiManager.createWifiLock(programName);
                wifiLock.acquire();
            }

            if(wakeLock == null) {
                log(". Блокировка состояния CPU...");
                PowerManager pm = (PowerManager) activity.getApplicationContext().getSystemService(Context.POWER_SERVICE);
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

    //================================= ДАЛЬШЕ ИДЁТ СТАРОЕ =============================================================

    static public String programName = "DrFailov_VK_iHA_bot";
    static public String botcmd = "botcmd,bcd";


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

}