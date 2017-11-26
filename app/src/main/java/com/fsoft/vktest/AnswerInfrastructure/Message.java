package com.fsoft.vktest.AnswerInfrastructure;

import com.fsoft.vktest.ApplicationManager;
import com.fsoft.vktest.Communication.Account.VK.VkAccountCore;
import com.fsoft.vktest.ViewsLayer.MessagesListFragment;
import com.perm.kate.api.Attachment;

import java.util.ArrayList;

/**
Эта надстройка должна заниматься хранением контекстной информации, помогать подбирать ответ
 * Created by Dr. Failov on 11.02.2017.
 */

public class Message extends MessageBase{
    //Коммуникатор при помищи этого интерфейса описывает процедуру отправки ответа
    protected OnAnswerReady onAnswerReady = null;
    //это поле хранит функцию которая в любой момент
    //Каждый раз когда поступает новое сообщение, оно регистрируется на экране
    //в виде отдельного блока. Чтобы иметь возможность обращаться к этому блоку, будем хранить
    // ссылку на него здесь
    public MessagesListFragment.MessageList.MessageListElement messageListElement = null;

    public Message(String source, String text, long author, ArrayList<Attachment> attachments, VkAccountCore botAccount, OnAnswerReady onAnswerReady) {
        super(source, text, author, attachments, botAccount);
        this.onAnswerReady = onAnswerReady;
    }

    public Message(Message toCopy) {
        super(toCopy);
        this.onAnswerReady = toCopy.onAnswerReady;
        this.messageListElement = toCopy.messageListElement;
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
            if(attachment.type.equals("photo"))
                cnt ++;
        return cnt;
    }
    public int getAudioCount(){
        if(attachments == null)
            return 0;
        int cnt = 0;
        for (Attachment attachment:attachments)
            if(attachment.type.equals("audio"))
                cnt ++;
        return cnt;
    }
    public int getVideoCount(){
        if(attachments == null)
            return 0;
        int cnt = 0;
        for (Attachment attachment:attachments)
            if(attachment.type.equals("video"))
                cnt ++;
        return cnt;
    }
    public int getDocumentsCount(){
        if(attachments == null)
            return 0;
        int cnt = 0;
        for (Attachment attachment:attachments)
            if(attachment.type.equals("doc"))
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
