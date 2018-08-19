package com.fsoft.vktest.AnswerInfrastructure;

import com.fsoft.vktest.AnswerInfrastructure.AnswerDatabase.Attachment;
import com.fsoft.vktest.ApplicationManager;
import com.fsoft.vktest.Communication.Account.Account;
import com.fsoft.vktest.Communication.Account.AccountBase;
import com.fsoft.vktest.Communication.Account.VK.VkAccountCore;
import com.fsoft.vktest.Utils.User;

import java.util.ArrayList;

/**
Эта надстройка должна заниматься хранением контекстной информации, помогать подбирать ответ
 * Created by Dr. Failov on 11.02.2017.
 */

public class Message extends MessageBase{
    //Коммуникатор при помищи этого интерфейса описывает процедуру отправки ответа
    protected OnAnswerReady onAnswerReady = null;


    public Message(String source, String text, User author, ArrayList<Attachment> attachments, AccountBase botAccount, OnAnswerReady onAnswerReady) {
        super(source, text, author, attachments, botAccount);
        this.onAnswerReady = onAnswerReady;
    }
    public Message(String source, String text, User author, AccountBase botAccount, OnAnswerReady onAnswerReady) {
        super(source, text, author, null, botAccount);
        this.onAnswerReady = onAnswerReady;
    }

    public Message(Message toCopy) {
        super(toCopy);
        this.onAnswerReady = toCopy.onAnswerReady;
    }

    public MessageBase withOnAnswerReady(OnAnswerReady onAnswerReady) {
        this.onAnswerReady = onAnswerReady;
        return this;
    }
    public OnAnswerReady getOnAnswerReady() {
        return onAnswerReady;
    }
    public void setOnAnswerReady(OnAnswerReady onAnswerReady) {
        this.onAnswerReady = onAnswerReady;
    }

    public int getPhotosCount(){
        if(attachments == null)
            return 0;
        int cnt = 0;
        for (Attachment attachment:attachments)
            if(attachment.getType().equals("photo"))
                cnt ++;
        return cnt;
    }
    public int getAudioCount(){
        if(attachments == null)
            return 0;
        int cnt = 0;
        for (Attachment attachment:attachments)
            if(attachment.getType().equals("audio"))
                cnt ++;
        return cnt;
    }
    public int getVideoCount(){
        if(attachments == null)
            return 0;
        int cnt = 0;
        for (Attachment attachment:attachments)
            if(attachment.getType().equals("video"))
                cnt ++;
        return cnt;
    }
    public int getDocumentsCount(){
        if(attachments == null)
            return 0;
        int cnt = 0;
        for (Attachment attachment:attachments)
            if(attachment.getType().equals("doc"))
                cnt ++;
        return cnt;
    }
    public int getStickersCount(){
        //// TODO: 14.07.2017 реализовать этот тип вложений
        return 0;
    }
    public int getRecordsCount(){
        return 0;
    }

    public void sendAnswer(String text){
        sendAnswer(new Answer(text));
    }
    public void sendAnswer(Answer answer){
        if(onAnswerReady != null) {
            try {
                this.answer = answer;
                onAnswerReady.sendAnswer(this);
            }
            catch (Exception e){
                if(botAccount != null)
                    botAccount.log("! Не удалось отправить ответ на сообщение " + getText() + " из-за ошибки: " + e.toString());
            }
        }
    }

    @Override
    public Message withAnswer(String text) {
        return (Message)super.withAnswer(text);
    }

    public interface OnAnswerReady{
        void sendAnswer(Message answer);
    }
}
