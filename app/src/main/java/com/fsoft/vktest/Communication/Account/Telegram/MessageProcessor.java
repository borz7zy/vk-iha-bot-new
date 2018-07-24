package com.fsoft.vktest.Communication.Account.Telegram;

import com.fsoft.vktest.ApplicationManager;
import com.fsoft.vktest.Communication.Account.VK.VkAccount;
import com.fsoft.vktest.Modules.CommandModule;

public class MessageProcessor extends CommandModule {
    private TgAccount tgAccount = null;


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
    }

    public void stopModule(){
        log("Обработка сообщений для аккаунта "+tgAccount+" останавливается...");
    }
}
