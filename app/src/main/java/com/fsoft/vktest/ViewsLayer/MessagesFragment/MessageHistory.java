package com.fsoft.vktest.ViewsLayer.MessagesFragment;

import com.fsoft.vktest.AnswerInfrastructure.BotBrain;
import com.fsoft.vktest.AnswerInfrastructure.Message;
import com.fsoft.vktest.ApplicationManager;

import java.util.ArrayList;
import java.util.Objects;

/*
* Этот класс занимается хранением истории сообщений для отображения её на экране "сообщения".
* Вызывать из ApplicationManager!
* Обращаться  из MessagesFragment через ApplicationManager
*
* */
public class MessageHistory {
    private static int MESSAGE_LIMIT = 500;
    private ArrayList<MessageStatus> messages = new ArrayList<>(); //сначала самые новые, 0=последнее
    private ApplicationManager applicationManager = null;


    public MessageHistory(ApplicationManager applicationManager) {
        this.applicationManager = applicationManager;
        applicationManager.getBrain().addMessageListener(new BotBrain.OnMessageStatusChangedListener() {
            @Override
            public void messageReceived(Message message) {
                messages.add(0, new MessageStatus(message).received());
                //почистить очередь больше 100
                while(messages.size() > MESSAGE_LIMIT)
                    messages.remove(MESSAGE_LIMIT);
            }

            @Override
            public void messageAnswered(Message message) {
                for (MessageStatus messageStatus : messages)
                    if(messageStatus.getMessage().getMessage_id() == message.getMessage_id())
                        messageStatus.answered().setMessage(message);

            }

            @Override
            public void messageError(Message message, Exception e) {
                for (MessageStatus messageStatus : messages)
                    if(messageStatus.getMessage().getMessage_id() == message.getMessage_id())
                        messageStatus.error().setMessage(message);
            }

            @Override
            public void messageIgnored(Message message) {
                for (MessageStatus messageStatus : messages)
                    if(messageStatus.getMessage().getMessage_id() == message.getMessage_id())
                        messageStatus.ignored().setMessage(message);
            }
        });
    }
    public ArrayList<MessageStatus> getMessages() {
        return messages;
    }
}
