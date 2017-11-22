package com.fsoft.vktest.Modules.Commands;

import com.fsoft.vktest.AnswerInfrastructure.Message;
import com.fsoft.vktest.ApplicationManager;
import com.fsoft.vktest.Modules.CommandModule;
import com.fsoft.vktest.Utils.CommandParser;

import java.util.ArrayList;

/**
 *
 * Created by Dr. Failov on 15.02.2017.
 */
public class SetBotMark  extends CommandModule {

    public SetBotMark(ApplicationManager applicationManager) {
        super(applicationManager);
    }

    @Override
    public ArrayList<CommandDesc> getHelp() {
        ArrayList <CommandDesc> result = new ArrayList<>();
        result.add(new CommandDesc("Изменить метку бота",
                "Метка бота отображается в скобках перед текстом ответа.\n" +
                        "Метка нужна для того, чтобы твои друзья могли отличать когда им пишет бот, а когда ты.\n" +
                        "Если ты хочешь чтобы метка не отображалась в сообщении, " +
                        "сделай метку \"EMPTY\".\n" +
                        "Заметь, полностью убрать метку получится только если ты купил донат-версию:)",
                "botcmd setbotmark <новая метка>"));
        return result;
    }

    @Override
    public String processCommand(Message message) {
        CommandParser commandParser = new CommandParser(message.getText());
        String word1 = commandParser.getWord() ;
        if(word1.equals("setbotmark") || word1.equals("setbotname")) {
            String newMark = commandParser.getText();
            if(applicationManager.isEmptyMark(newMark) && !applicationManager.isDonated())
                return "Прости, пустую метку можно поставить только если ты купил донатку. " +
                        "Подробнее о донатке смотри на экране \"Состояние\".";
            applicationManager.setBotMark(newMark);
            return "Новая метка бота сохранена: \"(" + applicationManager.getBotMark() + ")\".";
        }
        return "";
    }
}
