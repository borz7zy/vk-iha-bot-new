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
                if(messages.contains(message)){
                    MessageStatus messageStatus = messages.get(messages.indexOf(message));
                    messageStatus.received();
                }
                else {
                    messages.add(0, new MessageStatus(message).received());
                    //почистить очередь больше 100
                    while(messages.size() > MESSAGE_LIMIT)
                        messages.remove(MESSAGE_LIMIT);
                }
            }

            @Override
            public void messageAnswered(Message message) {
                if(messages.contains(message)){
                    MessageStatus messageStatus = messages.get(messages.indexOf(message));
                    messageStatus.answered();
                }
                else {
                    messages.add(0, new MessageStatus(message).answered());
                    //почистить очередь больше 100
                    while(messages.size() > MESSAGE_LIMIT)
                        messages.remove(MESSAGE_LIMIT);
                }
            }

            @Override
            public void messageError(Message message, Exception e) {
                if(messages.contains(message)){
                    MessageStatus messageStatus = messages.get(messages.indexOf(message));
                    messageStatus.error();
                }
                else {
                    messages.add(0, new MessageStatus(message).error());
                    //почистить очередь больше 100
                    while(messages.size() > MESSAGE_LIMIT)
                        messages.remove(MESSAGE_LIMIT);
                }
            }

            @Override
            public void messageIgnored(Message message) {
                if(messages.contains(message)){
                    MessageStatus messageStatus = messages.get(messages.indexOf(message));
                    messageStatus.ignored();
                }
                else {
                    messages.add(0, new MessageStatus(message).ignored());
                    //почистить очередь больше 100
                    while(messages.size() > MESSAGE_LIMIT)
                        messages.remove(MESSAGE_LIMIT);
                }
            }
        });
    }
    public ArrayList<MessageStatus> getMessages() {
        return messages;
    }

    public class MessageStatus{
        public final int STATUS_RECEIVED = 0;
        public final int STATUS_ANSWERED = 1;
        public final int STATUS_IGNORED = 2;
        public final int STATUS_ERROR = 3;
        private Message message = null;
        private int status = 0;

        public MessageStatus(Message message, int status) {
            this.message = message;
            this.status = status;
        }

        public MessageStatus(Message message) {
            this.message = message;
        }

        public MessageStatus received(){
            status = STATUS_RECEIVED;
            return this;
        }

        public MessageStatus answered(){
            status = STATUS_ANSWERED;
            return this;
        }

        public MessageStatus error(){
            status = STATUS_ERROR;
            return this;
        }

        public MessageStatus ignored(){
            status = STATUS_IGNORED;
            return this;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            //чтобы можно было использовать contains (message)
            if(o.getClass() == Message.class)
                return o.equals(message);
            if (o == null || getClass() != o.getClass()) return false;
            MessageStatus that = (MessageStatus) o;
            return Objects.equals(message, that.message);
        }

        @Override
        public int hashCode() {
            return Objects.hash(message);
        }

        public void setMessage(Message message) {
            this.message = message;
        }

        public void setStatus(int status) {
            this.status = status;
        }

        public Message getMessage() {
            return message;
        }

        public int getStatus() {
            return status;
        }
    }
}
