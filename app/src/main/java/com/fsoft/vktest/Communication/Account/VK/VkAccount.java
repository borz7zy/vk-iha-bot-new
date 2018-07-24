package com.fsoft.vktest.Communication.Account.VK;

import com.fsoft.vktest.ApplicationManager;

/**
 * эта структура должна уже работать на уровне прикладных задач:
 * чтение СМС, отклонение друзей, принятие, чтение стен, и т.д.
 * Created by Dr. Failov on 16.02.2017.
 */
public class VkAccount extends VkAccountCore {
    private AllowFriends allowFriends = null;
    private StatusBroadcaster statusBroadcaster = null;
    private MessageProcessor messageProcessor = null;

    public VkAccount(ApplicationManager applicationManager, String fileAddress) {
        super(applicationManager, fileAddress);
        allowFriends = new AllowFriends(applicationManager, this);
        statusBroadcaster = new StatusBroadcaster(applicationManager, this);
        messageProcessor = new MessageProcessor(applicationManager, this);

        childCommands.add(allowFriends);
        childCommands.add(statusBroadcaster);
        childCommands.add(messageProcessor);
    }

    @Override
    public void startAccount() {
        super.startAccount();
        allowFriends.startModule();
        statusBroadcaster.startModule();
        messageProcessor.startModule();
    }

    @Override
    public void stopAccount() {
        super.stopAccount();
        allowFriends.stopModule();
        statusBroadcaster.stopModule();
        messageProcessor.stopModule();
    }
}
