package com.fsoft.vktest;

import android.annotation.SuppressLint;
import android.content.Context;
import android.net.wifi.WifiManager;
import android.os.Environment;
import android.os.PowerManager;
import android.util.Log;

import com.fsoft.vktest.AnswerInfrastructure.BotBrain;
import com.fsoft.vktest.AnswerInfrastructure.Message;
import com.fsoft.vktest.Communication.Communicator;
import com.fsoft.vktest.Modules.Commands.ClearCache;
import com.fsoft.vktest.Modules.Commands.Command;
import com.fsoft.vktest.Modules.Commands.CommandDesc;
import com.fsoft.vktest.Modules.Commands.CpuTemp;
import com.fsoft.vktest.Modules.Commands.Decode;
import com.fsoft.vktest.Modules.Commands.Encode;
import com.fsoft.vktest.Modules.Commands.Version;
import com.fsoft.vktest.Modules.Autoreboot;
import com.fsoft.vktest.Modules.CommandModule;
import com.fsoft.vktest.Modules.DatabaseBackuper;
import com.fsoft.vktest.Modules.FileManager;
import com.fsoft.vktest.Communication.HttpServer;
import com.fsoft.vktest.Utils.CommandParser;
import com.fsoft.vktest.Utils.Parameters;
import com.fsoft.vktest.NewViewsLayer.MessagesFragment.MessageHistory;

import java.io.*;
import java.util.*;

/*
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
 * Communicator занимается работой с сетью. С аккаунтами. Именно этот модуль хранит аккаунты, запускает их. Аккаунты
 * инициируют вызовы других модулей.
 * /// как должны идти сообщения написанные боту изнутри программы?
 *
 *
 *
 *
 * Created by Dr. Failov on 14.08.2018.
 */
public class ApplicationManager extends CommandModule {
    static public String programName = "FP_iHA_bot";
    private static ApplicationManager applicationManagerInstance = null;

    static public String getVisibleName(){
        return getShortName();
    }

    static public String getShortName(){
        return "FP iHA bot";
    }

    public static ApplicationManager getInstance(){
        return applicationManagerInstance;
    }

    private BotService service = null;//это в общем то наш сервис. Он должен быть по любому
    private Communicator communicator;
    private BotBrain brain;
    private Parameters parameters;

    private MessageHistory messageHistory = null;
    private WifiManager.WifiLock wifiLock = null;
    private PowerManager.WakeLock wakeLock = null;
    private DatabaseBackuper databaseBackuper = null;
    private HttpServer httpServer = null;

    private long startedTime = 0; //нужен для учёта времени аптайма
//    private boolean standby = false;

    private Context context; // Add Context

    public ApplicationManager(BotService service) throws Exception{
        super();
        applicationManager = this;
        applicationManagerInstance = this;
        this.service = service;
        this.context = service.getApplicationContext(); // Get application context
        startedTime = System.currentTimeMillis();
        parameters = new Parameters(this);
        CommandParser.applicationManager = this;
        communicator = new Communicator(this);
        brain = new BotBrain(this, context); // Pass context to BotBrain
        databaseBackuper = new DatabaseBackuper(this);
        messageHistory = new MessageHistory(this);
        httpServer = new HttpServer(this);
        childCommands.add(new Version(this));
        childCommands.add(new Encode(this));
        childCommands.add(new Decode(this));
        childCommands.add(new CpuTemp(this));
        childCommands.add(new ClearCache(this));
        childCommands.add(new FileManager(this));
        childCommands.add(new Autoreboot(this));
        childCommands.add(brain);
        childCommands.add(communicator);
        childCommands.add(parameters);
        childCommands.add(databaseBackuper);
        childCommands.add(httpServer);
    }

    public static String getHomeFolder(){
        return Environment.getExternalStorageDirectory() + File.separator + programName;
    }

    public static String getDownloadsFolder(){
        return getHomeFolder() + File.separator + "downloads";
    }

    public Context getContext(){
        return context;
    }

    public void setContext(Context context) {
        this.context = context;
    }

    public String log(String text){
        Log.d("VK iHA bot", text);
        return text;
    }

    public Communicator getCommunicator() {
        return communicator;
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

    public MessageHistory getMessageHistory() {
        return messageHistory;
    }

    public boolean isRunning(){
        //если какой-то из модулей работает по таймеру, то эта проверка позволит ему понять что программа закрыта
        return service != null;
    }

    @Override
    public void stop(){
        super.stop();
        applicationManagerInstance = null;
        applicationManager = null;
        service = null;
        stopWiFiLock();
    }

//    @SuppressLint("InvalidWakeLockTag")
//    private void startWiFiLock(){
//        try {
//            if(wifiLock == null) {
//                log(". Блокировка состояния Wi-Fi...");
//                WifiManager wifiManager = (WifiManager) getContext().getApplicationContext().getSystemService(Context.WIFI_SERVICE);
//                wifiLock = wifiManager.createWifiLock(programName);
//                wifiLock.acquire();
//            }
//
//            if(wakeLock == null) {
//                log(". Блокировка состояния CPU...");
//                PowerManager pm = (PowerManager) getContext().getApplicationContext().getSystemService(Context.POWER_SERVICE);
//                wakeLock = pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "My Tag");
//                wakeLock.acquire();
//            }
//        }
//        catch (Exception e){
//            e.printStackTrace();
//            log("Ошибка блокировки Wi-Fi: " + e.toString());
//        }
//    }
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
}