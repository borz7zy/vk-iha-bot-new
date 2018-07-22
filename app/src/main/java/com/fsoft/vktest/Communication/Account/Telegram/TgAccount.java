package com.fsoft.vktest.Communication.Account.Telegram;

import com.fsoft.vktest.ApplicationManager;

public class TgAccount extends TgAccountCore {
    public TgAccount(ApplicationManager applicationManager, String fileName) {
        super(applicationManager, fileName);
    }

    @Override
    protected void startAccount() {
        super.startAccount();
    }

    @Override
    public void stopAccount() {
        super.stopAccount();
    }
}
