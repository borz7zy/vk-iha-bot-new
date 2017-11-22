package com.fsoft.vktest.AnswerInfrastructure.Functions.Modules;

import com.fsoft.vktest.AnswerInfrastructure.Answer;
import com.fsoft.vktest.AnswerInfrastructure.Functions.Function;
import com.fsoft.vktest.AnswerInfrastructure.Message;
import com.fsoft.vktest.ApplicationManager;
import com.fsoft.vktest.Modules.HttpServer;

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
                    applicationManager.getIgnorUsersList().add(message.getAuthor(), "Ѕлокировка по собственному желению");
                    message.setAnswer(new Answer("ѕоздравл€ю, ты был добавлен в список игнорируемых пользователей! " +
                            "“еперь € никогда не буду тебе отвечать."));
                    message = prepare(message);
                    warned.remove(message.getAuthor());
                    return message;
                }
                else {
                    warned.add(message.getAuthor());
                    message.setAnswer(new Answer("“ы правда хочешь, чтобы € теб€ заблокировал?\n" +
                            "я тебе после этого никогда не буду отвечать, и нет способа выйти из бана.\n" +
                            "“ы уверен? ≈сли да, повтори это ещЄ раз."));
                    message = prepare(message);
                    return message;
                }
            }
            catch (Exception  e){
                e.printStackTrace();
                message.setAnswer(new Answer("ѕрости, € не могу добавить теб€ в бан: " + e.getMessage()));
                message = prepare(message);
                return message;
            }
        }
        return super.processMessage(messageOriginal);
    }

    @Override
    public String defaultInvoker() {
        return "забань мен€";
    }

    @Override
    public String getName() {
        return "banhammer";
    }
    @Override
    public String getDescription() {
        return "Ѕан пользовател€ по просьбе \""+getStorage()+"\".\n" +
                "ѕользователь вноситс€ в список игнора после первого предупреждени€.";
    }
}
