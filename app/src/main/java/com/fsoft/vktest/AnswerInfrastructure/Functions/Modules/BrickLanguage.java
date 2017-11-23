package com.fsoft.vktest.AnswerInfrastructure.Functions.Modules;

import com.fsoft.vktest.AnswerInfrastructure.Answer;
import com.fsoft.vktest.AnswerInfrastructure.Functions.Function;
import com.fsoft.vktest.AnswerInfrastructure.Message;
import com.fsoft.vktest.ApplicationManager;

/**
 * переводчик на Кирпич и с Кирпича
 * Created by Dr. Failov on 12.02.2017.
 */
public class BrickLanguage extends Function {
    public BrickLanguage(ApplicationManager applicationManager) {
        super(applicationManager);
    }

    @Override
    public Message processMessage(Message messageOriginal) {
        if(!hasTreatment(messageOriginal))
            return super.processMessage(messageOriginal);
        Message message = remTreatment(messageOriginal);


        String text = message.getText();
        if(text.startsWith(getInvoker() + " ")  && text.length() < 400){
            String input = text.replace(getInvoker() + " ", "");
            String output = "";
            for (int i = 0; i < input.length(); i++) {
                char c = input.charAt(i);
                output += c;

                String oralk = "уеыаоэиюя";
                if(oralk.indexOf(c) >= 0)
                    output += "с" + c;

                String oralkb = "УЕЫАОЭИЮЯ";
                if(oralkb.indexOf(c) >= 0)
                    output += "С" + c;

                String orall = "eyuioa";
                if(orall.indexOf(c) >= 0)
                    output += "s" + c;

                String orallb = "EYUIOA";
                if(orallb.indexOf(c) >= 0)
                    output += "S" + c;
            }
            message.setAnswer(new Answer("Ваша фраза на \"кирпиче\":\n" + output));
            message = prepare(message);
            return message;
        }
        if(text.startsWith("де"+getInvoker()+" ") && text.length() < 400){
            String input = text.replace("де"+getInvoker()+" ", "");
            String output = "";
            for (int i = 0; i < input.length(); i++) {
                char c = input.charAt(i);
                output += c;
                String oral = "уеыаоэиюя" + "УЕЫАОЭИЮЯ" + "eyuioa" + "EYUIOA";
                if(oral.indexOf(c) >= 0)
                    i += 2;
            }

            message.setAnswer(new Answer("Ваша фраза переведена с \"кирпича\":\n" + output));
            message = prepare(message);
            return message;
        }
        return super.processMessage(messageOriginal);
    }

    @Override
    protected String defaultInvoker() {
        return "кирпич";
    }

    @Override
    public String getName() {
        return "bricklanguage";
    }

    @Override
    public String getDescription() {
        return "Шифратор/дешифратор языка \"кирпич\", доступный по командам \""+
                applicationManager.getBrain().getTreatment() + getInvoker() +" ...\"" +
                " и \"" +
                applicationManager.getBrain().getTreatment() + "де" + getInvoker() + " ...\"";
    }
}
