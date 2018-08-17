package com.fsoft.vktest.AnswerInfrastructure;


import com.fsoft.vktest.AnswerInfrastructure.AnswerDatabase.Attachment;

import java.util.ArrayList;

/**
 *
 * Created by Dr. Failov on 12.02.2017.
 */
public class Answer{
    public String text = "";
    public ArrayList<Attachment> attachments = new ArrayList<>();
    public ArrayList<Long> forwarded = new ArrayList<>();

    public Answer(String text) {
        this.text = text;
    }
    public Answer(String text, Attachment attachment) {
        this.text = text;
        ArrayList<Attachment> attachments = new ArrayList<>();
        attachments.add(attachment);
        this.attachments = attachments;
    }
    public Answer(String text, ArrayList<Attachment> attachments) {
        this.text = text;
        this.attachments = attachments;
    }
    public Answer(String text, ArrayList<Attachment> attachments, ArrayList<Long> forwarded) {
        this.text = text;
        this.attachments = attachments;
        this.forwarded = forwarded;
    }

    public ArrayList<String> getAttachmentStrings(){
        //позволяет получить массив типа:
        //doc000_000
        //photo000_000
        //doc001_001
        // ...
        ArrayList<String> result = new ArrayList<>();
        for(int i=0; i<attachments.size(); i++)
            result.add(attachments.get(i).toString());
        return result;
    }
    public boolean isEmpty(){
        return text.equals("") && attachments.size() == 0;
    }

    @Override
    public String toString() {
        if(!attachments.isEmpty() || !forwarded.isEmpty())
            return text + "(+"+attachments.size()+"att, +"+forwarded.size()+"fwd.)";
        return text;
    }
}
