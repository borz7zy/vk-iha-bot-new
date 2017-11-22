package com.fsoft.vktest.AnswerInfrastructure.Functions.Modules;

import com.fsoft.vktest.AnswerInfrastructure.Answer;
import com.fsoft.vktest.AnswerInfrastructure.Functions.Function;
import com.fsoft.vktest.AnswerInfrastructure.Message;
import com.fsoft.vktest.ApplicationManager;

/**
 * случайным образом отвечает процентики. А народ радуется.
 * Created by Dr. Failov on 12.02.2017.
 */
public class Infa extends Function {
    public Infa(ApplicationManager applicationManager) {
        super(applicationManager);
    }

    @Override
    public Message processMessage(Message messageOriginal) {
        if(!hasTreatment(messageOriginal))
            return super.processMessage(messageOriginal);
        Message message = remTreatment(messageOriginal);

        String text = message.getText();
        String[] words = text.split(" ");
        if(words.length > 1 && words[0].toLowerCase().replace(",", "").equals(getInvoker())){
            int infa = (int)(System.currentTimeMillis()%101);
            text = text.replace(getInvoker(), "");
            if(text.length() > 200)
                return super.processMessage(messageOriginal);
            message.setAnswer(new Answer("Инфа " + infa + "%."));
            message = prepare(message);
            return message;
        }
        return super.processMessage(messageOriginal);
    }

    @Override
    public String defaultInvoker() {
        return "инфа";
    }

    @Override public String getName() {
        return "infa";
    }
    @Override public String getDescription() {
        return "Подбор случайной вероятности по запросу \""+getInvoker()+"...\".";
    }
}
