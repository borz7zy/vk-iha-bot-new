package com.fsoft.vktest.AnswerInfrastructure.Functions.Modules;

import com.fsoft.vktest.AnswerInfrastructure.Answer;
import com.fsoft.vktest.AnswerInfrastructure.Functions.Function;
import com.fsoft.vktest.AnswerInfrastructure.Message;
import com.fsoft.vktest.ApplicationManager;
import com.fsoft.vktest.Communication.HttpServer;

import java.util.ArrayList;

/**
 *
 * Created by Dr. Failov on 28.09.2017.
 */

public class BanMe extends Function {
    private ArrayList<Long> warned = new ArrayList<>();


    public BanMe(ApplicationManager applicationManager) {
        super(applicationManager);
    }

    @Override
    public Message processMessage(Message messageOriginal) {
        if(!hasTreatment(messageOriginal))
            return super.processMessage(messageOriginal);
        Message message = remTreatment(messageOriginal);

        if(message.getAuthor() == HttpServer.USER_ID)
            return super.processMessage(messageOriginal);

        if(message.getText().equalsIgnoreCase(getInvoker())){
            try {
                if (warned.contains(message.getAuthor())) {
                    applicationManager.getBrain().getIgnor().add(message.getAuthor(), "Блокировка по собственному желению");
                    message.setAnswer(new Answer("Поздравляю, ты был добавлен в список игнорируемых пользователей! " +
                            "Теперь я никогда не буду тебе отвечать."));
                    message = prepare(message);
                    warned.remove(message.getAuthor());
                    return message;
                }
                else {
                    warned.add(message.getAuthor());
                    message.setAnswer(new Answer("Ты правда хочешь, чтобы я тебя заблокировал?\n" +
                            "Я тебе после этого никогда не буду отвечать, и нет способа выйти из бана.\n" +
                            "Ты уверен? Если да, повтори это ещё раз."));
                    message = prepare(message);
                    return message;
                }
            }
            catch (Exception  e){
                e.printStackTrace();
                message.setAnswer(new Answer("Прости, я не могу добавить тебя в бан: " + e.getMessage()));
                message = prepare(message);
                return message;
            }
        }
        return super.processMessage(messageOriginal);
    }

    @Override
    public String defaultInvoker() {
        return "забань меня";
    }

    @Override
    public String getName() {
        return "banhammer";
    }
    @Override
    public String getDescription() {
        return "Бан пользователя по просьбе \""+getStorage()+"\".\n" +
                "Пользователь вносится в список игнора после первого предупреждения.";
    }
}
