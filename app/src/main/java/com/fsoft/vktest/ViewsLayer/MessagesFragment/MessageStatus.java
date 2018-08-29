package com.fsoft.vktest.ViewsLayer.MessagesFragment;

import com.fsoft.vktest.AnswerInfrastructure.Message;

import java.util.Objects;

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

    public boolean isReceived(){
        return status == STATUS_RECEIVED;
    }

    public boolean isAnswered(){
        return status == STATUS_ANSWERED;
    }

    public boolean isIgnored(){
        return status == STATUS_IGNORED;
    }

    public boolean isError(){
        return status == STATUS_ERROR;
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