package com.fsoft.vktest.Communication.Account.Telegram;

import com.fsoft.vktest.ApplicationManager;
import com.fsoft.vktest.Communication.Account.VK.VkAccount;
import com.fsoft.vktest.Modules.CommandModule;
import com.fsoft.vktest.Utils.F;

import java.util.ArrayList;

public class MessageProcessor extends CommandModule {
    private TgAccount tgAccount = null;
    private long lastUpdateId = 0;
    private boolean isRunning = false;
    private int errors = 0;


    public MessageProcessor(ApplicationManager applicationManager, TgAccount tgAccount) {
        super(applicationManager);
        this.tgAccount = tgAccount;
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
                        log(". Received " + updates.size() + " "+tgAccount+" updates.");
                        errors = 0;
                        if(isRunning) {
                            processUpdates(updates);
                            update();
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
                        if(errors > 20){
                            tgAccount.stopAccount();
                            tgAccount.state("Слишком много ошибок " + error.getClass().getName() + " " +error.getMessage());
                        }
                        if(isRunning) {
                            if(errors != 0) {
                                log("Waiting " + errors + " seconds between errors...");
                                F.sleep(errors * 1000);
                            }
                            update();
                        }
                    }
                }).start();
            }
        }, lastUpdateId+1, 5);
    }

    public void stopModule(){
        log("Обработка сообщений для аккаунта "+tgAccount+" останавливается...");
        isRunning = false;
    }

    public void processUpdates(ArrayList<Update> updates){
        for (Update update:updates){
            if(update.getUpdate_id() > lastUpdateId)
                lastUpdateId = update.getUpdate_id();
            if(update.getMessage() != null){
                log(". ПОЛУЧЕНО СООБЩЕНИЕ: " + update.getMessage());
            }
        }
    }
}
