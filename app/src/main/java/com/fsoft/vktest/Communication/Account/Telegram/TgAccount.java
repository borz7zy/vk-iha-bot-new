package com.fsoft.vktest.Communication.Account.Telegram;

import com.fsoft.vktest.ApplicationManager;

public class TgAccount extends TgAccountCore {
    private MessageProcessor messageProcessor = null;

    public TgAccount(ApplicationManager applicationManager, String fileName) {
        super(applicationManager, fileName);
        messageProcessor = new MessageProcessor(applicationManager, this);
    }

    @Override
    public void startAccount() {
        super.startAccount();
        checkTokenValidity(new OnTokenValidityCheckedListener() {
            @Override
            public void onTokenPass() {
                messageProcessor.startModule();
            }

            @Override
            public void onTokenFail() {

            }
        });
    }

    @Override
    public void stopAccount() {
        super.stopAccount();
        messageProcessor.stopModule();
    }
}
